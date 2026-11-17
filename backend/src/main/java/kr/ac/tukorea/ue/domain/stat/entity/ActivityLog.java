package kr.ac.tukorea.ue.domain.stat.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id")
    private University university;

    @Column(name = "event_type", length = 30, nullable = false)
    private String eventType;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "keyword", length = 100)
    private String keyword;

    @Column(name = "result_count")
    private Integer resultCount;

    @Builder
    public ActivityLog(User user, University university, String eventType,
                       Long productId, String keyword, Integer resultCount) {
        this.user = user;
        this.university = university;
        this.eventType = eventType;
        this.productId = productId;
        this.keyword = keyword;
        this.resultCount = resultCount;
    }
}
