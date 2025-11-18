package com.cognify.cognify_backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cognify.cognify_backend.model.Triplet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripletExtractionService {

    private final ChatLanguageModel gemini;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Triplet> extractTriplets(List<String> texts, String personaId) {
        try {
            System.out.println("   🤖 Calling Gemini AI to extract triplets from " + texts.size() + " text chunks...");
            System.out.println("   ⚙️  Configured with 5 retries and exponential backoff");
            String joined = String.join("\n---\n", texts);
            
            String prompt = String.format("""
                Extract entity-relationship-entity triplets from the following text.
                Return your response as a JSON array of objects, each with "subject", "predicate", and "object" fields.
                Only extract meaningful relationships, not trivial ones.
                
                Example format:
                [
                  {"subject": "John", "predicate": "works at", "object": "Microsoft"},
                  {"subject": "Microsoft", "predicate": "located in", "object": "Seattle"}
                ]
                
                Text:
                %s
                
                Return ONLY the JSON array, no additional text.
                """, joined);

            System.out.println("   ⏳ Waiting for Gemini response...");
            String response = gemini.generate(prompt);
            System.out.println("   ✓ Received response from Gemini");
            log.debug("Gemini response: {}", response);
            
            // Clean the response (remove markdown code blocks if present)
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();
            
            // Parse JSON response
            List<Map<String, String>> tripletMaps = objectMapper.readValue(
                cleanedResponse, 
                new TypeReference<List<Map<String, String>>>() {}
            );
            
            // Convert to Triplet objects
            List<Triplet> triplets = new ArrayList<>();
            for (Map<String, String> map : tripletMaps) {
                Triplet triplet = new Triplet();
                triplet.setSubject(map.get("subject"));
                triplet.setPredicate(map.get("predicate"));
                triplet.setObject(map.get("object"));
                triplet.setPersonaId(personaId);
                triplets.add(triplet);
            }
            
            System.out.println("   ✓ Successfully parsed " + triplets.size() + " triplets");
            log.info("Extracted {} triplets for persona {}", triplets.size(), personaId);
            return triplets;
             
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("   ❌ Error parsing JSON response from Gemini: " + e.getMessage());
            log.error("Error parsing JSON response from Gemini", e);
            return new ArrayList<>();
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("503")) {
                System.err.println("   ⚠️  Gemini API is overloaded (503) - All retries exhausted");
                System.err.println("   💡 Training will continue without triplets for now");
            } else if (errorMsg != null && errorMsg.contains("429")) {
                System.err.println("   ⚠️  Gemini API rate limit exceeded (429)");
                System.err.println("   💡 Consider upgrading your API quota");
            } else {
                System.err.println("   ❌ Error extracting triplets: " + errorMsg);
            }
            log.error("Error extracting triplets", e);
            return new ArrayList<>();
        }
    }
}