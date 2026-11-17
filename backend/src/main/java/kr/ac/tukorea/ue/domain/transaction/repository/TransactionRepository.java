package kr.ac.tukorea.ue.domain.transaction.repository;

import kr.ac.tukorea.ue.domain.transaction.entity.Transaction;
import kr.ac.tukorea.ue.domain.transaction.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByProductIdAndStatusIn(Long productId, Collection<TransactionStatus> statuses);
    Page<Transaction> findBySellerId(Long sellerId, Pageable pageable);
    Page<Transaction> findByBuyerId(Long buyerId, Pageable pageable);

    // 어뷰징 방지: 7일 내 동일 사용자 간 완료된 거래 건수 체크
    long countBySellerIdAndBuyerIdAndStatusAndCompletedAtAfter(
            Long sellerId, Long buyerId, TransactionStatus status, LocalDateTime afterTime
    );

    boolean existsBySellerIdAndBuyerIdAndStatusIn(
            Long sellerId, Long buyerId, Collection<TransactionStatus> statuses
    );
}
