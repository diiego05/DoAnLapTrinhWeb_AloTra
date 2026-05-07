package com.alotra.repository;

import com.alotra.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomIdOrderByTimestampAsc(Long chatRoomId);

    List<ChatMessage> findByChatRoomIdAndIsReadFalse(Long chatRoomId);

    Long countByChatRoomIdAndIsReadFalse(Long chatRoomId);
}