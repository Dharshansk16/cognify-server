package com.cognify.cognify_backend.model;

import java.util.List;

import com.azure.search.documents.indexes.SearchableField;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDocument {
    
    @JsonProperty("id")
    @SearchableField(isKey = true)
    private String id;
    
    @JsonProperty("content")
    @SearchableField
    private String content;
    
    @JsonProperty("vector")
    private List<Float> vector;
    
    @JsonProperty("personaId")
    @SearchableField(isFilterable = true)
    private String personaId;
    
    @JsonProperty("pdfUrl")
    private String pdfUrl;
    
    @JsonProperty("uploadId")
    @SearchableField(isFilterable = true)
    private String uploadId;
    
    @JsonProperty("pageNumber")
    private int pageNumber;
    
    @JsonProperty("chunkIndex")
    private int chunkIndex;
}
