package kr.ac.tukorea.ue.domain.transaction.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TransactionStatus status = TransactionStatus.REQUESTED;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "agreed_price")
    private Integer agreedPrice;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "buyer_confirmed_at")
    private LocalDateTime buyerConfirmedAt;

    @Builder
    public Transaction(Product product, User seller, User buyer, TransactionStatus status,
                       LocalDateTime requestedAt) {
        this.product = product;
        this.seller = seller;
        this.buyer = buyer;
        this.status = (status != null) ? status : TransactionStatus.REQUESTED;
        this.requestedAt = (requestedAt != null) ? requestedAt : LocalDateTime.now();
    }

    public void accept() {
        this.status = TransactionStatus.RESERVED;
        this.acceptedAt = LocalDateTime.now();
    }

    public void cancel(User cancelledBy, String cancelReason) {
        this.status = TransactionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = cancelledBy;
        this.cancelReason = cancelReason;
    }

    public void complete(Integer agreedPrice) {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.agreedPrice = agreedPrice;
    }

    public void confirmByBuyer() {
        this.buyerConfirmedAt = LocalDateTime.now();
    }
}
