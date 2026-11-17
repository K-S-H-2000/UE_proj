package kr.ac.tukorea.ue.domain.interaction.repository;

import kr.ac.tukorea.ue.domain.interaction.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    List<Block> findByBlockerIdOrderByCreatedAtDesc(Long blockerId);

    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :blockerId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);

    @Query("SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :userId UNION SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId")
    List<Long> findAllMutualBlockedUserIds(@Param("userId") Long userId);
}
