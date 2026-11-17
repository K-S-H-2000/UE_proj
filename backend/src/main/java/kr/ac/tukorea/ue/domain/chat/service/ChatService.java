package kr.ac.tukorea.ue.domain.chat.service;

import kr.ac.tukorea.ue.domain.chat.dto.ChatDto;
import kr.ac.tukorea.ue.domain.chat.entity.ChatCreatedVia;
import kr.ac.tukorea.ue.domain.chat.entity.ChatRoom;
import kr.ac.tukorea.ue.domain.chat.entity.Message;
import kr.ac.tukorea.ue.domain.chat.entity.MessageType;
import kr.ac.tukorea.ue.domain.chat.repository.ChatRoomRepository;
import kr.ac.tukorea.ue.domain.chat.repository.MessageRepository;
import kr.ac.tukorea.ue.domain.comment.entity.Comment;
import kr.ac.tukorea.ue.domain.comment.service.CommentService;
import kr.ac.tukorea.ue.domain.interaction.repository.BlockRepository;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.entity.ProductStatus;
import kr.ac.tukorea.ue.domain.product.service.ProductService;
import kr.ac.tukorea.ue.domain.transaction.entity.Transaction;
import kr.ac.tukorea.ue.domain.transaction.entity.TransactionStatus;
import kr.ac.tukorea.ue.domain.transaction.repository.TransactionRepository;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ProductService productService;
    private final CommentService commentService;
    private final UserService userService;
    private final BlockRepository blockRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 7-1. 구매 요청 (채팅방 개설)
     */
    @Transactional
    public ChatDto.RoomCreateResponse openPurchaseChatRoom(Long buyerId, Long productId) {
        Product product = productService.findProductById(productId);

        if (product.getSeller().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.CANNOT_REQUEST_OWN_PRODUCT);
        }

        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }

        if (blockRepository.existsByBlockerIdAndBlockedId(buyerId, product.getSeller().getId()) ||
            blockRepository.existsByBlockerIdAndBlockedId(product.getSeller().getId(), buyerId)) {
            throw new BusinessException(ErrorCode.BLOCKED_RELATION);
        }

        Optional<ChatRoom> existingRoom = chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId);
        if (existingRoom.isPresent()) {
            return ChatDto.RoomCreateResponse.builder()
                    .roomId(existingRoom.get().getId())
                    .isNew(false)
                    .build();
        }

        User buyer = userService.findUserById(buyerId);
        ChatRoom room = ChatRoom.builder()
                .product(product)
                .buyer(buyer)
                .seller(product.getSeller())
                .createdVia(ChatCreatedVia.PURCHASE_REQUEST)
                .build();

        ChatRoom saved = chatRoomRepository.save(room);

        return ChatDto.RoomCreateResponse.builder()
                .roomId(saved.getId())
                .isNew(true)
                .build();
    }

    /**
     * 7-2. 거래 요청 (판매자 -> 댓글 작성자)
     */
    @Transactional
    public ChatDto.RoomCreateResponse openTradeChatRoomFromComment(Long sellerId, Long commentId) {
        Comment comment = commentService.findCommentById(commentId);
        Product product = comment.getProduct();

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "판매글 작성자만 거래 요청을 보낼 수 있어요.");
        }

        User buyer = comment.getUser();
        if (buyer.getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.CANNOT_REQUEST_OWN_PRODUCT);
        }

        Optional<ChatRoom> existingRoom = chatRoomRepository.findByProductIdAndBuyerId(product.getId(), buyer.getId());
        if (existingRoom.isPresent()) {
            return ChatDto.RoomCreateResponse.builder()
                    .roomId(existingRoom.get().getId())
                    .isNew(false)
                    .build();
        }

        ChatRoom room = ChatRoom.builder()
                .product(product)
                .buyer(buyer)
                .seller(product.getSeller())
                .createdVia(ChatCreatedVia.TRADE_REQUEST)
                .build();

        ChatRoom saved = chatRoomRepository.save(room);

        return ChatDto.RoomCreateResponse.builder()
                .roomId(saved.getId())
                .isNew(true)
                .build();
    }

    /**
     * 7-3. 채팅방 목록 조회
     */
    public List<ChatDto.RoomListResponse> getMyChatRooms(Long userId) {
        List<ChatRoom> buyerRooms = chatRoomRepository.findByBuyerIdOrderByLastMessageAtDesc(userId);
        List<ChatRoom> sellerRooms = chatRoomRepository.findBySellerIdOrderByLastMessageAtDesc(userId);

        Set<ChatRoom> allRooms = new LinkedHashSet<>();
        allRooms.addAll(buyerRooms);
        allRooms.addAll(sellerRooms);

        List<ChatRoom> sortedRooms = new ArrayList<>(allRooms);
        sortedRooms.sort((a, b) -> b.getLastMessageAt().compareTo(a.getLastMessageAt()));

        return sortedRooms.stream().map(room -> {
            boolean isSeller = room.getSeller().getId().equals(userId);
            User opponent = isSeller ? room.getBuyer() : room.getSeller();
            LocalDateTime lastReadAt = isSeller ? room.getSellerLastReadAt() : room.getBuyerLastReadAt();

            Optional<Message> latestMsg = messageRepository.findTopByRoomIdOrderByCreatedAtDesc(room.getId());
            String lastMessageContent = latestMsg.map(Message::getContent).orElse("대화가 시작되었습니다.");

            int unreadCount = messageRepository.countByRoomIdAndSenderIdNotAndCreatedAtAfter(
                    room.getId(), userId, lastReadAt
            );

            // 진행 중인 예약(트랜잭션) ID 조회
            Optional<Transaction> activeTrx = transactionRepository.findByProductIdAndStatusIn(
                    room.getProduct().getId(),
                    List.of(TransactionStatus.REQUESTED, TransactionStatus.RESERVED)
            );
            Long activeTrxId = activeTrx.map(Transaction::getId).orElse(null);

            String thumb = room.getProduct().getImages().isEmpty()
                    ? null
                    : room.getProduct().getImages().get(0).getS3Url();

            return ChatDto.RoomListResponse.builder()
                    .roomId(room.getId())
                    .opponent(UserDto.SimpleUserResponse.from(opponent))
                    .product(ChatDto.ProductSummary.builder()
                            .productId(room.getProduct().getId())
                            .title(room.getProduct().getTitle())
                            .thumbnailUrl(thumb)
                            .status(room.getProduct().getStatus().name())
                            .build())
                    .lastMessage(lastMessageContent)
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unreadCount)
                    .myRole(isSeller ? "SELLER" : "BUYER")
                    .activeTransactionId(activeTrxId)
                    .build();
        }).toList();
    }

    /**
     * 7-4. 메시지 조회 (읽음 처리 포함)
     */
    @Transactional
    public Page<ChatDto.MessageResponse> getMessages(Long userId, Long roomId, Pageable pageable) {
        ChatRoom room = findRoomById(roomId);

        boolean isBuyer = room.getBuyer().getId().equals(userId);
        boolean isSeller = room.getSeller().getId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 채팅방의 참여자가 아니에요.");
        }

        // 읽음 처리 시각 갱신
        LocalDateTime now = LocalDateTime.now();
        if (isSeller) {
            room.updateSellerLastRead(now);
        } else {
            room.updateBuyerLastRead(now);
        }

        Page<Message> msgPage = messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId, pageable);

        return msgPage.map(m -> ChatDto.MessageResponse.builder()
                .messageId(m.getId())
                .roomId(room.getId())
                .sender(UserDto.SimpleUserResponse.from(m.getSender()))
                .content(m.getContent())
                .messageType(m.getMessageType().name())
                .createdAt(m.getCreatedAt())
                .build());
    }

    /**
     * 7-5. 메시지 전송
     */
    @Transactional
    public ChatDto.MessageResponse sendMessage(
            Long senderId, Long roomId, String content, MessageType messageType
    ) {
        ChatRoom room = findRoomById(roomId);

        boolean isBuyer = room.getBuyer().getId().equals(senderId);
        boolean isSeller = room.getSeller().getId().equals(senderId);

        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 채팅방의 참여자가 아니에요.");
        }

        User sender = isSeller ? room.getSeller() : room.getBuyer();
        MessageType type = (messageType != null) ? messageType : MessageType.TEXT;

        Message message = Message.builder()
                .room(room)
                .sender(sender)
                .messageType(type)
                .content(content)
                .build();

        Message saved = messageRepository.save(message);

        LocalDateTime now = LocalDateTime.now();
        room.updateLastMessage(now);
        if (isSeller) {
            room.updateSellerLastRead(now);
        } else {
            room.updateBuyerLastRead(now);
        }

        return ChatDto.MessageResponse.builder()
                .messageId(saved.getId())
                .roomId(room.getId())
                .sender(UserDto.SimpleUserResponse.from(sender))
                .content(saved.getContent())
                .messageType(saved.getMessageType().name())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public ChatRoom findRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없어요."));
    }
}
