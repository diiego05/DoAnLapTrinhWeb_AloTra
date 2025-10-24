package com.alotra.controller.api;

import com.alotra.dto.ChatMessageDTO;
import com.alotra.dto.ChatRoomDTO;
import com.alotra.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor

public class ChatRoomApiController {

    private final ChatService chatService;

    /**
     * Lấy danh sách tất cả phòng chat (Admin/Manager only)
     * GET /api/chat/rooms
     */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> getAllRooms() {
        List<ChatRoomDTO> rooms = chatService.getActiveRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * Lấy chi tiết phòng chat + lịch sử tin nhắn
     * GET /api/chat/rooms/{roomId}
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDTO> getRoomDetails(@PathVariable Long roomId) {
        ChatRoomDTO room = chatService.getRoomDetails(roomId);
        return ResponseEntity.ok(room);
    }

    /**
     * Tạo hoặc lấy phòng chat của người dùng
     * GET /api/chat/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ChatRoomDTO> getOrCreateUserRoom(@PathVariable String userId) {
        ChatRoomDTO room;

        // Kiểm tra nếu là guest ID (bắt đầu bằng "guest_")
        if (userId.startsWith("guest_")) {
            room = chatService.getOrCreateGuestRoom(userId);
        } else {
            // User đã đăng nhập - PARSE String thành Long
            try {
                Long userIdLong = Long.parseLong(userId);
                room = chatService.getOrCreateUserRoom(userIdLong);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.ok(room);
    }

    /**
     * Đánh dấu tất cả tin nhắn chưa đọc là đã đọc
     * POST /api/chat/rooms/{roomId}/mark-all-read
     */
    @PostMapping("/rooms/{roomId}/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable Long roomId) {
        chatService.markAllAsRead(roomId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All messages marked as read");
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử chat của phòng
     * GET /api/chat/rooms/{roomId}/messages
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(@PathVariable Long roomId) {
        List<ChatMessageDTO> messages = chatService.getChatHistory(roomId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Lấy số lượng tin nhắn chưa đọc
     * GET /api/chat/rooms/{roomId}/unread-count
     */
    @GetMapping("/rooms/{roomId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long roomId) {
        Long count = chatService.getUnreadCount(roomId);
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Đóng phòng chat
     * POST /api/chat/rooms/{roomId}/close
     */
    @PostMapping("/rooms/{roomId}/close")
    public ResponseEntity<ChatRoomDTO> closeRoom(@PathVariable Long roomId) {
        ChatRoomDTO room = chatService.closeRoom(roomId);
        return ResponseEntity.ok(room);
    }
}