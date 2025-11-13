package com.cognify.cognify_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cognify.cognify_backend.model.Upload;

@Repository
public interface UploadRepository extends JpaRepository<Upload, String> {
    List<Upload> findByUploadedById(String userId);
    List<Upload> findByPersonaId(String personaId);
}
