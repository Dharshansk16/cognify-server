package com.cognify.cognify_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cognify.cognify_backend.model.Persona;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, String> {
}
