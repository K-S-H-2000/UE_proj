package kr.ac.tukorea.ue.domain.user.controller;

import jakarta.validation.Valid;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final kr.ac.tukorea.ue.domain.interaction.service.FavoriteService favoriteService;

    /**
     * 3-1. 내 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto.MyProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserDto.MyProfileResponse response = userService.getMyProfile(userPrincipal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 3-2. 타인 프로필 조회 (학과 및 판매 목록 비공개)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto.PublicProfileResponse> getPublicProfile(
            @PathVariable Long userId
    ) {
        UserDto.PublicProfileResponse response = userService.getPublicProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 3-3. 프로필 수정
     */
    @PatchMapping("/me")
    public ResponseEntity<UserDto.MyProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserDto.UpdateRequest request
    ) {
        UserDto.MyProfileResponse response = userService.updateProfile(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 3-4. 프로필 사진 업로드
     */
    @PostMapping("/me/image")
    public ResponseEntity<UserDto.ProfileImageResponse> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("image") MultipartFile file
    ) {
        // 임시 저장 또는 S3 업로드 로직 (현재는 파일명 기반 가상 URL 반환)
        String mockS3Url = "https://ue-bucket.s3.ap-northeast-2.amazonaws.com/profile/"
                + userPrincipal.getUserId() + "_" + file.getOriginalFilename();
        UserDto.ProfileImageResponse response = userService.updateProfileImage(userPrincipal.getUserId(), mockS3Url);
        return ResponseEntity.ok(response);
    }

    /**
     * 5-2. 찜 목록 조회
     */
    @GetMapping("/me/favorites")
    public ResponseEntity<kr.ac.tukorea.ue.global.dto.PageResponse<kr.ac.tukorea.ue.domain.product.dto.ProductDto.FeedResponse>> getMyFavorites(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable
    ) {
        var response = favoriteService.getMyFavorites(userPrincipal.getUserId(), pageable);
        return ResponseEntity.ok(response);
    }
}
