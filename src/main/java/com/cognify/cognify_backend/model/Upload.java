package com.cognify.cognify_backend.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "uploads")
@Data
@NoArgsConstructor
public class Upload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String filename;

    // Optional: url in S3/blob
    private String url;

    // who uploaded this file
    @ManyToOne(optional = false)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    // optional persona association
    @ManyToOne
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

}
