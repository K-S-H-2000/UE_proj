package kr.ac.tukorea.ue.domain.report.service;

import kr.ac.tukorea.ue.domain.comment.entity.Comment;
import kr.ac.tukorea.ue.domain.comment.service.CommentService;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.service.ProductService;
import kr.ac.tukorea.ue.domain.report.dto.ReportDto;
import kr.ac.tukorea.ue.domain.report.entity.Report;
import kr.ac.tukorea.ue.domain.report.entity.ReportReasonCode;
import kr.ac.tukorea.ue.domain.report.entity.ReportStatus;
import kr.ac.tukorea.ue.domain.report.repository.ReportRepository;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;
    private final ProductService productService;
    private final CommentService commentService;

    /**
     * 9-3. 신고 접수
     */
    @Transactional
    public ReportDto.CreateResponse createReport(Long reporterId, ReportDto.CreateRequest request) {
        User reporter = userService.findUserById(reporterId);

        Product targetProduct = null;
        Comment targetComment = null;
        User targetUser = null;

        String type = request.getTargetType().toUpperCase();
        switch (type) {
            case "PRODUCT" -> {
                targetProduct = productService.findProductById(request.getTargetId());
                if (targetProduct.getSeller().getId().equals(reporterId)) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "본인의 상품은 신고할 수 없어요.");
                }
            }
            case "COMMENT" -> {
                targetComment = commentService.findCommentById(request.getTargetId());
                if (targetComment.getUser().getId().equals(reporterId)) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "본인의 댓글은 신고할 수 없어요.");
                }
            }
            case "USER" -> {
                if (request.getTargetId().equals(reporterId)) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT, "본인은 신고할 수 없어요.");
                }
                targetUser = userService.findUserById(request.getTargetId());
            }
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 신고 대상 타입이 아니에요.");
        }

        ReportReasonCode reasonCode;
        try {
            reasonCode = ReportReasonCode.valueOf(request.getReasonCode().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "올바른 신고 사유가 아니에요.");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetProduct(targetProduct)
                .targetComment(targetComment)
                .targetUser(targetUser)
                .reasonCode(reasonCode)
                .detail(request.getDetail())
                .status(ReportStatus.RECEIVED)
                .build();

        Report saved = reportRepository.save(report);

        return ReportDto.CreateResponse.builder()
                .reportId(saved.getId())
                .build();
    }
}
