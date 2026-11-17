package kr.ac.tukorea.ue.domain.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class TransactionDto {

    // === 요청 ===

    @Getter
    @NoArgsConstructor
    public static class CancelRequest {
        private String reason;
    }

    @Getter
    @NoArgsConstructor
    public static class CompleteRequest {
        @Min(value = 0, message = "거래 금액은 0원 이상이어야 해요.")
        private Integer agreedPrice;
    }

    @Getter
    @NoArgsConstructor
    public static class ConfirmRequest {
        private boolean confirmed;
    }

    // === 응답 ===

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReservationResponse {
        private final Long transactionId;
        private final String status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime expiresAt;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HistoryResponse {
        private final Long transactionId;
        private final ProductSummary product;
        private final UserDto.SimpleUserResponse opponent;
        private final Integer agreedPrice;
        private final String status;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime completedAt;
    }

    @Getter
    @Builder
    public static class ProductSummary {
        private final Long productId;
        private final String title;
        private final String thumbnailUrl;
    }
}
