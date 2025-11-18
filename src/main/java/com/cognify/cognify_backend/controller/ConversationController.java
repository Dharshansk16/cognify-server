package com.cognify.cognify_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cognify.cognify_backend.dto.ChatRequest;
import com.cognify.cognify_backend.dto.ChatResponse;
import com.cognify.cognify_backend.dto.ConversationResponse;
import com.cognify.cognify_backend.model.User;
import com.cognify.cognify_backend.service.ConversationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {
        ChatResponse response = conversationService.chat(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal User user) {
        List<ConversationResponse> conversations = conversationService.getUserConversations(user);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable String conversationId,
            @AuthenticationPrincipal User user) {
        ConversationResponse conversation = conversationService.getConversation(conversationId, user);
        return ResponseEntity.ok(conversation);
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String conversationId,
            @AuthenticationPrincipal User user) {
        conversationService.deleteConversation(conversationId, user);
        return ResponseEntity.noContent().build();
    }
}
