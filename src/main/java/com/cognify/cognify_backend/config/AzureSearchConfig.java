package com.cognify.cognify_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;

@Configuration
public class AzureSearchConfig {

    @Value("${azure.search.endpoint}")
    private String endpoint;

    @Value("${azure.search.api-key}")
    private String apiKey;

    @Value("${azure.search.index-name}")
    private String indexName;

    @Bean
    public SearchClient searchClient() {
        return new SearchClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .indexName(indexName)
                .buildClient();
    }
}
