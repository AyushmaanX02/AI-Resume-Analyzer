package com.ayushmaan.resumeanalyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String filename;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String rawText;

    // Stored as JSON string: {skills: [], experience: [], education: [], projects: [], certifications: []}
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String parsedSectionsJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
