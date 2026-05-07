package com.alotra.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequestDTO {
    private Long roomId;
    private Long senderId;
    private String content;
    private String senderType; // ADMIN, MANAGER, SUPPORT_STAFF, USER
}