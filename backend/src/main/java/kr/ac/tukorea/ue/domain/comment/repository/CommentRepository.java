package kr.ac.tukorea.ue.domain.comment.repository;

import kr.ac.tukorea.ue.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByProductIdAndParentIsNullOrderByCreatedAtAsc(Long productId);
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
    List<Comment> findByProductIdOrderByCreatedAtAsc(Long productId);
    int countByProductIdAndDeletedAtIsNull(Long productId);
}
