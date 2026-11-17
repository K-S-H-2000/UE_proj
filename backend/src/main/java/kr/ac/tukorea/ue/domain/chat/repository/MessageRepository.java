package kr.ac.tukorea.ue.domain.chat.repository;

import kr.ac.tukorea.ue.domain.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByRoomIdOrderByCreatedAtAsc(Long roomId, Pageable pageable);
    Optional<Message> findTopByRoomIdOrderByCreatedAtDesc(Long roomId);
    int countByRoomIdAndSenderIdNotAndCreatedAtAfter(Long roomId, Long senderId, LocalDateTime afterTime);
}
