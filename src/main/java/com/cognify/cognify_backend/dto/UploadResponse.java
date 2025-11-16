package com.cognify.cognify_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResponse {
    private String id;
    private String filename;
    private String url;
    private String trainingStatus;
    private String message;
}
