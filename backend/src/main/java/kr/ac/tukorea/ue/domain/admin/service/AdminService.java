package kr.ac.tukorea.ue.domain.admin.service;

import kr.ac.tukorea.ue.domain.admin.dto.AdminDto;
import kr.ac.tukorea.ue.domain.report.entity.Report;
import kr.ac.tukorea.ue.domain.report.entity.ReportStatus;
import kr.ac.tukorea.ue.domain.report.repository.ReportRepository;
import kr.ac.tukorea.ue.domain.search.entity.Synonym;
import kr.ac.tukorea.ue.domain.search.repository.SynonymRepository;
import kr.ac.tukorea.ue.domain.stat.entity.KpiDaily;
import kr.ac.tukorea.ue.domain.stat.repository.KpiDailyRepository;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.entity.UserStatus;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.dto.PageResponse;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final SynonymRepository synonymRepository;
    private final KpiDailyRepository kpiDailyRepository;

    /**
     * 10-1. 신고 목록 조회
     */
    public PageResponse<AdminDto.ReportResponse> getReports(ReportStatus status, Pageable pageable) {
        Page<Report> page = (status != null)
                ? reportRepository.findByStatus(status, pageable)
                : reportRepository.findAll(pageable);

        return PageResponse.from(page.map(AdminDto.ReportResponse::from));
    }

    /**
     * 10-2. 신고 처리 (조치 내용 및 상태 기록)
     */
    @Transactional
    public AdminDto.ReportResponse processReport(Long reportId, AdminDto.ReportProcessRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신고 내역을 찾을 수 없어요."));

        report.resolve(request.getAdminNote());
        return AdminDto.ReportResponse.from(report);
    }

    /**
     * 10-3. 계정 이용 제한 / 해제
     */
    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = userService.findUserById(userId);
        user.updateStatus(status);
    }

    /**
     * 10-4. 동의어 목록 조회
     */
    public List<AdminDto.SynonymResponse> getSynonyms() {
        return synonymRepository.findAll().stream()
                .map(AdminDto.SynonymResponse::from)
                .toList();
    }

    /**
     * 10-5. 동의어 추가
     */
    @Transactional
    public AdminDto.SynonymResponse createSynonym(AdminDto.SynonymCreateRequest request) {
        Synonym synonym = Synonym.builder()
                .keyword(request.getKeyword())
                .synonym(request.getSynonym())
                .build();

        Synonym saved = synonymRepository.save(synonym);
        return AdminDto.SynonymResponse.from(saved);
    }

    /**
     * 10-6. 동의어 삭제
     */
    @Transactional
    public void deleteSynonym(Long synonymId) {
        if (!synonymRepository.existsById(synonymId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "동의어를 찾을 수 없어요.");
        }
        synonymRepository.deleteById(synonymId);
    }

    /**
     * 10-7. 일별 KPI 조회
     */
    public List<AdminDto.KpiDailyResponse> getDailyKpis(LocalDate from, LocalDate to) {
        LocalDate startDate = (from != null) ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = (to != null) ? to : LocalDate.now();

        List<KpiDaily> kpis = kpiDailyRepository.findByIdStatDateBetweenOrderByIdStatDateAsc(startDate, endDate);
        return kpis.stream()
                .map(AdminDto.KpiDailyResponse::from)
                .toList();
    }
}
