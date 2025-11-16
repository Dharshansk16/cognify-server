package com.cognify.cognify_backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cognify.cognify_backend.dto.UploadResponse;
import com.cognify.cognify_backend.model.Upload;
import com.cognify.cognify_backend.service.UploadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("personaId") String personaId
    ) throws Exception {
        Upload upload = uploadService.uploadFile(file, userId, personaId);
        return ResponseEntity.status(201)
                .body(new UploadResponse(
                        upload.getId(),
                        upload.getFilename(),
                        upload.getUrl(),
                        "started",
                        "File uploaded successfully and training started"
                ));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UploadResponse>> listUploads(@RequestParam(required = false) String personaId) {
        List<UploadResponse> uploads = uploadService.getAllUploads(personaId)
                .stream()
                .map(u -> new UploadResponse(u.getId(), u.getFilename(), u.getUrl(), "completed", ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(uploads);
    }
}
