package kr.ac.tukorea.ue.domain.stat.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.domain.university.entity.University;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kpi_daily")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KpiDaily {

    @EmbeddedId
    private KpiDailyId id;

    @MapsId("universityId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "signup_count", nullable = false)
    private int signupCount = 0;

    @Column(name = "active_users", nullable = false)
    private int activeUsers = 0;

    @Column(name = "product_count", nullable = false)
    private int productCount = 0;

    @Column(name = "reserved_count", nullable = false)
    private int reservedCount = 0;

    @Column(name = "completed_count", nullable = false)
    private int completedCount = 0;

    @Builder
    public KpiDaily(KpiDailyId id, University university, Integer signupCount, Integer activeUsers,
                    Integer productCount, Integer reservedCount, Integer completedCount) {
        this.id = id;
        this.university = university;
        this.signupCount = (signupCount != null) ? signupCount : 0;
        this.activeUsers = (activeUsers != null) ? activeUsers : 0;
        this.productCount = (productCount != null) ? productCount : 0;
        this.reservedCount = (reservedCount != null) ? reservedCount : 0;
        this.completedCount = (completedCount != null) ? completedCount : 0;
    }
}
