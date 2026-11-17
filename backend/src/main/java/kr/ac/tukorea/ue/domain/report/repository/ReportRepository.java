package kr.ac.tukorea.ue.domain.report.repository;

import kr.ac.tukorea.ue.domain.report.entity.Report;
import kr.ac.tukorea.ue.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findByReporterId(Long reporterId, Pageable pageable);
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
}
