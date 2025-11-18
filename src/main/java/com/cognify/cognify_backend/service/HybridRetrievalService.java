package com.cognify.cognify_backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import com.azure.search.documents.SearchClient;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.util.SearchPagedIterable;
import com.cognify.cognify_backend.model.TrainingDocument;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRetrievalService {

    private final EmbeddingModel embeddingModel;
    private final SearchClient searchClient;
    private final Driver neo4jDriver;

    public String retrieveContext(String query, String personaId) {
        try {
            log.info("Retrieving context for query: {} and persona: {}", query, personaId);
            
            // 1. Vector search in Azure AI Search
            List<String> vectorResults = vectorSearch(query, personaId);
            
            // 2. Graph traversal in Neo4j
            List<String> graphResults = graphSearch(query, personaId);
            
            // 3. Combine and deduplicate results
            StringBuilder context = new StringBuilder();
            
            if (!vectorResults.isEmpty()) {
                context.append("📚 Relevant Information from Documents:\n");
                for (int i = 0; i < vectorResults.size(); i++) {
                    context.append(String.format("%d. %s\n\n", i + 1, vectorResults.get(i)));
                }
            }
            
            if (!graphResults.isEmpty()) {
                context.append("\n🔗 Related Knowledge from Knowledge Graph:\n");
                for (String fact : graphResults) {
                    context.append("• ").append(fact).append("\n");
                }
            }
            
            if (context.length() == 0) {
                return "No relevant information found in the trained documents.";
            }
            
            log.info("Retrieved {} vector results and {} graph facts", vectorResults.size(), graphResults.size());
            return context.toString();
            
        } catch (Exception e) {
            log.error("Error retrieving context", e);
            return "Error retrieving information from knowledge base.";
        }
    }

    private List<String> vectorSearch(String query, String personaId) {
        try {
            // Generate embedding for the query
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            List<Float> queryVector = queryEmbedding.vectorAsList();
            
            // Pad to 1536 dimensions
            while (queryVector.size() < 1536) {
                queryVector.add(0.0f);
            }
            
            // Search options with persona filter
            SearchOptions searchOptions = new SearchOptions()
                    .setFilter(String.format("personaId eq '%s'", personaId))
                    .setTop(5);
            
            // Perform search
            SearchPagedIterable results = searchClient.search(query, searchOptions, null);
            
            List<String> contexts = new ArrayList<>();
            results.forEach(result -> {
                TrainingDocument doc = result.getDocument(TrainingDocument.class);
                if (doc != null && doc.getContent() != null) {
                    contexts.add(doc.getContent());
                }
            });
            
            return contexts;
            
        } catch (Exception e) {
            log.error("Error in vector search", e);
            return new ArrayList<>();
        }
    }

    private List<String> graphSearch(String query, String personaId) {
        try (Session session = neo4jDriver.session()) {
            // Extract potential entities from query (simple keyword extraction)
            String[] keywords = query.toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", " ")
                    .split("\\s+");
            
            List<String> facts = new ArrayList<>();
            
            for (String keyword : keywords) {
                if (keyword.length() < 3) continue; // Skip short words
                
                Result result = session.run("""
                    MATCH (s:Entity)-[r:RELATION]->(o:Entity)
                    WHERE s.personaId = $persona 
                      AND (toLower(s.name) CONTAINS toLower($keyword) 
                           OR toLower(o.name) CONTAINS toLower($keyword)
                           OR toLower(r.type) CONTAINS toLower($keyword))
                    RETURN s.name as subject, r.type as predicate, o.name as object
                    LIMIT 3
                """, Map.of("persona", personaId, "keyword", keyword));
                
                result.stream().forEach(record -> {
                    String fact = String.format("%s %s %s",
                            record.get("subject").asString(),
                            record.get("predicate").asString(),
                            record.get("object").asString());
                    if (!facts.contains(fact)) {
                        facts.add(fact);
                    }
                });
            }
            
            return facts.stream().limit(10).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error in graph search", e);
            return new ArrayList<>();
        }
    }

    public int countSources(String personaId) {
        try {
            SearchOptions options = new SearchOptions()
                    .setFilter(String.format("personaId eq '%s'", personaId))
                    .setTop(0);
            
            SearchPagedIterable results = searchClient.search("*", options, null);
            Long count = results.getTotalCount();
            return count != null ? count.intValue() : 0;
            
        } catch (Exception e) {
            log.error("Error counting sources", e);
            return 0;
        }
    }
}
