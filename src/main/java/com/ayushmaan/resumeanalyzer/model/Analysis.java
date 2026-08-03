package com.ayushmaan.resumeanalyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "analyses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long resumeId;

    @Column(nullable = false)
    private Long jdId;

    @Column(nullable = false)
    private Double matchScore;

    private Double skillsMatchScore;
    private Double experienceMatchScore;
    private Double keywordScore;

    // JSON arrays
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String matchedSkillsJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String missingSkillsJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String suggestionsJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String atsFlagsJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
