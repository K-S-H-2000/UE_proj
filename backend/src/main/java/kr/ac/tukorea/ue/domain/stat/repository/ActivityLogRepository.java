package kr.ac.tukorea.ue.domain.stat.repository;

import kr.ac.tukorea.ue.domain.stat.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
}
