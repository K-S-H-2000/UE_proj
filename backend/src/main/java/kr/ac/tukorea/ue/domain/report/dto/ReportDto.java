package kr.ac.tukorea.ue.domain.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ReportDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "신고 대상 타입은 필수예요.")
        private String targetType;   // PRODUCT | COMMENT | USER

        @NotNull(message = "신고 대상 ID는 필수예요.")
        private Long targetId;

        @NotBlank(message = "신고 사유는 필수예요.")
        private String reasonCode;   // NO_SHOW | MISMATCH | PROHIBITED | RUDE | ETC

        private String detail;
    }

    @Getter
    @Builder
    public static class CreateResponse {
        private final Long reportId;
    }
}
