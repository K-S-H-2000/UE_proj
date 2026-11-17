package kr.ac.tukorea.ue.domain.stat.repository;

import kr.ac.tukorea.ue.domain.stat.entity.KpiDaily;
import kr.ac.tukorea.ue.domain.stat.entity.KpiDailyId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface KpiDailyRepository extends JpaRepository<KpiDaily, KpiDailyId> {
    List<KpiDaily> findByIdStatDateBetweenOrderByIdStatDateAsc(LocalDate from, LocalDate to);
}
