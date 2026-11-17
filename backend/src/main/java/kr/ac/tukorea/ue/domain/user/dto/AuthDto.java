package kr.ac.tukorea.ue.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    // === 요청 ===

    @Getter
    @NoArgsConstructor
    public static class EmailSendRequest {
        @NotBlank(message = "이메일은 필수예요.")
        @Email(message = "올바른 이메일 형식이 아니에요.")
        private String email;
    }

    @Getter
    @NoArgsConstructor
    public static class EmailVerifyRequest {
        @NotBlank(message = "이메일은 필수예요.")
        @Email
        private String email;

        @NotBlank(message = "인증 코드는 필수예요.")
        private String code;
    }

    @Getter
    @NoArgsConstructor
    public static class SignupRequest {
        @NotBlank(message = "닉네임은 필수예요.")
        @Size(min = 2, max = 30, message = "닉네임은 2~30자여야 해요.")
        private String nickname;

        @Size(max = 50, message = "학과명은 50자를 초과할 수 없어요.")
        private String department;
    }

    @Getter
    @NoArgsConstructor
    public static class TokenRefreshRequest {
        @NotBlank(message = "refreshToken은 필수예요.")
        private String refreshToken;
    }

    // === 응답 ===

    @Getter
    public static class EmailSendResponse {
        private final String universityName;
        private final int expiresInSeconds;

        public EmailSendResponse(String universityName, int expiresInSeconds) {
            this.universityName = universityName;
            this.expiresInSeconds = expiresInSeconds;
        }
    }

    @Getter
    public static class EmailVerifyResponse {
        private final boolean isNewUser;
        private final String signupToken;
        private final String accessToken;
        private final String refreshToken;

        public EmailVerifyResponse(boolean isNewUser, String signupToken,
                                   String accessToken, String refreshToken) {
            this.isNewUser = isNewUser;
            this.signupToken = signupToken;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    @Getter
    public static class NicknameCheckResponse {
        private final boolean available;
        private final java.util.List<String> suggestions;

        public NicknameCheckResponse(boolean available, java.util.List<String> suggestions) {
            this.available = available;
            this.suggestions = suggestions;
        }
    }

    @Getter
    public static class SignupResponse {
        private final String accessToken;
        private final String refreshToken;
        private final UserSummary user;

        public SignupResponse(String accessToken, String refreshToken, UserSummary user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }
    }

    @Getter
    public static class UserSummary {
        private final Long userId;
        private final String nickname;
        private final String universityName;

        public UserSummary(Long userId, String nickname, String universityName) {
            this.userId = userId;
            this.nickname = nickname;
            this.universityName = universityName;
        }
    }

    @Getter
    public static class TokenResponse {
        private final String accessToken;

        public TokenResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }
}
