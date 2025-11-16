package com.cognify.cognify_backend.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobClientBuilder;
import com.cognify.cognify_backend.config.AzureBlobProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor


public class AzureBlobService {

      private final AzureBlobProperties properties;


    public String uploadFile(MultipartFile file) throws IOException {
        String containerName = properties.getContainerName();
        String connectionString = properties.getConnectionString();
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is empty");

        String filename = System.currentTimeMillis() + "-" + file.getOriginalFilename();

        var blobClient = new BlobClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .blobName(filename)
                .buildClient();
        
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        return blobClient.getBlobUrl();
    }
}
