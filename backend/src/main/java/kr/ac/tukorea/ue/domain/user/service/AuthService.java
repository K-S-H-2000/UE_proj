package kr.ac.tukorea.ue.domain.user.service;

import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.domain.university.repository.UniversityRepository;
import kr.ac.tukorea.ue.domain.user.dto.AuthDto;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.entity.UserStatus;
import kr.ac.tukorea.ue.domain.user.repository.UserRepository;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import kr.ac.tukorea.ue.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UniversityRepository universityRepository;
    private final UserRepository userRepository;
    private final EmailVerificationCodeStore codeStore;
    private final EmailSender emailSender;
    private final JwtTokenProvider jwtTokenProvider;

    private static final int EXPIRY_SECONDS = 600; // 10분
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 2-1. 인증 메일 발송
     */
    @Transactional
    public AuthDto.EmailSendResponse sendVerificationEmail(String email) {
        String domain = extractDomain(email);

        University university = universityRepository.findByEmailDomain(domain)
                .filter(University::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOMAIN_NOT_ALLOWED));

        // 6자리 난수 코드 생성
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        codeStore.save(email, code, EXPIRY_SECONDS);

        emailSender.sendVerificationCode(email, code);

        return new AuthDto.EmailSendResponse(university.getName(), EXPIRY_SECONDS);
    }

    /**
     * 2-2. 인증 코드 확인
     */
    @Transactional
    public AuthDto.EmailVerifyResponse verifyCode(String email, String code) {
        if (!codeStore.verify(email, code)) {
            throw new BusinessException(ErrorCode.CODE_INVALID);
        }

        String domain = extractDomain(email);
        University university = universityRepository.findByEmailDomain(domain)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOMAIN_NOT_ALLOWED));

        Optional<User> userOptional = userRepository.findByEmail(email);

        codeStore.remove(email);

        if (userOptional.isEmpty()) {
            // 신규 가입 대상자 -> signupToken 발급
            String signupToken = jwtTokenProvider.createSignupToken(email, university.getId());
            return new AuthDto.EmailVerifyResponse(true, signupToken, null, null);
        } else {
            // 기존 회원 -> 로그인 처리 (accessToken, refreshToken 발급)
            User user = userOptional.get();
            String accessToken = jwtTokenProvider.createAccessToken(user.getId(), university.getId());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
            return new AuthDto.EmailVerifyResponse(false, null, accessToken, refreshToken);
        }
    }

    /**
     * 2-3. 닉네임 중복 검사 (같은 학교 내 유일성)
     */
    public AuthDto.NicknameCheckResponse checkNickname(Long universityId, String nickname) {
        boolean exists = userRepository.existsByUniversityIdAndNickname(universityId, nickname);

        if (!exists) {
            return new AuthDto.NicknameCheckResponse(true, Collections.emptyList());
        }

        // 중복 시 추천 닉네임 후보 생성
        List<String> suggestions = new ArrayList<>();
        String[] candidates = {
                nickname + "2",
                nickname + "이",
                nickname + "_TU",
                nickname + "_" + (secureRandom.nextInt(90) + 10)
        };

        for (String candidate : candidates) {
            if (!userRepository.existsByUniversityIdAndNickname(universityId, candidate)) {
                suggestions.add(candidate);
                if (suggestions.size() >= 3) break;
            }
        }

        return new AuthDto.NicknameCheckResponse(false, suggestions);
    }

    /**
     * 2-4. 회원가입 완료
     */
    @Transactional
    public AuthDto.SignupResponse signup(String signupToken, AuthDto.SignupRequest request) {
        jwtTokenProvider.validateToken(signupToken);
        String email = jwtTokenProvider.getSignupEmail(signupToken);
        Long universityId = jwtTokenProvider.getSignupUniversityId(signupToken);

        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOMAIN_NOT_ALLOWED));

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        if (userRepository.existsByUniversityIdAndNickname(universityId, request.getNickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }

        User user = User.builder()
                .university(university)
                .email(email)
                .nickname(request.getNickname())
                .department(request.getDepartment())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getId(), university.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getId());

        AuthDto.UserSummary userSummary = new AuthDto.UserSummary(
                savedUser.getId(),
                savedUser.getNickname(),
                university.getName()
        );

        return new AuthDto.SignupResponse(accessToken, refreshToken, userSummary);
    }

    /**
     * 2-5. 토큰 재발급
     */
    public AuthDto.TokenResponse refreshToken(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없어요."));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUniversity().getId());
        return new AuthDto.TokenResponse(newAccessToken);
    }

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 이메일 형식이 아니에요.");
        }
        return email.substring(email.indexOf("@") + 1).toLowerCase().trim();
    }
}
