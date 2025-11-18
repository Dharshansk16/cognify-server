package com.cognify.cognify_backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cognify.cognify_backend.dto.ChatRequest;
import com.cognify.cognify_backend.dto.ChatResponse;
import com.cognify.cognify_backend.dto.ConversationResponse;
import com.cognify.cognify_backend.dto.MessageResponse;
import com.cognify.cognify_backend.model.Conversation;
import com.cognify.cognify_backend.model.Message;
import com.cognify.cognify_backend.model.Persona;
import com.cognify.cognify_backend.model.User;
import com.cognify.cognify_backend.repository.ConversationRepository;
import com.cognify.cognify_backend.repository.MessageRepository;
import com.cognify.cognify_backend.repository.PersonaRepository;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PersonaRepository personaRepository;
    private final HybridRetrievalService retrievalService;
    private final ChatLanguageModel gemini;

    @Transactional
    public ChatResponse chat(ChatRequest request, User user) {
        // Get persona
        String personaId = request.getPersonaId();
        if (personaId == null) {
            throw new RuntimeException("Persona ID is required");
        }
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new RuntimeException("Persona not found"));
        
        // Verify ownership
        if (!persona.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have access to this persona");
        }
        
        // Get or create conversation
        Conversation conversation;
        String conversationId = request.getConversationId();
        if (conversationId != null) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            conversation.setUpdatedAt(LocalDateTime.now());
        } else {
            conversation = new Conversation();
            conversation.setUser(user);
            conversation.setPersona(persona);
            conversation.setTitle(generateTitle(request.getMessage()));
            conversation = conversationRepository.save(conversation);
        }
        
        // Save user message
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setAuthor("user");
        userMessage.setContent(request.getMessage());
        messageRepository.save(userMessage);
        
        // Retrieve context using hybrid RAG
        log.info("Retrieving context for persona: {}", persona.getName());
        String context = retrievalService.retrieveContext(request.getMessage(), request.getPersonaId());
        
        // Generate response using Gemini
        String systemPrompt = String.format("""
            You are %s, an AI assistant. %s
            
            IMPORTANT RULES:
            1. Answer ONLY based on the provided context below
            2. If the context doesn't contain relevant information, say "I don't have information about that in my knowledge base"
            3. Be conversational and helpful
            4. Stay in character as %s
            5. Do not make up information
            
            CONTEXT FROM TRAINED DOCUMENTS:
            %s
            
            USER QUESTION: %s
            
            Provide a helpful response based ONLY on the context above:
            """, 
            persona.getName(),
            persona.getDescription() != null ? persona.getDescription() : "",
            persona.getName(),
            context,
            request.getMessage()
        );
        
        String response;
        try {
            response = gemini.generate(systemPrompt);
        } catch (Exception e) {
            log.error("Error generating response", e);
            response = "I'm having trouble processing your request right now. Please try again.";
        }
        
        // Save persona response
        Message personaMessage = new Message();
        personaMessage.setConversation(conversation);
        personaMessage.setAuthor("persona");
        personaMessage.setContent(response);
        messageRepository.save(personaMessage);
        
        // Count sources
        int sourcesUsed = retrievalService.countSources(request.getPersonaId());
        
        return new ChatResponse(
                conversation.getId(),
                request.getMessage(),
                response,
                persona.getName(),
                sourcesUsed
        );
    }

    public List<ConversationResponse> getUserConversations(User user) {
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        
        return conversations.stream()
                .map(this::toConversationResponse)
                .collect(Collectors.toList());
    }

    public ConversationResponse getConversation(String conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return toConversationResponse(conversation);
    }

    @Transactional
    public void deleteConversation(String conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        conversationRepository.delete(conversation);
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        List<MessageResponse> messages = conversation.getMessages().stream()
                .map(m -> new MessageResponse(m.getId(), m.getAuthor(), m.getContent(), m.getCreatedAt()))
                .collect(Collectors.toList());
        
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getPersona().getId(),
                conversation.getPersona().getName(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages
        );
    }

    private String generateTitle(String firstMessage) {
        // Simple title generation from first message
        String title = firstMessage.length() > 50 
                ? firstMessage.substring(0, 47) + "..." 
                : firstMessage;
        return title;
    }
}
