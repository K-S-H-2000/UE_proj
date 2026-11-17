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
import kr.ac.tukorea.ue.domain.transaction.repository.TransactionRepository;
import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CommentService commentService;

    @Mock
    private UserService userService;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ChatService chatService;

    private User seller;
    private User buyer;
    private Product product;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        University university = University.builder().name("한국공학대학교").emailDomain("tukorea.ac.kr").build();

        seller = User.builder().university(university).nickname("판매자").email("seller@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(seller, "id", 10L);

        buyer = User.builder().university(university).nickname("구매자").email("buyer@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(buyer, "id", 20L);

        product = Product.builder().university(university).seller(seller).title("공학용 계산기").price(15000).status(ProductStatus.ON_SALE).build();
        ReflectionTestUtils.setField(product, "id", 100L);

        chatRoom = ChatRoom.builder().product(product).seller(seller).buyer(buyer).createdVia(ChatCreatedVia.PURCHASE_REQUEST).build();
        ReflectionTestUtils.setField(chatRoom, "id", 300L);
    }

    @Test
    @DisplayName("7-1. 본인 상품에 구매 요청 시 CANNOT_REQUEST_OWN_PRODUCT 예외")
    void openPurchaseChatRoom_ownProduct() {
        given(productService.findProductById(100L)).willReturn(product);

        assertThatThrownBy(() -> chatService.openPurchaseChatRoom(10L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REQUEST_OWN_PRODUCT);
    }

    @Test
    @DisplayName("7-1. 판매중(ON_SALE)이 아닌 상품 구매 요청 시 PRODUCT_NOT_ON_SALE 예외")
    void openPurchaseChatRoom_notOnSale() {
        product.updateStatus(ProductStatus.SOLD);
        given(productService.findProductById(100L)).willReturn(product);

        assertThatThrownBy(() -> chatService.openPurchaseChatRoom(20L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_ON_SALE);
    }

    @Test
    @DisplayName("7-1. 차단 관계인 사용자의 상품에 구매 요청 시 BLOCKED_RELATION 예외")
    void openPurchaseChatRoom_blocked() {
        given(productService.findProductById(100L)).willReturn(product);
        given(blockRepository.existsByBlockerIdAndBlockedId(20L, 10L)).willReturn(true);

        assertThatThrownBy(() -> chatService.openPurchaseChatRoom(20L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BLOCKED_RELATION);
    }

    @Test
    @DisplayName("7-1. 기존 채팅방이 없는 경우 신규 방 개설 (isNew=true)")
    void openPurchaseChatRoom_new() {
        given(productService.findProductById(100L)).willReturn(product);
        given(chatRoomRepository.findByProductIdAndBuyerId(100L, 20L)).willReturn(Optional.empty());
        given(userService.findUserById(20L)).willReturn(buyer);
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);

        ChatDto.RoomCreateResponse response = chatService.openPurchaseChatRoom(20L, 100L);

        assertThat(response.getRoomId()).isEqualTo(300L);
        assertThat(response.isNew()).isTrue();
    }

    @Test
    @DisplayName("7-1. 이미 존재하는 채팅방인 경우 기존 방 반환 (isNew=false)")
    void openPurchaseChatRoom_existing() {
        given(productService.findProductById(100L)).willReturn(product);
        given(chatRoomRepository.findByProductIdAndBuyerId(100L, 20L)).willReturn(Optional.of(chatRoom));

        ChatDto.RoomCreateResponse response = chatService.openPurchaseChatRoom(20L, 100L);

        assertThat(response.getRoomId()).isEqualTo(300L);
        assertThat(response.isNew()).isFalse();
    }

    @Test
    @DisplayName("7-2. 판매자가 아닌 사용자가 댓글에서 거래 요청 시 FORBIDDEN 예외")
    void openTradeChatRoom_notSeller() {
        Comment comment = Comment.builder().product(product).user(buyer).content("얼마?").build();
        given(commentService.findCommentById(500L)).willReturn(comment);

        assertThatThrownBy(() -> chatService.openTradeChatRoomFromComment(999L, 500L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("7-5. 메시지 전송 시 저장 및 최신 메시지 시각 갱신")
    void sendMessage_success() {
        given(chatRoomRepository.findById(300L)).willReturn(Optional.of(chatRoom));
        Message message = Message.builder().room(chatRoom).sender(buyer).content("안녕하세요").build();
        ReflectionTestUtils.setField(message, "id", 1000L);
        given(messageRepository.save(any(Message.class))).willReturn(message);

        ChatDto.MessageResponse response = chatService.sendMessage(20L, 300L, "안녕하세요", MessageType.TEXT);

        assertThat(response.getMessageId()).isEqualTo(1000L);
        assertThat(response.getContent()).isEqualTo("안녕하세요");
    }
}
