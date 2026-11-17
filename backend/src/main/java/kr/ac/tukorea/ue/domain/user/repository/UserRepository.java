package kr.ac.tukorea.ue.domain.user.repository;

import kr.ac.tukorea.ue.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUniversityIdAndNickname(Long universityId, String nickname);
    boolean existsByUniversityIdAndNickname(Long universityId, String nickname);
    boolean existsByEmail(String email);
}
