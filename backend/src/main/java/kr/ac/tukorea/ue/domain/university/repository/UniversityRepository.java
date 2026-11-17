package kr.ac.tukorea.ue.domain.university.repository;

import kr.ac.tukorea.ue.domain.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Long> {
    Optional<University> findByEmailDomain(String emailDomain);
    boolean existsByEmailDomain(String emailDomain);
}
