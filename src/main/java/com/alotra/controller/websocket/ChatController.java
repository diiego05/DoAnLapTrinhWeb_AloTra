package com.alotra.controller.websocket;

import com.alotra.dto.ChatMessageDTO;
import com.alotra.dto.ChatMessageRequestDTO;
import com.alotra.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi tin nhắn trong phòng chat
     * Client gửi tới: /app/chat/send
     * Format: {"roomId": 1, "senderId": 1, "content": "Hello", "senderType": "USER"}
     */
    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessageRequestDTO request) {
        try {
            ChatMessageDTO messageDTO = chatService.saveMessage(
                request.getRoomId(),
                request.getSenderId(),
                request.getContent(),
                request.getSenderType()
            );

            // Gửi tin nhắn tới tất cả subscriber trong phòng
            messagingTemplate.convertAndSend(
                    "/topic/chat/room/" + request.getRoomId(),
                    messageDTO
            );
        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(
                request.getSenderId().toString(),
                "/queue/errors",
                "Error: " + e.getMessage()
            );
        }
    }

    /**
     * Người dùng tham gia phòng chat
     * Client gửi tới: /app/chat/join/{roomId}
     */
    @MessageMapping("/chat/join/{roomId}")
    public void joinRoom(@DestinationVariable Long roomId) {
        messagingTemplate.convertAndSend(
                "/topic/chat/room/" + roomId,
                new java.util.HashMap<String, Object>() {{
                    put("type", "USER_JOINED");
                    put("message", "User joined the room");
                    put("timestamp", java.time.LocalDateTime.now());
                }}
        );
    }

    /**
     * Người dùng rời khỏi phòng chat
     * Client gửi tới: /app/chat/leave/{roomId}
     */
    @MessageMapping("/chat/leave/{roomId}")
    public void leaveRoom(@DestinationVariable Long roomId) {
        messagingTemplate.convertAndSend(
                "/topic/chat/room/" + roomId,
                new java.util.HashMap<String, Object>() {{
                    put("type", "USER_LEFT");
                    put("message", "User left the room");
                    put("timestamp", java.time.LocalDateTime.now());
                }}
        );
    }

    /**
     * Chỉ báo người dùng đang gõ tin nhắn
     * Client gửi tới: /app/chat/typing/{roomId}/{userId}
     */
    @MessageMapping("/chat/typing/{roomId}/{userId}")
    public void typingIndicator(@DestinationVariable Long roomId, @DestinationVariable Long userId) {
        messagingTemplate.convertAndSend(
                "/topic/chat/room/" + roomId,
                new java.util.HashMap<String, Object>() {{
                    put("type", "TYPING");
                    put("userId", userId);
                    put("timestamp", java.time.LocalDateTime.now());
                }}
        );
    }
}