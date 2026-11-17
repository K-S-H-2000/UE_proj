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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationCodeStore codeStore;

    @Mock
    private EmailSender emailSender;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private University university;

    @BeforeEach
    void setUp() {
        university = University.builder()
                .name("한국공학대학교")
                .emailDomain("tukorea.ac.kr")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(university, "id", 1L);
    }

    @Test
    @DisplayName("2-1. 허용되지 않은 이메일 도메인 발송 시 DOMAIN_NOT_ALLOWED 예외 발생")
    void sendVerificationEmail_domainNotAllowed() {
        given(universityRepository.findByEmailDomain("gmail.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.sendVerificationEmail("test@gmail.com"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOMAIN_NOT_ALLOWED);
    }

    @Test
    @DisplayName("2-1. 정상 대학 도메인 이메일 발송 성공")
    void sendVerificationEmail_success() {
        given(universityRepository.findByEmailDomain("tukorea.ac.kr")).willReturn(Optional.of(university));

        AuthDto.EmailSendResponse response = authService.sendVerificationEmail("test@tukorea.ac.kr");

        assertThat(response.getUniversityName()).isEqualTo("한국공학대학교");
        assertThat(response.getExpiresInSeconds()).isEqualTo(600);
        verify(emailSender).sendVerificationCode(eq("test@tukorea.ac.kr"), anyString());
        verify(codeStore).save(eq("test@tukorea.ac.kr"), anyString(), eq(600L));
    }

    @Test
    @DisplayName("2-2. 인증 코드 불일치 시 CODE_INVALID 예외 발생")
    void verifyCode_invalidCode() {
        given(codeStore.verify("test@tukorea.ac.kr", "123456")).willReturn(false);

        assertThatThrownBy(() -> authService.verifyCode("test@tukorea.ac.kr", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CODE_INVALID);
    }

    @Test
    @DisplayName("2-2. 신규 사용자 인증 성공 시 signupToken 발급")
    void verifyCode_newUser_success() {
        given(codeStore.verify("new@tukorea.ac.kr", "123456")).willReturn(true);
        given(universityRepository.findByEmailDomain("tukorea.ac.kr")).willReturn(Optional.of(university));
        given(userRepository.findByEmail("new@tukorea.ac.kr")).willReturn(Optional.empty());
        given(jwtTokenProvider.createSignupToken("new@tukorea.ac.kr", 1L)).willReturn("mock-signup-token");

        AuthDto.EmailVerifyResponse response = authService.verifyCode("new@tukorea.ac.kr", "123456");

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.getSignupToken()).isEqualTo("mock-signup-token");
        assertThat(response.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("2-2. 기존 사용자 인증 성공 시 로그인 토큰(access/refresh) 발급")
    void verifyCode_existingUser_success() {
        User user = User.builder()
                .university(university)
                .email("exist@tukorea.ac.kr")
                .nickname("기존유저")
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        given(codeStore.verify("exist@tukorea.ac.kr", "123456")).willReturn(true);
        given(universityRepository.findByEmailDomain("tukorea.ac.kr")).willReturn(Optional.of(university));
        given(userRepository.findByEmail("exist@tukorea.ac.kr")).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(10L, 1L)).willReturn("mock-access-token");
        given(jwtTokenProvider.createRefreshToken(10L)).willReturn("mock-refresh-token");

        AuthDto.EmailVerifyResponse response = authService.verifyCode("exist@tukorea.ac.kr", "123456");

        assertThat(response.isNewUser()).isFalse();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
    }

    @Test
    @DisplayName("2-3. 사용 가능한 닉네임인 경우 available=true 반환")
    void checkNickname_available() {
        given(userRepository.existsByUniversityIdAndNickname(1L, "승현")).willReturn(false);

        AuthDto.NicknameCheckResponse response = authService.checkNickname(1L, "승현");

        assertThat(response.isAvailable()).isTrue();
        assertThat(response.getSuggestions()).isEmpty();
    }

    @Test
    @DisplayName("2-3. 중복된 닉네임인 경우 available=false 및 추천 닉네임 반환")
    void checkNickname_duplicated() {
        given(userRepository.existsByUniversityIdAndNickname(1L, "승현")).willReturn(true);

        AuthDto.NicknameCheckResponse response = authService.checkNickname(1L, "승현");

        assertThat(response.isAvailable()).isFalse();
        assertThat(response.getSuggestions()).isNotEmpty();
    }

    @Test
    @DisplayName("2-4. 회원가입 시 닉네임 중복이면 NICKNAME_DUPLICATED 예외 발생")
    void signup_nicknameDuplicated() {
        given(jwtTokenProvider.getSignupEmail("valid-token")).willReturn("new@tukorea.ac.kr");
        given(jwtTokenProvider.getSignupUniversityId("valid-token")).willReturn(1L);
        given(universityRepository.findById(1L)).willReturn(Optional.of(university));
        given(userRepository.existsByEmail("new@tukorea.ac.kr")).willReturn(false);
        given(userRepository.existsByUniversityIdAndNickname(1L, "중복닉네임")).willReturn(true);

        AuthDto.SignupRequest request = new AuthDto.SignupRequest();
        ReflectionTestUtils.setField(request, "nickname", "중복닉네임");
        ReflectionTestUtils.setField(request, "department", "컴퓨터공학");

        assertThatThrownBy(() -> authService.signup("valid-token", request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_DUPLICATED);
    }
}
