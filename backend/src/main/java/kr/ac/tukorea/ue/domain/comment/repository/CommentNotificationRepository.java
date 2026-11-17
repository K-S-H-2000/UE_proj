package kr.ac.tukorea.ue.domain.comment.repository;

import kr.ac.tukorea.ue.domain.comment.entity.CommentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentNotificationRepository extends JpaRepository<CommentNotification, Long> {
    Optional<CommentNotification> findByCommentIdAndUserId(Long commentId, Long userId);
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}
