package kr.ac.tukorea.ue.domain.comment.controller;

import jakarta.validation.Valid;
import kr.ac.tukorea.ue.domain.comment.dto.CommentDto;
import kr.ac.tukorea.ue.domain.comment.service.CommentService;
import kr.ac.tukorea.ue.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 6-1. 댓글 목록 조회
     */
    @GetMapping("/products/{productId}/comments")
    public ResponseEntity<List<CommentDto.Response>> getComments(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId
    ) {
        List<CommentDto.Response> response = commentService.getComments(
                userPrincipal.getUniversityId(), userPrincipal.getUserId(), productId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 6-2. 댓글 작성
     */
    @PostMapping("/products/{productId}/comments")
    public ResponseEntity<CommentDto.CreateResponse> createComment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId,
            @Valid @RequestBody CommentDto.CreateRequest request
    ) {
        CommentDto.CreateResponse response = commentService.createComment(
                userPrincipal.getUniversityId(), userPrincipal.getUserId(), productId, request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 6-3. 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(userPrincipal.getUserId(), commentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 6-4. 댓글 좋아요 등록 / 해제 (토글)
     */
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<CommentDto.LikeResponse> addLike(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long commentId
    ) {
        CommentDto.LikeResponse response = commentService.toggleLike(userPrincipal.getUserId(), commentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<CommentDto.LikeResponse> removeLike(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long commentId
    ) {
        CommentDto.LikeResponse response = commentService.toggleLike(userPrincipal.getUserId(), commentId);
        return ResponseEntity.ok(response);
    }

    /**
     * 6-5. 답글 알림 토글
     */
    @PostMapping("/comments/{commentId}/notification")
    public ResponseEntity<CommentDto.NotificationResponse> enableNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long commentId
    ) {
        CommentDto.NotificationResponse response = commentService.toggleNotification(userPrincipal.getUserId(), commentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}/notification")
    public ResponseEntity<CommentDto.NotificationResponse> disableNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long commentId
    ) {
        CommentDto.NotificationResponse response = commentService.toggleNotification(userPrincipal.getUserId(), commentId);
        return ResponseEntity.ok(response);
    }
}
