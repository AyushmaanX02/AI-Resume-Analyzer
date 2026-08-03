package com.ayushmaan.resumeanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private Long id;
    private Long resumeId;
    private Long jdId;
    private String resumeFilename;
    private String jdTitle;
    private Double matchScore;
    private Double skillsMatchScore;
    private Double experienceMatchScore;
    private Double keywordScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> suggestions;
    private List<String> atsFlags;
    private LocalDateTime createdAt;
}
