package kr.ac.tukorea.ue.domain.comment.service;

import kr.ac.tukorea.ue.domain.comment.dto.CommentDto;
import kr.ac.tukorea.ue.domain.comment.entity.Comment;
import kr.ac.tukorea.ue.domain.comment.entity.CommentLike;
import kr.ac.tukorea.ue.domain.comment.entity.CommentNotification;
import kr.ac.tukorea.ue.domain.comment.repository.CommentLikeRepository;
import kr.ac.tukorea.ue.domain.comment.repository.CommentNotificationRepository;
import kr.ac.tukorea.ue.domain.comment.repository.CommentRepository;
import kr.ac.tukorea.ue.domain.interaction.repository.BlockRepository;
import kr.ac.tukorea.ue.domain.product.entity.Product;
import kr.ac.tukorea.ue.domain.product.service.ProductService;
import kr.ac.tukorea.ue.domain.user.entity.User;
import kr.ac.tukorea.ue.domain.user.service.UserService;
import kr.ac.tukorea.ue.global.error.BusinessException;
import kr.ac.tukorea.ue.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentNotificationRepository commentNotificationRepository;
    private final ProductService productService;
    private final UserService userService;
    private final BlockRepository blockRepository;

    private static final String DELETED_COMMENT_MESSAGE = "삭제된 댓글입니다.";

    /**
     * 6-1. 댓글 목록 조회 (대댓글 2단계 중첩)
     */
    public List<CommentDto.Response> getComments(Long universityId, Long userId, Long productId) {
        Product product = productService.findProductById(productId);

        if (!product.getUniversity().getId().equals(universityId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 학교의 상품이 아니에요.");
        }

        List<Long> blockedUserIds = blockRepository.findAllMutualBlockedUserIds(userId);
        List<Comment> allComments = commentRepository.findByProductIdOrderByCreatedAtAsc(productId);

        // 1. 대댓글을 부모 댓글 ID별로 그룹화
        Map<Long, List<Comment>> repliesMap = new HashMap<>();
        List<Comment> rootComments = new ArrayList<>();

        for (Comment c : allComments) {
            // 차단된 작성자의 댓글 제외 (단, 삭제되었더라도 대댓글이 살아있을 수 있으므로 대댓글 유무 고려)
            if (blockedUserIds.contains(c.getUser().getId())) {
                continue;
            }

            if (c.getParent() == null) {
                rootComments.add(c);
            } else {
                repliesMap.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c);
            }
        }

        Long sellerId = product.getSeller().getId();

        // 2. 루트 댓글 및 대댓글 응답 DTO 구성
        List<CommentDto.Response> response = new ArrayList<>();
        for (Comment root : rootComments) {
            List<Comment> replyList = repliesMap.getOrDefault(root.getId(), Collections.emptyList());

            List<CommentDto.Response> replyDtos = replyList.stream()
                    .map(r -> toResponseDto(r, userId, sellerId, Collections.emptyList()))
                    .toList();

            response.add(toResponseDto(root, userId, sellerId, replyDtos));
        }

        return response;
    }

    /**
     * 6-2. 댓글 작성
     */
    @Transactional
    public CommentDto.CreateResponse createComment(
            Long universityId, Long userId, Long productId, CommentDto.CreateRequest request
    ) {
        Product product = productService.findProductById(productId);

        if (!product.getUniversity().getId().equals(universityId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "해당 학교의 상품이 아니에요.");
        }

        if (blockRepository.existsByBlockerIdAndBlockedId(userId, product.getSeller().getId()) ||
            blockRepository.existsByBlockerIdAndBlockedId(product.getSeller().getId(), userId)) {
            throw new BusinessException(ErrorCode.BLOCKED_RELATION);
        }

        User user = userService.findUserById(userId);
        Comment parent = null;

        if (request.getParentCommentId() != null) {
            parent = findCommentById(request.getParentCommentId());

            // CMT-08: 대댓글에는 답글을 달 수 없음 (2단계 제한)
            if (parent.getParent() != null) {
                throw new BusinessException(ErrorCode.REPLY_DEPTH_EXCEEDED);
            }

            // CMT-08: 대댓글에는 가격 제안 불가
            if (request.getOfferPrice() != null) {
                throw new BusinessException(ErrorCode.OFFER_NOT_ALLOWED_ON_REPLY);
            }
        } else {
            // CMT-01a: 판매자는 가격 제안 불가
            if (product.getSeller().getId().equals(userId) && request.getOfferPrice() != null) {
                throw new BusinessException(ErrorCode.OFFER_NOT_ALLOWED_FOR_SELLER);
            }
        }

        Comment comment = Comment.builder()
                .product(product)
                .user(user)
                .parent(parent)
                .content(request.getContent())
                .offerPrice(request.getOfferPrice())
                .likeCount(0)
                .build();

        Comment saved = commentRepository.save(comment);

        return CommentDto.CreateResponse.builder()
                .commentId(saved.getId())
                .build();
    }

    /**
     * 6-3. 댓글 삭제 (CMT-03: 대댓글이 있으면 소프트 딜리트, 없으면 하드 딜리트)
     */
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = findCommentById(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 댓글만 삭제할 수 있어요.");
        }

        if (!comment.getChildren().isEmpty()) {
            comment.delete();
        } else {
            commentRepository.delete(comment);
        }
    }

    /**
     * 6-4. 좋아요 토글
     */
    @Transactional
    public CommentDto.LikeResponse toggleLike(Long userId, Long commentId) {
        Comment comment = findCommentById(commentId);
        User user = userService.findUserById(userId);

        boolean alreadyLiked = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);

        if (alreadyLiked) {
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            comment.decrementLikeCount();
            return new CommentDto.LikeResponse(false, comment.getLikeCount());
        } else {
            CommentLike like = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentLikeRepository.save(like);
            comment.incrementLikeCount();
            return new CommentDto.LikeResponse(true, comment.getLikeCount());
        }
    }

    /**
     * 6-5. 답글 알림 토글
     */
    @Transactional
    public CommentDto.NotificationResponse toggleNotification(Long userId, Long commentId) {
        Comment comment = findCommentById(commentId);
        User user = userService.findUserById(userId);

        boolean alreadyEnabled = commentNotificationRepository.existsByCommentIdAndUserId(commentId, userId);

        if (alreadyEnabled) {
            commentNotificationRepository.deleteByCommentIdAndUserId(commentId, userId);
            return new CommentDto.NotificationResponse(false);
        } else {
            CommentNotification notification = CommentNotification.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentNotificationRepository.save(notification);
            return new CommentDto.NotificationResponse(true);
        }
    }

    public Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없어요."));
    }

    private CommentDto.Response toResponseDto(
            Comment comment, Long currentUserId, Long sellerId, List<CommentDto.Response> replies
    ) {
        boolean isLiked = commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);
        boolean isMine = comment.getUser().getId().equals(currentUserId);
        boolean isSeller = comment.getUser().getId().equals(sellerId);
        boolean notifEnabled = commentNotificationRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);

        String displayContent = comment.isDeleted() ? DELETED_COMMENT_MESSAGE : comment.getContent();

        return CommentDto.Response.builder()
                .commentId(comment.getId())
                .user(CommentDto.UserInfo.builder()
                        .userId(comment.getUser().getId())
                        .nickname(comment.getUser().getNickname())
                        .build())
                .content(displayContent)
                .offerPrice(comment.getOfferPrice())
                .likeCount(comment.getLikeCount())
                .isLiked(isLiked)
                .isMine(isMine)
                .isSeller(isSeller)
                .notificationEnabled(notifEnabled)
                .createdAt(comment.getCreatedAt())
                .replies(replies.isEmpty() ? null : replies)
                .build();
    }
}
