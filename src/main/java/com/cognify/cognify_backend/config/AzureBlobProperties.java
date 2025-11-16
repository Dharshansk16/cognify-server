package com.cognify.cognify_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "azure.storage.blob")
@Data
public class AzureBlobProperties {
    private String containerName;
    private String connectionString;
}
