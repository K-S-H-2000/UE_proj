package kr.ac.tukorea.ue.domain.chat.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_chat_room", columnNames = {"product_id", "buyer_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_via", length = 20, nullable = false)
    private ChatCreatedVia createdVia;

    @Column(name = "buyer_last_read_at", nullable = false)
    private LocalDateTime buyerLastReadAt;

    @Column(name = "seller_last_read_at", nullable = false)
    private LocalDateTime sellerLastReadAt;

    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    @Builder
    public ChatRoom(Product product, User buyer, User seller, ChatCreatedVia createdVia,
                    LocalDateTime buyerLastReadAt, LocalDateTime sellerLastReadAt, LocalDateTime lastMessageAt) {
        this.product = product;
        this.buyer = buyer;
        this.seller = seller;
        this.createdVia = createdVia;
        LocalDateTime now = LocalDateTime.now();
        this.buyerLastReadAt = (buyerLastReadAt != null) ? buyerLastReadAt : now;
        this.sellerLastReadAt = (sellerLastReadAt != null) ? sellerLastReadAt : now;
        this.lastMessageAt = (lastMessageAt != null) ? lastMessageAt : now;
    }

    public void updateLastMessage(LocalDateTime messageTime) {
        this.lastMessageAt = messageTime;
    }

    public void updateBuyerLastRead(LocalDateTime readTime) {
        this.buyerLastReadAt = readTime;
    }

    public void updateSellerLastRead(LocalDateTime readTime) {
        this.sellerLastReadAt = readTime;
    }
}
