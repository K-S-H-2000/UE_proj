package kr.ac.tukorea.ue.domain.user.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_univ_nickname", columnNames = {"university_id", "nickname"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "nickname", length = 30, nullable = false)
    private String nickname;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "sale_count", nullable = false)
    private int saleCount = 0;

    @Column(name = "buy_count", nullable = false)
    private int buyCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Builder
    public User(University university, String email, String nickname, String department,
                String profileImageUrl, Integer saleCount, Integer buyCount,
                UserStatus status, LocalDateTime verifiedAt) {
        this.university = university;
        this.email = email;
        this.nickname = nickname;
        this.department = department;
        this.profileImageUrl = profileImageUrl;
        this.saleCount = (saleCount != null) ? saleCount : 0;
        this.buyCount = (buyCount != null) ? buyCount : 0;
        this.status = (status != null) ? status : UserStatus.ACTIVE;
        this.verifiedAt = (verifiedAt != null) ? verifiedAt : LocalDateTime.now();
    }

    public void updateProfile(String nickname, String department, String profileImageUrl) {
        if (nickname != null) this.nickname = nickname;
        if (department != null) this.department = department;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void incrementSaleCount() {
        this.saleCount++;
    }

    public void incrementBuyCount() {
        this.buyCount++;
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    public void renewVerification() {
        this.verifiedAt = LocalDateTime.now();
    }
}
