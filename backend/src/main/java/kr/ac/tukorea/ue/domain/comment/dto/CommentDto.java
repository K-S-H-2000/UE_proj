package kr.ac.tukorea.ue.domain.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class CommentDto {

    // === 요청 ===

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "댓글 내용은 필수예요.")
        private String content;

        @Min(value = 0, message = "가격 제안은 0원 이상이어야 해요.")
        private Integer offerPrice;

        private Long parentCommentId;
    }

    @Getter
    @Builder
    public static class CreateResponse {
        private final Long commentId;
    }

    // === 응답 ===

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private final Long commentId;
        private final UserInfo user;
        private final String content;
        private final Integer offerPrice;
        private final int likeCount;
        private final boolean isLiked;
        private final boolean isMine;
        private final boolean isSeller;
        private final boolean notificationEnabled;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime createdAt;

        private final List<Response> replies;
    }

    @Getter
    @Builder
    public static class UserInfo {
        private final Long userId;
        private final String nickname;
    }

    @Getter
    public static class LikeResponse {
        private final boolean isLiked;
        private final int likeCount;

        public LikeResponse(boolean isLiked, int likeCount) {
            this.isLiked = isLiked;
            this.likeCount = likeCount;
        }
    }

    @Getter
    public static class NotificationResponse {
        private final boolean notificationEnabled;

        public NotificationResponse(boolean notificationEnabled) {
            this.notificationEnabled = notificationEnabled;
        }
    }
}
