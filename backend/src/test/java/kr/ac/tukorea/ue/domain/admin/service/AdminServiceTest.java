package kr.ac.tukorea.ue.domain.admin.service;

import kr.ac.tukorea.ue.domain.admin.dto.AdminDto;
import kr.ac.tukorea.ue.domain.report.entity.Report;
import kr.ac.tukorea.ue.domain.report.entity.ReportReasonCode;
import kr.ac.tukorea.ue.domain.report.entity.ReportStatus;
import kr.ac.tukorea.ue.domain.report.repository.ReportRepository;
import kr.ac.tukorea.ue.domain.search.entity.Synonym;
import kr.ac.tukorea.ue.domain.search.repository.SynonymRepository;
import kr.ac.tukorea.ue.domain.stat.repository.KpiDailyRepository;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.entity.UserStatus;
import kr.ac.tukorea.ue.domain.user.service.UserService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserService userService;

    @Mock
    private SynonymRepository synonymRepository;

    @Mock
    private KpiDailyRepository kpiDailyRepository;

    @InjectMocks
    private AdminService adminService;

    private User reporter;
    private User reportedUser;
    private Report report;

    @BeforeEach
    void setUp() {
        reporter = User.builder().nickname("신고자").email("reporter@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(reporter, "id", 1L);

        reportedUser = User.builder().nickname("피신고자").email("reported@tukorea.ac.kr").build();
        ReflectionTestUtils.setField(reportedUser, "id", 2L);

        report = Report.builder()
                .reporter(reporter)
                .targetUser(reportedUser)
                .reasonCode(ReportReasonCode.RUDE)
                .detail("욕설 사용")
                .status(ReportStatus.RECEIVED)
                .build();
        ReflectionTestUtils.setField(report, "id", 100L);
    }

    @Test
    @DisplayName("10-2. 관리자 신고 처리 시 상태 RESOLVED 전이 및 관리자 메모 저장")
    void processReport_success() {
        given(reportRepository.findById(100L)).willReturn(Optional.of(report));

        AdminDto.ReportProcessRequest request = new AdminDto.ReportProcessRequest();
        ReflectionTestUtils.setField(request, "status", ReportStatus.RESOLVED);
        ReflectionTestUtils.setField(request, "adminNote", "경고 조치 완료");

        AdminDto.ReportResponse response = adminService.processReport(100L, request);

        assertThat(response.getStatus()).isEqualTo("RESOLVED");
        assertThat(response.getAdminNote()).isEqualTo("경고 조치 완료");
    }

    @Test
    @DisplayName("10-3. 관리자 계정 상태 변경 (RESTRICTED)")
    void updateUserStatus_success() {
        given(userService.findUserById(2L)).willReturn(reportedUser);

        adminService.updateUserStatus(2L, UserStatus.RESTRICTED);

        assertThat(reportedUser.getStatus()).isEqualTo(UserStatus.RESTRICTED);
    }

    @Test
    @DisplayName("10-5. 검색 동의어 추가")
    void createSynonym_success() {
        Synonym synonym = Synonym.builder().keyword("책상").synonym("테이블").build();
        ReflectionTestUtils.setField(synonym, "id", 10L);

        given(synonymRepository.save(any(Synonym.class))).willReturn(synonym);

        AdminDto.SynonymCreateRequest request = new AdminDto.SynonymCreateRequest();
        ReflectionTestUtils.setField(request, "keyword", "책상");
        ReflectionTestUtils.setField(request, "synonym", "테이블");

        AdminDto.SynonymResponse response = adminService.createSynonym(request);

        assertThat(response.getKeyword()).isEqualTo("책상");
        assertThat(response.getSynonym()).isEqualTo("테이블");
    }

    @Test
    @DisplayName("10-6. 검색 동의어 삭제")
    void deleteSynonym_success() {
        given(synonymRepository.existsById(10L)).willReturn(true);

        adminService.deleteSynonym(10L);

        verify(synonymRepository).deleteById(10L);
    }
}
