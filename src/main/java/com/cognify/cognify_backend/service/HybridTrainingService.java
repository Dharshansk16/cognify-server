package com.cognify.cognify_backend.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.azure.search.documents.SearchClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.cognify.cognify_backend.config.AzureBlobProperties;
import com.cognify.cognify_backend.model.TrainingDocument;
import com.cognify.cognify_backend.model.Triplet;
import com.cognify.cognify_backend.model.Upload;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridTrainingService {

    private final EmbeddingModel embeddingModel;
    private final TripletExtractionService tripletExtractor;
    private final SearchClient searchClient;
    private final Driver neo4jDriver;
    private final AzureBlobProperties azureBlobProperties;

    @Async
    public void trainUploadAsync(Upload upload) {
        long startTime = System.currentTimeMillis();
        String uploadId = upload.getId();
        String personaId = upload.getPersona().getId();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 HYBRID RAG TRAINING STARTED");
        System.out.println("   Upload ID: " + uploadId);
        System.out.println("   Persona ID: " + personaId);
        System.out.println("   PDF URL: " + upload.getUrl());
        System.out.println("=".repeat(80) + "\n");

        try {
            // Step 0: Download PDF
            System.out.println("📥 [STEP 1/5] Downloading PDF...");
            long stepStart = System.currentTimeMillis();
            String pdfPath = download(upload.getUrl());
            System.out.println("   ✓ PDF ready: " + pdfPath);
            System.out.println("   ⏱ Time: " + (System.currentTimeMillis() - stepStart) + "ms\n");

            String pdfUrl = upload.getUrl();

            // Step 1: Load and chunk the PDF
            System.out.println("📄 [STEP 2/5] Loading and chunking PDF...");
            stepStart = System.currentTimeMillis();
            List<TrainingDocument> chunks = loadAndChunk(pdfPath, uploadId, personaId, pdfUrl);
            System.out.println("   ✓ Created " + chunks.size() + " chunks");
            System.out.println("   ⏱ Time: " + (System.currentTimeMillis() - stepStart) + "ms\n");

            // Step 2: Generate Embeddings
            System.out.println("🧠 [STEP 3/5] Generating embeddings with Gemini...");
            stepStart = System.currentTimeMillis();
            generateEmbeddings(chunks);
            System.out.println("   ✓ Generated embeddings for " + chunks.size() + " chunks");
            System.out.println("   ⏱ Time: " + (System.currentTimeMillis() - stepStart) + "ms\n");

            // Step 3: Extract Triplets (Gemini)
            System.out.println("🔗 [STEP 4/5] Extracting knowledge graph triplets...");
            stepStart = System.currentTimeMillis();
            List<String> texts = chunks.stream().map(TrainingDocument::getContent).toList();
            List<Triplet> triplets = tripletExtractor.extractTriplets(texts, personaId);
            System.out.println("   ✓ Extracted " + triplets.size() + " entity-relationship triplets");
            if (!triplets.isEmpty()) {
                System.out.println("   📊 Sample triplet: " + triplets.get(0).getSubject() + 
                                 " → " + triplets.get(0).getPredicate() + 
                                 " → " + triplets.get(0).getObject());
            }
            System.out.println("   ⏱ Time: " + (System.currentTimeMillis() - stepStart) + "ms\n");

            // Step 4: Upload chunks → Azure Search
            System.out.println("☁️  [STEP 5/5] Uploading to Azure AI Search...");
            stepStart = System.currentTimeMillis();
            uploadToAzureSearch(chunks);
            System.out.println("   ✓ Uploaded " + chunks.size() + " chunks to vector database");
            System.out.println("   ⏱ Time: " + (System.currentTimeMillis() - stepStart) + "ms\n");

            // Step 5: Upload triplets → Neo4j
            System.out.println("🌐 [STEP 6/6] Uploading to Neo4j knowledge graph...");
            stepStart = System.currentTimeMillis();
            uploadTripletsToNeo4j(triplets, personaId);
            System.out.println("   ✓ Uploaded " + triplets.size() + " triplets to graph database");
            System.out.println("   ⏱ Time: " + (System.currentTimeMillis() - stepStart) + "ms\n");

            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("=".repeat(80));
            System.out.println("✅ TRAINING COMPLETED SUCCESSFULLY");
            System.out.println("   📊 Statistics:");
            System.out.println("      - Total chunks: " + chunks.size());
            System.out.println("      - Total triplets: " + triplets.size());
            System.out.println("      - Vector embeddings: " + chunks.size());
            System.out.println("      - Knowledge graph nodes: ~" + (triplets.size() * 2));
            System.out.println("   ⏱ Total time: " + totalTime + "ms (" + (totalTime/1000.0) + "s)");
            System.out.println("=".repeat(80) + "\n");

        } catch (IOException e) {
            System.err.println("\n❌ TRAINING FAILED - I/O Error");
            System.err.println("   Error: " + e.getMessage());
            log.error("Training failed due to I/O error: {}", e.getMessage(), e);
        } catch (RuntimeException e) {
            System.err.println("\n❌ TRAINING FAILED - Runtime Error");
            System.err.println("   Error: " + e.getMessage());
            log.error("Training failed due to runtime error: {}", e.getMessage(), e);
        }
    }

    private String download(String url) throws IOException {
        System.out.println("   🔗 Downloading from Azure Blob Storage...");
        
        // Extract blob name from URL
        // URL format: https://<account>.blob.core.windows.net/<container>/<blobname>
        String blobName = url.substring(url.lastIndexOf('/') + 1);
        
        BlobClient blobClient = new BlobClientBuilder()
                .connectionString(azureBlobProperties.getConnectionString())
                .containerName(azureBlobProperties.getContainerName())
                .blobName(blobName)
                .buildClient();
        
        // Create temporary file
        File tempFile = Files.createTempFile("cognify-pdf-", ".pdf").toFile();
        tempFile.deleteOnExit();
        
        // Download blob to temp file
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            blobClient.downloadStream(fos);
        }
        
        System.out.println("   ✓ Downloaded to temporary file: " + tempFile.getAbsolutePath());
        System.out.println("   📊 File size: " + (tempFile.length() / 1024) + " KB");
        
        return tempFile.getAbsolutePath();
    }

    private List<TrainingDocument> loadAndChunk(String pdfPath, String uploadId, String personaId, String pdfUrl) throws IOException {
        try (PDDocument pdf = PDDocument.load(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();

            List<TrainingDocument> docs = new ArrayList<>();

            int pageCount = pdf.getNumberOfPages();
            System.out.println("   📖 Processing " + pageCount + " pages...");

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(pdf);

                // LangChain4j chunking
                DocumentSplitter splitter = DocumentSplitters.recursive(1000, 100);
                List<TextSegment> segments = splitter.split(dev.langchain4j.data.document.Document.from(text));

                int i = 0;
                for (TextSegment s : segments) {
                    docs.add(new TrainingDocument(
                            uploadId + "-" + page + "-" + i,
                            s.text(),
                            new ArrayList<>(),
                            personaId,
                            pdfUrl,
                            uploadId,
                            page,
                            i
                    ));
                    i++;
                }
                System.out.println("      Page " + page + "/" + pageCount + ": " + segments.size() + " chunks");
            }

            return docs;
        }
    }

    private void generateEmbeddings(List<TrainingDocument> docs) {
        int total = docs.size();
        int processed = 0;
        int lastPercent = -1;
        
        System.out.println("   ℹ️  Note: Padding 768-dimensional Gemini embeddings to 1536 dimensions for Azure Search");
        
        for (TrainingDocument d : docs) {
            Embedding e = embeddingModel.embed(d.getContent()).content();
            List<Float> embedding = e.vectorAsList();
            
            // Pad from 768 to 1536 dimensions with zeros
            List<Float> paddedEmbedding = new ArrayList<>(embedding);
            while (paddedEmbedding.size() < 1536) {
                paddedEmbedding.add(0.0f);
            }
            
            d.setVector(paddedEmbedding);
            
            processed++;
            int percent = (processed * 100) / total;
            if (percent != lastPercent && percent % 10 == 0) {
                System.out.println("      Progress: " + percent + "% (" + processed + "/" + total + ")");
                lastPercent = percent;
            }
        }
    }

    private void uploadToAzureSearch(List<TrainingDocument> docs) {
        searchClient.uploadDocuments(docs);
    }

    private void uploadTripletsToNeo4j(List<Triplet> triplets, String personaId) {
        if (triplets.isEmpty()) {
            System.out.println("   ℹ️  No triplets to upload to Neo4j");
            return;
        }
        
        try (Session session = neo4jDriver.session()) {
            // Convert Triplet objects to Maps that Neo4j can understand
            List<Map<String, String>> tripletMaps = triplets.stream()
                    .map(t -> Map.of(
                            "subject", t.getSubject(),
                            "predicate", t.getPredicate(),
                            "object", t.getObject()
                    ))
                    .toList();
            
            session.run("""
                UNWIND $triplets AS t
                MERGE (s:Entity {name: t.subject, personaId: $persona})
                MERGE (o:Entity {name: t.object, personaId: $persona})
                MERGE (s)-[:RELATION {type: t.predicate, personaId: $persona}]->(o)
            """, Map.of("triplets", tripletMaps, "persona", personaId));
        }
    }
}
