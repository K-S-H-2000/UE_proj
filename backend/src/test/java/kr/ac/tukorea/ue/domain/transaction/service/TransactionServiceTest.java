package kr.ac.tukorea.ue.domain.transaction.service;

import kr.ac.tukorea.ue.domain.chat.entity.ChatCreatedVia;
import kr.ac.tukorea.ue.domain.chat.entity.ChatRoom;
import kr.ac.tukorea.ue.domain.chat.repository.ChatRoomRepository;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.entity.ProductStatus;
import kr.ac.tukorea.ue.domain.transaction.dto.TransactionDto;
import kr.ac.tukorea.ue.domain.transaction.entity.Transaction;
import kr.ac.tukorea.ue.domain.transaction.entity.TransactionStatus;
import kr.ac.tukorea.ue.domain.transaction.repository.TransactionRepository;
import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.domain.user.entity.User;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User seller;
    private User buyer;
    private Product product;
    private ChatRoom chatRoom;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        University university = University.builder().name("한국공학대학교").emailDomain("tukorea.ac.kr").build();
        ReflectionTestUtils.setField(university, "id", 1L);

        seller = User.builder().university(university).nickname("판매자").email("seller@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(seller, "id", 10L);

        buyer = User.builder().university(university).nickname("구매자").email("buyer@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(buyer, "id", 20L);

        product = Product.builder().university(university).seller(seller).title("계산기").price(15000).build();
        ReflectionTestUtils.setField(product, "id", 100L);

        chatRoom = ChatRoom.builder().product(product).seller(seller).buyer(buyer).createdVia(ChatCreatedVia.PURCHASE_REQUEST).build();
        ReflectionTestUtils.setField(chatRoom, "id", 300L);

        transaction = Transaction.builder().product(product).seller(seller).buyer(buyer).status(TransactionStatus.REQUESTED).build();
        ReflectionTestUtils.setField(transaction, "id", 500L);
    }

    @Test
    @DisplayName("8-1. 판매자가 아닌 사용자가 예약 요청 시 FORBIDDEN 예외")
    void requestReservation_notSeller() {
        given(chatRoomRepository.findById(300L)).willReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> transactionService.requestReservation(999L, 300L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("8-1. 이미 진행 중인 예약/요청이 있는 상품에 다시 요청 시 ACTIVE_TRANSACTION_EXISTS 예외 (TRST-01c)")
    void requestReservation_activeExists() {
        given(chatRoomRepository.findById(300L)).willReturn(Optional.of(chatRoom));
        given(transactionRepository.findByProductIdAndStatusIn(eq(100L), any()))
                .willReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.requestReservation(10L, 300L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACTIVE_TRANSACTION_EXISTS);
    }

    @Test
    @DisplayName("8-1. 정상 예약 요청 시 상품 상태 RESERVE_REQUESTED 전이")
    void requestReservation_success() {
        given(chatRoomRepository.findById(300L)).willReturn(Optional.of(chatRoom));
        given(transactionRepository.findByProductIdAndStatusIn(eq(100L), any()))
                .willReturn(Optional.empty());
        given(transactionRepository.save(any(Transaction.class))).willReturn(transaction);

        TransactionDto.ReservationResponse response = transactionService.requestReservation(10L, 300L);

        assertThat(response.getStatus()).isEqualTo("REQUESTED");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.RESERVE_REQUESTED);
    }

    @Test
    @DisplayName("8-2. 구매자가 아닌 사용자가 예약 수락 시 FORBIDDEN 예외")
    void acceptReservation_notBuyer() {
        given(transactionRepository.findById(500L)).willReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.acceptReservation(999L, 500L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("8-2. 정상 예약 수락 시 상태 RESERVED 전이")
    void acceptReservation_success() {
        given(transactionRepository.findById(500L)).willReturn(Optional.of(transaction));

        transactionService.acceptReservation(20L, 500L);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.RESERVED);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.RESERVED);
    }

    @Test
    @DisplayName("8-4. 거래 당사자 예약 취소 시 상품 ON_SALE 복귀")
    void cancelReservation_success() {
        given(transactionRepository.findById(500L)).willReturn(Optional.of(transaction));

        transactionService.cancelReservation(20L, 500L, "다른 물건 구매");

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @DisplayName("8-5. RESERVED 상태가 아닌데 판매완료 처리 시 INVALID_STATE_TRANSITION 예외")
    void completeTransaction_invalidState() {
        // 현재 상태가 REQUESTED
        given(transactionRepository.findById(500L)).willReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.completeTransaction(10L, 500L, 12000))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATE_TRANSITION);
    }

    @Test
    @DisplayName("8-6. 거래 완료 확인 - 7일 내 첫 거래인 경우에만 양측 실적 카운트 증가 (TRST-04)")
    void confirmTransaction_firstInSevenDays_incrementCount() {
        transaction.accept();
        transaction.complete(15000);
        given(transactionRepository.findById(500L)).willReturn(Optional.of(transaction));
        given(transactionRepository.countBySellerIdAndBuyerIdAndStatusAndCompletedAtAfter(
                eq(10L), eq(20L), eq(TransactionStatus.COMPLETED), any(LocalDateTime.class)
        )).willReturn(1L); // 7일 내 첫 거래

        transactionService.confirmTransaction(20L, 500L, true);

        assertThat(seller.getSaleCount()).isEqualTo(1);
        assertThat(buyer.getBuyCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("8-6. 거래 완료 확인 - 7일 내 재거래 시 실적 카운트 증가하지 않음 (어뷰징 방지)")
    void confirmTransaction_abusePrevention_noIncrement() {
        transaction.accept();
        transaction.complete(15000);
        given(transactionRepository.findById(500L)).willReturn(Optional.of(transaction));
        given(transactionRepository.countBySellerIdAndBuyerIdAndStatusAndCompletedAtAfter(
                eq(10L), eq(20L), eq(TransactionStatus.COMPLETED), any(LocalDateTime.class)
        )).willReturn(2L); // 7일 내 2회 이상 거래

        transactionService.confirmTransaction(20L, 500L, true);

        assertThat(seller.getSaleCount()).isEqualTo(0);
        assertThat(buyer.getBuyCount()).isEqualTo(0);
    }
}
