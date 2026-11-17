package kr.ac.tukorea.ue.domain.transaction.service;

import kr.ac.tukorea.ue.domain.chat.entity.ChatRoom;
import kr.ac.tukorea.ue.domain.chat.repository.ChatRoomRepository;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.entity.ProductStatus;
import kr.ac.tukorea.ue.domain.transaction.dto.TransactionDto;
import kr.ac.tukorea.ue.domain.transaction.entity.Transaction;
import kr.ac.tukorea.ue.domain.transaction.entity.TransactionStatus;
import kr.ac.tukorea.ue.domain.transaction.repository.TransactionRepository;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ChatRoomRepository chatRoomRepository;

    private static final int RESERVATION_EXPIRY_HOURS = 1;
    private static final int ABUSE_PREVENTION_DAYS = 7;

    /**
     * 8-1. 예약 요청 (판매자 -> 구매자)
     */
    @Transactional
    public TransactionDto.ReservationResponse requestReservation(Long sellerId, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없어요."));

        if (!chatRoom.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "판매자만 예약을 요청할 수 있어요.");
        }

        Product product = chatRoom.getProduct();

        // TRST-01c: 한 상품에 동시에 유효한 예약 요청·예약은 1건
        boolean hasActiveTransaction = transactionRepository.findByProductIdAndStatusIn(
                product.getId(),
                List.of(TransactionStatus.REQUESTED, TransactionStatus.RESERVED)
        ).isPresent();

        if (hasActiveTransaction) {
            throw new BusinessException(ErrorCode.ACTIVE_TRANSACTION_EXISTS);
        }

        Transaction transaction = Transaction.builder()
                .product(product)
                .seller(chatRoom.getSeller())
                .buyer(chatRoom.getBuyer())
                .status(TransactionStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        product.updateStatus(ProductStatus.RESERVE_REQUESTED);
        Transaction saved = transactionRepository.save(transaction);

        return TransactionDto.ReservationResponse.builder()
                .transactionId(saved.getId())
                .status(saved.getStatus().name())
                .expiresAt(saved.getRequestedAt().plusHours(RESERVATION_EXPIRY_HOURS))
                .build();
    }

    /**
     * 8-2. 예약 수락 (구매자)
     */
    @Transactional
    public void acceptReservation(Long buyerId, Long transactionId) {
        Transaction transaction = findTransactionById(transactionId);

        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "구매자만 예약을 수락할 수 있어요.");
        }

        if (transaction.getStatus() != TransactionStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        transaction.accept();
        transaction.getProduct().updateStatus(ProductStatus.RESERVED);
    }

    /**
     * 8-2. 예약 거절 (구매자)
     */
    @Transactional
    public void rejectReservation(Long buyerId, Long transactionId) {
        Transaction transaction = findTransactionById(transactionId);

        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "구매자만 예약을 거절할 수 있어요.");
        }

        if (transaction.getStatus() != TransactionStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        transaction.cancel(transaction.getBuyer(), "구매자 예약 거절");
        transaction.getProduct().updateStatus(ProductStatus.ON_SALE);
    }

    /**
     * 8-3. 예약 요청 철회 (판매자)
     */
    @Transactional
    public void withdrawReservation(Long sellerId, Long transactionId) {
        Transaction transaction = findTransactionById(transactionId);

        if (!transaction.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "판매자만 요청을 철회할 수 있어요.");
        }

        if (transaction.getStatus() != TransactionStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        transaction.cancel(transaction.getSeller(), "판매자 요청 철회");
        transaction.getProduct().updateStatus(ProductStatus.ON_SALE);
    }

    /**
     * 8-4. 예약 취소 (판매자 또는 구매자 누구나 페널티 없이 가능)
     */
    @Transactional
    public void cancelReservation(Long userId, Long transactionId, String reason) {
        Transaction transaction = findTransactionById(transactionId);

        boolean isSeller = transaction.getSeller().getId().equals(userId);
        boolean isBuyer = transaction.getBuyer().getId().equals(userId);

        if (!isSeller && !isBuyer) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "거래 당사자만 취소할 수 있어요.");
        }

        if (transaction.getStatus() != TransactionStatus.REQUESTED && transaction.getStatus() != TransactionStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        User canceller = isSeller ? transaction.getSeller() : transaction.getBuyer();
        transaction.cancel(canceller, reason != null ? reason : "사유 미입력");
        transaction.getProduct().updateStatus(ProductStatus.ON_SALE);
    }

    /**
     * 8-5. 판매완료 처리 (판매자)
     */
    @Transactional
    public void completeTransaction(Long sellerId, Long transactionId, Integer agreedPrice) {
        Transaction transaction = findTransactionById(transactionId);

        if (!transaction.getSeller().getId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "판매자만 판매완료 처리할 수 있어요.");
        }

        if (transaction.getStatus() != TransactionStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        int finalPrice = (agreedPrice != null) ? agreedPrice : transaction.getProduct().getPrice();
        transaction.complete(finalPrice);
        transaction.getProduct().updateStatus(ProductStatus.SOLD);
    }

    /**
     * 8-6. 거래 완료 확인 (구매자)
     * - 동일 상대와의 거래 완료는 7일 내 1회만 판매자/구매자 거래 실적 카운트 반영 (TRST-04)
     */
    @Transactional
    public void confirmTransaction(Long buyerId, Long transactionId, boolean confirmed) {
        Transaction transaction = findTransactionById(transactionId);

        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "구매자만 거래 완료를 확인할 수 있어요.");
        }

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        if (confirmed) {
            transaction.confirmByBuyer();

            // 7일 내 동일 상대와의 거래 완료 횟수 검사
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(ABUSE_PREVENTION_DAYS);
            long recentCount = transactionRepository.countBySellerIdAndBuyerIdAndStatusAndCompletedAtAfter(
                    transaction.getSeller().getId(),
                    transaction.getBuyer().getId(),
                    TransactionStatus.COMPLETED,
                    sevenDaysAgo
            );

            // 7일 내 첫 번째 거래 완료인 경우에만 양측 카운트 증가
            if (recentCount <= 1) {
                transaction.getSeller().incrementSaleCount();
                transaction.getBuyer().incrementBuyCount();
            }
        }
    }

    /**
     * 8-7. 거래 내역 조회
     */
    public List<TransactionDto.HistoryResponse> getMyTransactions(Long userId, String role, Pageable pageable) {
        Page<Transaction> page = "SELLER".equalsIgnoreCase(role)
                ? transactionRepository.findBySellerId(userId, pageable)
                : transactionRepository.findByBuyerId(userId, pageable);

        return page.getContent().stream()
                .map(trx -> {
                    User opponent = "SELLER".equalsIgnoreCase(role) ? trx.getBuyer() : trx.getSeller();
                    String thumb = trx.getProduct().getImages().isEmpty()
                            ? null
                            : trx.getProduct().getImages().get(0).getS3Url();

                    return TransactionDto.HistoryResponse.builder()
                            .transactionId(trx.getId())
                            .product(TransactionDto.ProductSummary.builder()
                                    .productId(trx.getProduct().getId())
                                    .title(trx.getProduct().getTitle())
                                    .thumbnailUrl(thumb)
                                    .build())
                            .opponent(UserDto.SimpleUserResponse.from(opponent))
                            .agreedPrice(trx.getAgreedPrice())
                            .status(trx.getStatus().name())
                            .completedAt(trx.getCompletedAt())
                            .build();
                })
                .toList();
    }

    public Transaction findTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "거래 정보를 찾을 수 없어요."));
    }
}
