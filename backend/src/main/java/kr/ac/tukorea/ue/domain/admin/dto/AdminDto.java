package kr.ac.tukorea.ue.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.ac.tukorea.ue.domain.report.entity.Report;
import kr.ac.tukorea.ue.domain.report.entity.ReportStatus;
import kr.ac.tukorea.ue.domain.search.entity.Synonym;
import kr.ac.tukorea.ue.domain.stat.entity.KpiDaily;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import kr.ac.tukorea.ue.domain.user.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminDto {

    // === 신고 처리 ===

    @Getter
    @NoArgsConstructor
    public static class ReportProcessRequest {
        @NotNull(message = "처리 상태는 필수예요.")
        private ReportStatus status;

        private String adminNote;
    }

    @Getter
    @Builder
    public static class ReportResponse {
        private final Long reportId;
        private final UserDto.SimpleUserResponse reporter;
        private final String targetType;
        private final Long targetId;
        private final String reasonCode;
        private final String detail;
        private final String status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime createdAt;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime resolvedAt;

        private final String adminNote;

        public static ReportResponse from(Report report) {
            String targetType;
            Long targetId;
            if (report.getTargetProduct() != null) {
                targetType = "PRODUCT";
                targetId = report.getTargetProduct().getId();
            } else if (report.getTargetComment() != null) {
                targetType = "COMMENT";
                targetId = report.getTargetComment().getId();
            } else {
                targetType = "USER";
                targetId = report.getTargetUser().getId();
            }

            return ReportResponse.builder()
                    .reportId(report.getId())
                    .reporter(UserDto.SimpleUserResponse.from(report.getReporter()))
                    .targetType(targetType)
                    .targetId(targetId)
                    .reasonCode(report.getReasonCode().name())
                    .detail(report.getDetail())
                    .status(report.getStatus().name())
                    .createdAt(report.getCreatedAt())
                    .resolvedAt(report.getResolvedAt())
                    .adminNote(report.getAdminNote())
                    .build();
        }
    }

    // === 유저 상태 변경 ===

    @Getter
    @NoArgsConstructor
    public static class UserStatusUpdateRequest {
        @NotNull(message = "변경할 상태값은 필수예요.")
        private UserStatus status;
    }

    // === 검색 동의어 ===

    @Getter
    @NoArgsConstructor
    public static class SynonymCreateRequest {
        @NotBlank(message = "키워드는 필수예요.")
        private String keyword;

        @NotBlank(message = "동의어는 필수예요.")
        private String synonym;
    }

    @Getter
    @Builder
    public static class SynonymResponse {
        private final Long synonymId;
        private final String keyword;
        private final String synonym;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime createdAt;

        public static SynonymResponse from(Synonym s) {
            return SynonymResponse.builder()
                    .synonymId(s.getId())
                    .keyword(s.getKeyword())
                    .synonym(s.getSynonym())
                    .createdAt(s.getCreatedAt())
                    .build();
        }
    }

    // === 일별 통계 (KPI) ===

    @Getter
    @Builder
    public static class KpiDailyResponse {
        private final LocalDate statDate;
        private final Long universityId;
        private final String universityName;
        private final int signupCount;
        private final int activeUsers;
        private final int productCount;
        private final int reservedCount;
        private final int completedCount;

        public static KpiDailyResponse from(KpiDaily kpi) {
            return KpiDailyResponse.builder()
                    .statDate(kpi.getId().getStatDate())
                    .universityId(kpi.getUniversity().getId())
                    .universityName(kpi.getUniversity().getName())
                    .signupCount(kpi.getSignupCount())
                    .activeUsers(kpi.getActiveUsers())
                    .productCount(kpi.getProductCount())
                    .reservedCount(kpi.getReservedCount())
                    .completedCount(kpi.getCompletedCount())
                    .build();
        }
    }
}
