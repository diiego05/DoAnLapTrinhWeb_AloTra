package com.alotra.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ChatMessages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoomId", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SenderId", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false, length = 50)
    private String senderType; // ADMIN, MANAGER, SUPPORT_STAFF, USER

    @PrePersist
    public void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}