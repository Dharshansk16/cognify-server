package com.cognify.cognify_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    
    @NotBlank(message = "Persona ID is required")
    private String personaId;
    
    private String conversationId; // Optional - creates new if not provided
    
    @NotBlank(message = "Message is required")
    private String message;
}
