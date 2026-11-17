package kr.ac.tukorea.ue.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import kr.ac.tukorea.ue.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDto {

    // === 요청 ===

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        @Size(min = 2, max = 30, message = "닉네임은 2~30자여야 해요.")
        private String nickname;

        @Size(max = 50, message = "학과명은 50자를 초과할 수 없어요.")
        private String department;
    }

    // === 응답 ===

    @Getter
    @Builder
    public static class MyProfileResponse {
        private final Long userId;
        private final String nickname;
        private final String department;
        private final String universityName;
        private final String profileImageUrl;
        private final int saleCount;
        private final int buyCount;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private final LocalDateTime verifiedAt;

        public static MyProfileResponse from(User user) {
            return MyProfileResponse.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .department(user.getDepartment())
                    .universityName(user.getUniversity().getName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .saleCount(user.getSaleCount())
                    .buyCount(user.getBuyCount())
                    .verifiedAt(user.getVerifiedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PublicProfileResponse {
        private final Long userId;
        private final String nickname;
        private final boolean universityVerified;
        private final String universityName;
        private final String profileImageUrl;
        private final int saleCount;
        private final int buyCount;

        public static PublicProfileResponse from(User user) {
            return PublicProfileResponse.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .universityVerified(true)
                    .universityName(user.getUniversity().getName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .saleCount(user.getSaleCount())
                    .buyCount(user.getBuyCount())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SimpleUserResponse {
        private final Long userId;
        private final String nickname;

        public static SimpleUserResponse from(User user) {
            return SimpleUserResponse.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SellerResponse {
        private final Long userId;
        private final String nickname;
        private final int saleCount;
        private final int buyCount;

        public static SellerResponse from(User user) {
            return SellerResponse.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .saleCount(user.getSaleCount())
                    .buyCount(user.getBuyCount())
                    .build();
        }
    }

    @Getter
    public static class ProfileImageResponse {
        private final String profileImageUrl;

        public ProfileImageResponse(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}
