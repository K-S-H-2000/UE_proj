package kr.ac.tukorea.ue.domain.user.service;

import kr.ac.tukorea.ue.domain.university.entity.University;
import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.repository.UserRepository;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private University university;

    @BeforeEach
    void setUp() {
        university = University.builder()
                .name("한국공학대학교")
                .emailDomain("tukorea.ac.kr")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(university, "id", 1L);

        user = User.builder()
                .university(university)
                .email("seunghyun@tukorea.ac.kr")
                .nickname("승현")
                .department("전자공학")
                .profileImageUrl("https://example.com/profile.jpg")
                .saleCount(5)
                .buyCount(2)
                .build();
        ReflectionTestUtils.setField(user, "id", 5L);
    }

    @Test
    @DisplayName("3-1. 내 프로필 조회 시 학과 및 전체 정보 반환")
    void getMyProfile_success() {
        given(userRepository.findById(5L)).willReturn(Optional.of(user));

        UserDto.MyProfileResponse response = userService.getMyProfile(5L);

        assertThat(response.getUserId()).isEqualTo(5L);
        assertThat(response.getNickname()).isEqualTo("승현");
        assertThat(response.getDepartment()).isEqualTo("전자공학");
        assertThat(response.getUniversityName()).isEqualTo("한국공학대학교");
        assertThat(response.getSaleCount()).isEqualTo(5);
        assertThat(response.getBuyCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("3-2. 타인 프로필 조회 시 학과 정보가 노출되지 않음")
    void getPublicProfile_success() {
        given(userRepository.findById(5L)).willReturn(Optional.of(user));

        UserDto.PublicProfileResponse response = userService.getPublicProfile(5L);

        assertThat(response.getUserId()).isEqualTo(5L);
        assertThat(response.getNickname()).isEqualTo("승현");
        assertThat(response.isUniversityVerified()).isTrue();
        assertThat(response.getUniversityName()).isEqualTo("한국공학대학교");
        assertThat(response.getSaleCount()).isEqualTo(5);
        assertThat(response.getBuyCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("3-3. 프로필 수정 시 닉네임 중복 발생 시 NICKNAME_DUPLICATED 예외")
    void updateProfile_nicknameDuplicated() {
        given(userRepository.findById(5L)).willReturn(Optional.of(user));
        given(userRepository.existsByUniversityIdAndNickname(1L, "중복닉네임")).willReturn(true);

        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", "중복닉네임");
        ReflectionTestUtils.setField(request, "department", "기계공학");

        assertThatThrownBy(() -> userService.updateProfile(5L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_DUPLICATED);
    }

    @Test
    @DisplayName("3-3. 프로필 수정 성공")
    void updateProfile_success() {
        given(userRepository.findById(5L)).willReturn(Optional.of(user));
        given(userRepository.existsByUniversityIdAndNickname(1L, "새닉네임")).willReturn(false);

        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        ReflectionTestUtils.setField(request, "nickname", "새닉네임");
        ReflectionTestUtils.setField(request, "department", "기계공학");

        UserDto.MyProfileResponse response = userService.updateProfile(5L, request);

        assertThat(response.getNickname()).isEqualTo("새닉네임");
        assertThat(response.getDepartment()).isEqualTo("기계공학");
    }
}
