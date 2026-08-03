package com.ayushmaan.resumeanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobDescriptionService {

    private final ResumeParsingService parsingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobDescriptionService(ResumeParsingService parsingService) {
        this.parsingService = parsingService;
    }

    /**
     * Extracts required skills and basic requirements from JD text.
     * Reuses the same skill-dictionary matcher as resumes so scoring is apples-to-apples.
     */
    public String parseRequirements(String jdText) {
        Map<String, Object> requirements = new LinkedHashMap<>();

        List<String> allSkills = parsingService.extractSkills(jdText);
        requirements.put("requiredSkills", allSkills);
        requirements.put("minYearsExperience", extractYearsOfExperience(jdText));

        try {
            return objectMapper.writeValueAsString(requirements);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Integer extractYearsOfExperience(String text) {
        Pattern p = Pattern.compile("(\\d+)\\+?\\s*(?:years|yrs)\\s*(?:of)?\\s*experience", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
