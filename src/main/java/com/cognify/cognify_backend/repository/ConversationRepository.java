package com.cognify.cognify_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cognify.cognify_backend.model.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findByUserId(String userId);
    List<Conversation> findByPersonaId(String personaId);
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);
}
