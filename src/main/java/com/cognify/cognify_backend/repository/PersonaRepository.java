package com.cognify.cognify_backend.repository;

import com.cognify.cognify_backend.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, String> {
    Optional<Persona> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
