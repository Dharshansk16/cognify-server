package com.cognify.cognify_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private String conversationId;
    private String userMessage;
    private String personaResponse;
    private String personaName;
    private int sourcesUsed;
}
