package kr.ac.tukorea.ue.domain.report.entity;

import jakarta.persistence.*;
import kr.ac.tukorea.ue.domain.comment.entity.Comment;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.global.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_product_id")
    private Product targetProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_comment_id")
    private Comment targetComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 30, nullable = false)
    private ReportReasonCode reasonCode;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReportStatus status = ReportStatus.RECEIVED;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Builder
    public Report(User reporter, Product targetProduct, Comment targetComment, User targetUser,
                  ReportReasonCode reasonCode, String detail, ReportStatus status) {
        this.reporter = reporter;
        this.targetProduct = targetProduct;
        this.targetComment = targetComment;
        this.targetUser = targetUser;
        this.reasonCode = reasonCode;
        this.detail = detail;
        this.status = (status != null) ? status : ReportStatus.RECEIVED;
    }

    public void resolve(String adminNote) {
        this.status = ReportStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.adminNote = adminNote;
    }
}
