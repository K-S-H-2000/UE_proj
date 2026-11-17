package kr.ac.tukorea.ue.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.ac.tukorea.ue.domain.chat.entity.MessageType;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ChatDto {

    // === 요청 ===

    @Getter
    @NoArgsConstructor
    public static class SendMessageRequest {
        @NotNull(message = "채팅방 ID는 필수예요.")
        private Long roomId;

        @NotBlank(message = "메시지 내용은 필수예요.")
        private String content;

        private MessageType messageType;
    }

    // === 응답 ===

    @Getter
    @Builder
    public static class RoomCreateResponse {
        private final Long roomId;
        private final boolean isNew;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoomListResponse {
        private final Long roomId;
        private final UserDto.SimpleUserResponse opponent;
        private final ProductSummary product;
        private final String lastMessage;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime lastMessageAt;

        private final int unreadCount;
        private final String myRole;
        private final Long activeTransactionId;
    }

    @Getter
    @Builder
    public static class ProductSummary {
        private final Long productId;
        private final String title;
        private final String thumbnailUrl;
        private final String status;
    }

    @Getter
    @Builder
    public static class MessageResponse {
        private final Long messageId;
        private final Long roomId;
        private final UserDto.SimpleUserResponse sender;
        private final String content;
        private final String messageType;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime createdAt;
    }
}
