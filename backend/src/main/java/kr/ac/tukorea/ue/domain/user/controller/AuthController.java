package kr.ac.tukorea.ue.domain.user.controller;

import jakarta.validation.Valid;
import kr.ac.tukorea.ue.domain.user.dto.AuthDto;
import kr.ac.tukorea.ue.domain.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 2-1. 인증 메일 발송
     */
    @PostMapping("/email/send")
    public ResponseEntity<AuthDto.EmailSendResponse> sendEmail(
            @Valid @RequestBody AuthDto.EmailSendRequest request
    ) {
        AuthDto.EmailSendResponse response = authService.sendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * 2-2. 인증 코드 확인
     */
    @PostMapping("/email/verify")
    public ResponseEntity<AuthDto.EmailVerifyResponse> verifyCode(
            @Valid @RequestBody AuthDto.EmailVerifyRequest request
    ) {
        AuthDto.EmailVerifyResponse response = authService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(response);
    }

    /**
     * 2-3. 닉네임 중복 검사
     */
    @GetMapping("/nickname/check")
    public ResponseEntity<AuthDto.NicknameCheckResponse> checkNickname(
            @RequestParam(required = false, defaultValue = "1") Long universityId,
            @RequestParam String nickname
    ) {
        AuthDto.NicknameCheckResponse response = authService.checkNickname(universityId, nickname);
        return ResponseEntity.ok(response);
    }

    /**
     * 2-4. 회원가입 완료
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthDto.SignupResponse> signup(
            @RequestHeader(value = "X-Signup-Token", required = false) String headerToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody AuthDto.SignupRequest request
    ) {
        String token = headerToken;
        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        AuthDto.SignupResponse response = authService.signup(token, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2-5. 토큰 재발급
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<AuthDto.TokenResponse> refreshToken(
            @Valid @RequestBody AuthDto.TokenRefreshRequest request
    ) {
        AuthDto.TokenResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}
