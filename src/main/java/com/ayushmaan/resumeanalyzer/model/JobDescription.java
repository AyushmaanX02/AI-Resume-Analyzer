package com.ayushmaan.resumeanalyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_descriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String rawText;

    // Stored as JSON string: {requiredSkills: [], preferredSkills: [], keywords: []}
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String parsedRequirementsJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
