package com.cognify.cognify_backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cognify.cognify_backend.model.Persona;
import com.cognify.cognify_backend.model.Upload;
import com.cognify.cognify_backend.model.User;
import com.cognify.cognify_backend.repository.PersonaRepository;
import com.cognify.cognify_backend.repository.UploadRepository;
import com.cognify.cognify_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final UploadRepository uploadRepository;
    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final AzureBlobService azureBlobService;
    private final HybridTrainingService trainingService;

    public Upload uploadFile(MultipartFile file, String userId, String personaId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("Persona not found"));

        String url = azureBlobService.uploadFile(file);

        Upload upload = new Upload();
        upload.setFilename(file.getOriginalFilename());
        upload.setUploadedBy(user);
        upload.setPersona(persona);
        upload.setUrl(url);
        upload.setCreatedAt(LocalDateTime.now());

        Upload saved = uploadRepository.save(upload);

        // Start training asynchronously
        trainingService.trainUploadAsync(saved);

        return saved;
    }

    public List<Upload> getAllUploads(String personaId) {
        if (personaId != null && !personaId.isEmpty()) {
            return uploadRepository.findByPersonaId(personaId);
        }
        return uploadRepository.findAll();
    }
}
