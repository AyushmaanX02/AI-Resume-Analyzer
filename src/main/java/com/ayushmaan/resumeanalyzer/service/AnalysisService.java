package com.ayushmaan.resumeanalyzer.service;

import com.ayushmaan.resumeanalyzer.dto.AnalysisResponse;
import com.ayushmaan.resumeanalyzer.model.Analysis;
import com.ayushmaan.resumeanalyzer.model.JobDescription;
import com.ayushmaan.resumeanalyzer.model.Resume;
import com.ayushmaan.resumeanalyzer.repository.AnalysisRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final ResumeService resumeService;
    private final JobDescriptionStorageService jdStorageService;
    private final MatchingService matchingService;
    private final LlmService llmService;
    private final AnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisResponse runAnalysis(Long userId, Long resumeId, Long jdId) {
        Resume resume = resumeService.getById(resumeId, userId);
        JobDescription jd = jdStorageService.getById(jdId, userId);

        List<String> resumeSkills = extractListField(resume.getParsedSectionsJson(), "skills");
        List<String> jdSkills = extractListField(jd.getParsedRequirementsJson(), "requiredSkills");

        MatchingService.MatchResult matchResult = matchingService.computeMatch(
                resume.getRawText(), jd.getRawText(), resumeSkills, jdSkills);

        LlmService.LlmAnalysisResult llmResult = llmService.generateSuggestions(
                resume.getRawText(), jd.getRawText(), matchResult.missingSkills);

        Analysis analysis = new Analysis();
        analysis.setUserId(userId);
        analysis.setResumeId(resumeId);
        analysis.setJdId(jdId);
        analysis.setMatchScore(matchResult.overallScore);
        analysis.setSkillsMatchScore(matchResult.skillsMatchScore);
        analysis.setExperienceMatchScore(matchResult.experienceMatchScore);
        analysis.setKeywordScore(matchResult.keywordScore);
        analysis.setMatchedSkillsJson(toJson(matchResult.matchedSkills));
        analysis.setMissingSkillsJson(toJson(matchResult.missingSkills));
        analysis.setSuggestionsJson(toJson(llmResult.suggestions));
        analysis.setAtsFlagsJson(toJson(llmResult.atsFlags));

        analysis = analysisRepository.save(analysis);

        return toResponse(analysis, resume.getFilename(), jd.getTitle());
    }

    public AnalysisResponse getById(Long id, Long userId) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found with id: " + id));

        if (!analysis.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Access denied");
        }

        Resume resume = resumeService.getById(analysis.getResumeId(), userId);
        JobDescription jd = jdStorageService.getById(analysis.getJdId(), userId);

        return toResponse(analysis, resume.getFilename(), jd.getTitle());
    }

    public List<AnalysisResponse> getHistoryResponses(Long userId) {
        List<Analysis> list = analysisRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<AnalysisResponse> responses = new ArrayList<>();
        for (Analysis a : list) {
            String resumeName = "Resume #" + a.getResumeId();
            String jdTitle = "Job Description #" + a.getJdId();
            try {
                Resume r = resumeService.getById(a.getResumeId(), userId);
                if (r != null) resumeName = r.getFilename();
            } catch (Exception ignored) {}
            try {
                JobDescription jd = jdStorageService.getById(a.getJdId(), userId);
                if (jd != null && jd.getTitle() != null) jdTitle = jd.getTitle();
            } catch (Exception ignored) {}

            responses.add(toResponse(a, resumeName, jdTitle));
        }
        return responses;
    }

    private AnalysisResponse toResponse(Analysis a, String resumeFilename, String jdTitle) {
        List<String> matched = parseJsonList(a.getMatchedSkillsJson());
        List<String> missing = parseJsonList(a.getMissingSkillsJson());
        List<String> suggestions = parseJsonList(a.getSuggestionsJson());
        List<String> atsFlags = parseJsonList(a.getAtsFlagsJson());

        return new AnalysisResponse(
                a.getId(),
                a.getResumeId(),
                a.getJdId(),
                resumeFilename,
                jdTitle,
                a.getMatchScore(),
                a.getSkillsMatchScore() != null ? a.getSkillsMatchScore() : a.getMatchScore(),
                a.getExperienceMatchScore() != null ? a.getExperienceMatchScore() : a.getMatchScore(),
                a.getKeywordScore() != null ? a.getKeywordScore() : a.getMatchScore(),
                matched,
                missing,
                suggestions,
                atsFlags,
                a.getCreatedAt()
        );
    }

    private List<String> parseJsonList(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                node.forEach(item -> result.add(item.asText()));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private List<String> extractListField(String json, String field) {
        List<String> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode arr = root.path(field);
            if (arr.isArray()) {
                arr.forEach(n -> result.add(n.asText()));
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
