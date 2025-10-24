package com.alotra.service;


import com.alotra.dto.ChatMessageDTO;
import com.alotra.dto.ChatRoomDTO;
import com.alotra.entity.ChatMessage;
import com.alotra.entity.ChatRoom;
import com.alotra.entity.Role;
import com.alotra.entity.User;
import com.alotra.repository.ChatMessageRepository;
import com.alotra.repository.ChatRoomRepository;
import com.alotra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    @Transactional
    public ChatRoomDTO getOrCreateUserRoom(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom room = roomRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setUser(user);
                    newRoom.setStatus("ACTIVE");
                    return roomRepository.save(newRoom);
                });

        return mapRoomToDTO(room);
    }

    /**
     * Tạo hoặc lấy phòng chat của người dùng
     */
    @Transactional
    public ChatRoomDTO getOrCreateGuestRoom(String guestId) {
        // Tìm hoặc tạo user guest
        User guestUser = userRepository.findByEmail(guestId + "@guest.temp")
                .orElseGet(() -> {
                    User newGuest = new User();
                    newGuest.setEmail(guestId + "@guest.temp");

                    // Tạo tên "Khách Hàng X"
                    long guestCount = userRepository.countByEmailContaining("@guest.temp") + 1;
                    newGuest.setFullName("Khách Hàng " + guestCount);

                    newGuest.setPhone("0000000000");

                    // ✅ SỬA: Dùng setPasswordHash thay vì setPassword
                    newGuest.setPasswordHash("GUEST_NO_LOGIN");

                    // ✅ THÊM: Set status và role
                    newGuest.setStatus("ACTIVE");

                    // ✅ THÊM: Set role mặc định (giả sử role USER có id = 3)
                    Role guestRole = new Role();
                    guestRole.setId(3L); // ID của role USER
                    newGuest.setRole(guestRole);

                    return userRepository.save(newGuest);
                });

        // Tạo phòng chat cho guest
        ChatRoom room = roomRepository.findByUserId(guestUser.getId())
                .orElseGet(() -> {
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setUser(guestUser);
                    newRoom.setStatus("ACTIVE");
                    return roomRepository.save(newRoom);
                });

        return mapRoomToDTO(room);
    }

    /**
     * Lưu tin nhắn vào database
     */
    @Transactional
    public ChatMessageDTO saveMessage(Long roomId, Long senderId, String content, String senderType) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        ChatMessage message = new ChatMessage();
        message.setChatRoom(room);
        message.setSender(sender);
        message.setContent(content);
        message.setSenderType(senderType);
        message.setIsRead(false);

        ChatMessage savedMessage = messageRepository.save(message);
        return mapMessageToDTO(savedMessage);
    }

    /**
     * Lấy lịch sử chat
     */
    public List<ChatMessageDTO> getChatHistory(Long roomId) {
        List<ChatMessage> messages = messageRepository.findByChatRoomIdOrderByTimestampAsc(roomId);
        return messages.stream()
                .map(this::mapMessageToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Đánh dấu tất cả tin nhắn chưa đọc là đã đọc
     */
    @Transactional
    public void markAllAsRead(Long roomId) {
        List<ChatMessage> unreadMessages = messageRepository.findByChatRoomIdAndIsReadFalse(roomId);
        unreadMessages.forEach(msg -> msg.setIsRead(true));
        messageRepository.saveAll(unreadMessages);
    }

    /**
     * Lấy danh sách phòng chat active
     */
    public List<ChatRoomDTO> getActiveRooms() {
        List<ChatRoom> rooms = roomRepository.findByStatus("ACTIVE");
        return rooms.stream()
                .map(this::mapRoomToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết phòng chat
     */
    public ChatRoomDTO getRoomDetails(Long roomId) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        ChatRoomDTO dto = mapRoomToDTO(room);
        List<ChatMessage> messages = messageRepository.findByChatRoomIdOrderByTimestampAsc(roomId);
        dto.setMessages(
            messages.stream()
                .map(this::mapMessageToDTO)
                .collect(Collectors.toList())
        );

        return dto;
    }

    /**
     * Đóng phòng chat
     */
    @Transactional
    public ChatRoomDTO closeRoom(Long roomId) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        room.setStatus("CLOSED");
        room.setClosedAt(LocalDateTime.now());
        roomRepository.save(room);
        return mapRoomToDTO(room);
    }

    /**
     * Lấy số lượng tin nhắn chưa đọc
     */
    public Long getUnreadCount(Long roomId) {
        return messageRepository.countByChatRoomIdAndIsReadFalse(roomId);
    }

    private ChatRoomDTO mapRoomToDTO(ChatRoom room) {
        ChatRoomDTO dto = new ChatRoomDTO();
        dto.setId(room.getId());
        dto.setUserId(room.getUser().getId());
        dto.setUserName(room.getUser().getFullName());
        dto.setUserEmail(room.getUser().getEmail());
        dto.setUserPhone(room.getUser().getPhone());
        dto.setCreatedAt(room.getCreatedAt());
        dto.setStatus(room.getStatus());
        dto.setClosedAt(room.getClosedAt());
        dto.setUnreadCount(messageRepository.countByChatRoomIdAndIsReadFalse(room.getId()));
        return dto;
    }

    private ChatMessageDTO mapMessageToDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setRoomId(message.getChatRoom().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFullName());
        dto.setSenderEmail(message.getSender().getEmail());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        dto.setIsRead(message.getIsRead());
        dto.setSenderType(message.getSenderType());
        return dto;
    }
}