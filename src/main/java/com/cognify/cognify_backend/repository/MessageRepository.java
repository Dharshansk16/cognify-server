package com.cognify.cognify_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cognify.cognify_backend.model.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByConversationId(String conversationId);
}
