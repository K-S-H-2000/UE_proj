package kr.ac.tukorea.ue.domain.chat.controller;

import jakarta.validation.Valid;
import kr.ac.tukorea.ue.domain.chat.dto.ChatDto;
import kr.ac.tukorea.ue.domain.chat.service.ChatService;
import kr.ac.tukorea.ue.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 7-1. 구매 요청 (채팅방 개설)
     */
    @PostMapping("/products/{productId}/purchase-request")
    public ResponseEntity<ChatDto.RoomCreateResponse> openPurchaseChatRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long productId
    ) {
        ChatDto.RoomCreateResponse response = chatService.openPurchaseChatRoom(
                userPrincipal.getUserId(), productId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 7-2. 거래 요청 (판매자 -> 댓글 작성자)
     */
    @PostMapping("/comments/{commentId}/trade-request")
    public ResponseEntity<ChatDto.RoomCreateResponse> openTradeChatRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long commentId
    ) {
        ChatDto.RoomCreateResponse response = chatService.openTradeChatRoomFromComment(
                userPrincipal.getUserId(), commentId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 7-3. 채팅방 목록 조회
     */
    @GetMapping("/chat/rooms")
    public ResponseEntity<List<ChatDto.RoomListResponse>> getMyChatRooms(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<ChatDto.RoomListResponse> response = chatService.getMyChatRooms(userPrincipal.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * 7-4. 메시지 조회 (역방향 페이징)
     */
    @GetMapping("/chat/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatDto.MessageResponse>> getMessages(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roomId,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Page<ChatDto.MessageResponse> response = chatService.getMessages(
                userPrincipal.getUserId(), roomId, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 7-5. 메시지 전송 (REST)
     */
    @PostMapping("/chat/rooms/{roomId}/messages")
    public ResponseEntity<ChatDto.MessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long roomId,
            @Valid @RequestBody ChatDto.SendMessageRequest request
    ) {
        ChatDto.MessageResponse response = chatService.sendMessage(
                userPrincipal.getUserId(), roomId, request.getContent(), request.getMessageType()
        );
        return ResponseEntity.ok(response);
    }
}
