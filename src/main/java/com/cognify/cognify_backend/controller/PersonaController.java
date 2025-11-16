package com.cognify.cognify_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cognify.cognify_backend.model.Persona;
import com.cognify.cognify_backend.model.User;
import com.cognify.cognify_backend.service.PersonaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    @GetMapping
    public List<Persona> listUserPersonas(@AuthenticationPrincipal User user) {
        return personaService.listUserPersonas(user.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Persona> getPersona(@PathVariable String id) {
        var persona = personaService.getPersona(id);
        return persona == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(persona);
    }

    @PostMapping
    public Persona createPersona(@AuthenticationPrincipal User user, @RequestBody Persona persona) {
        return personaService.createPersona(user.getId(), persona);
    }

    @PatchMapping("/{id}")
    public Persona updatePersona(@PathVariable String id, @RequestBody Persona persona) {
        return personaService.updatePersona(id, persona);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePersona(@PathVariable String id) {
        personaService.deletePersona(id);
        return ResponseEntity.ok().build();
    }
}
