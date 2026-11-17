package kr.ac.tukorea.ue.domain.chat.controller;

import kr.ac.tukorea.ue.domain.chat.dto.ChatDto;
import kr.ac.tukorea.ue.domain.chat.service.ChatService;
import kr.ac.tukorea.ue.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 7-5. WebSocket 메시지 수신 및 브로드캐스트
     * 발행: /pub/chat/message
     * 구독: /sub/chat/room/{roomId}
     */
    @MessageMapping("/chat/message")
    public void handleMessage(ChatDto.SendMessageRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long senderId = principal.getUserId();

        log.info("WS message received from user {} in room {}: {}", senderId, request.getRoomId(), request.getContent());

        ChatDto.MessageResponse response = chatService.sendMessage(
                senderId, request.getRoomId(), request.getContent(), request.getMessageType()
        );

        // 방 참여자들에게 실시간 브로드캐스트
        messagingTemplate.convertAndSend("/sub/chat/room/" + request.getRoomId(), response);
    }
}
