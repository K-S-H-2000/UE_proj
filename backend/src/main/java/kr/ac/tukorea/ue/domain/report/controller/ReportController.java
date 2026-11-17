package kr.ac.tukorea.ue.domain.report.controller;

import jakarta.validation.Valid;
import kr.ac.tukorea.ue.domain.report.dto.ReportDto;
import kr.ac.tukorea.ue.domain.report.service.ReportService;
import kr.ac.tukorea.ue.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 9-3. 신고 접수
     */
    @PostMapping
    public ResponseEntity<ReportDto.CreateResponse> createReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ReportDto.CreateRequest request
    ) {
        ReportDto.CreateResponse response = reportService.createReport(userPrincipal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
