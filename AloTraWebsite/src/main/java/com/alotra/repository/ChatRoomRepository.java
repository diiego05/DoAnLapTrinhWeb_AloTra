package com.alotra.repository;


import com.alotra.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByUserId(Long userId);

    List<ChatRoom> findByStatus(String status);

    List<ChatRoom> findAllByOrderByCreatedAtDesc();
}