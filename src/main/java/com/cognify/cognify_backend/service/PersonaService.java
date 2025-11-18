package com.cognify.cognify_backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cognify.cognify_backend.model.Persona;
import com.cognify.cognify_backend.model.User;
import com.cognify.cognify_backend.repository.PersonaRepository;
import com.cognify.cognify_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final UserRepository userRepository;

    public List<Persona> listUserPersonas(String userId) {
        return personaRepository.findByOwnerId(userId);
    }

    public Persona getPersona(String id) {
        if (id == null) return null;
        return personaRepository.findById(id).orElse(null);
    }

    public Persona createPersona(String userId, Persona persona) {
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
        User user = userRepository.findById(userId).orElseThrow();
        persona.setOwner(user);
        persona.setCreatedAt(LocalDateTime.now());
        persona.setUpdatedAt(LocalDateTime.now());
        return personaRepository.save(persona);
    }

    public Persona updatePersona(String id, Persona data) {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        Persona persona = personaRepository.findById(id).orElseThrow();

        if (data.getName() != null) persona.setName(data.getName());
        if (data.getDescription() != null) persona.setDescription(data.getDescription());
        if (data.getImageUrl() != null) persona.setImageUrl(data.getImageUrl());

        persona.setUpdatedAt(LocalDateTime.now());
        return personaRepository.save(persona);
    }

    public void deletePersona(String id) {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        personaRepository.deleteById(id);
    }
}
