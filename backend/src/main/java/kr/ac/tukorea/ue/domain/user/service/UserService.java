package kr.ac.tukorea.ue.domain.user.service;

import kr.ac.tukorea.ue.domain.user.dto.UserDto;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.repository.UserRepository;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 3-1. 내 프로필 조회
     */
    public UserDto.MyProfileResponse getMyProfile(Long userId) {
        User user = findUserById(userId);
        return UserDto.MyProfileResponse.from(user);
    }

    /**
     * 3-2. 타인 프로필 조회 (학과/판매 목록 비공개)
     */
    public UserDto.PublicProfileResponse getPublicProfile(Long targetUserId) {
        User targetUser = findUserById(targetUserId);
        return UserDto.PublicProfileResponse.from(targetUser);
    }

    /**
     * 3-3. 프로필 수정 (닉네임, 학과 변경)
     */
    @Transactional
    public UserDto.MyProfileResponse updateProfile(Long userId, UserDto.UpdateRequest request) {
        User user = findUserById(userId);

        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            boolean duplicated = userRepository.existsByUniversityIdAndNickname(
                    user.getUniversity().getId(),
                    request.getNickname()
            );
            if (duplicated) {
                throw new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
            }
        }

        user.updateProfile(request.getNickname(), request.getDepartment(), null);
        return UserDto.MyProfileResponse.from(user);
    }

    /**
     * 3-4. 프로필 사진 업데이트
     */
    @Transactional
    public UserDto.ProfileImageResponse updateProfileImage(Long userId, String profileImageUrl) {
        User user = findUserById(userId);
        user.updateProfile(null, null, profileImageUrl);
        return new UserDto.ProfileImageResponse(profileImageUrl);
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없어요."));
    }
}
