package com.alotra.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private LocalDateTime createdAt;
    private String status;
    private LocalDateTime closedAt;
    private Long unreadCount;
    private List<ChatMessageDTO> messages;
}