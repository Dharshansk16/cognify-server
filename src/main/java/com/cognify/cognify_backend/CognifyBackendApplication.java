package com.cognify.cognify_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CognifyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CognifyBackendApplication.class, args);
	}

}
