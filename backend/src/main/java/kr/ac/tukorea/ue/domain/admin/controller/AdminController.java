package kr.ac.tukorea.ue.domain.admin.controller;

import jakarta.validation.Valid;
import kr.ac.tukorea.ue.domain.admin.dto.AdminDto;
import kr.ac.tukorea.ue.domain.admin.service.AdminService;
import kr.ac.tukorea.ue.domain.report.entity.ReportStatus;
import kr.ac.tukorea.ue.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 10-1. 신고 목록 조회
     */
    @GetMapping("/reports")
    public ResponseEntity<PageResponse<AdminDto.ReportResponse>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<AdminDto.ReportResponse> response = adminService.getReports(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 10-2. 신고 처리 (조치 내용 기록)
     */
    @PatchMapping("/reports/{reportId}")
    public ResponseEntity<AdminDto.ReportResponse> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody AdminDto.ReportProcessRequest request
    ) {
        AdminDto.ReportResponse response = adminService.processReport(reportId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 10-3. 계정 이용 제한 / 해제
     */
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminDto.UserStatusUpdateRequest request
    ) {
        adminService.updateUserStatus(userId, request.getStatus());
        return ResponseEntity.ok().build();
    }

    /**
     * 10-4. 동의어 목록 조회
     */
    @GetMapping("/synonyms")
    public ResponseEntity<List<AdminDto.SynonymResponse>> getSynonyms() {
        List<AdminDto.SynonymResponse> response = adminService.getSynonyms();
        return ResponseEntity.ok(response);
    }

    /**
     * 10-5. 동의어 추가
     */
    @PostMapping("/synonyms")
    public ResponseEntity<AdminDto.SynonymResponse> createSynonym(
            @Valid @RequestBody AdminDto.SynonymCreateRequest request
    ) {
        AdminDto.SynonymResponse response = adminService.createSynonym(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 10-6. 동의어 삭제
     */
    @DeleteMapping("/synonyms/{synonymId}")
    public ResponseEntity<Void> deleteSynonym(@PathVariable Long synonymId) {
        adminService.deleteSynonym(synonymId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 10-7. 일별 KPI 조회
     */
    @GetMapping("/stats/daily")
    public ResponseEntity<List<AdminDto.KpiDailyResponse>> getDailyKpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<AdminDto.KpiDailyResponse> response = adminService.getDailyKpis(from, to);
        return ResponseEntity.ok(response);
    }
}
