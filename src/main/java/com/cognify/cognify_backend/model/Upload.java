package com.cognify.cognify_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "uploads")
@Data
@NoArgsConstructor
public class Upload {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
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

    @OneToMany(mappedBy = "upload", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainingChunk> chunks;

    @OneToMany(mappedBy = "upload", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Neo4jNode> neo4jNodes;
}
