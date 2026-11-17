package kr.ac.tukorea.ue.domain.chat.repository;

import kr.ac.tukorea.ue.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);
    List<ChatRoom> findByBuyerIdOrderByLastMessageAtDesc(Long buyerId);
    List<ChatRoom> findBySellerIdOrderByLastMessageAtDesc(Long sellerId);
}
