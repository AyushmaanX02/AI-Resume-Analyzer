package com.ayushmaan.resumeanalyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchingServiceTest {

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService();
    }

    @Test
    void testComputeMatchHighFit() {
        String resumeText = "Senior Java developer building high scale Spring Boot microservices with MySQL and Docker.";
        String jdText = "Looking for a Backend Java Engineer experienced in Spring Boot, MySQL, and Docker.";

        List<String> resumeSkills = List.of("java", "spring boot", "mysql", "docker");
        List<String> jdRequiredSkills = List.of("java", "spring boot", "mysql", "docker");

        MatchingService.MatchResult result = matchingService.computeMatch(resumeText, jdText, resumeSkills, jdRequiredSkills);

        assertNotNull(result);
        assertEquals(100.0, result.skillsMatchScore);
        assertTrue(result.overallScore >= 70.0, "Overall score should be high for matching skills and text");
        assertEquals(4, result.matchedSkills.size());
        assertEquals(0, result.missingSkills.size());
    }

    @Test
    void testComputeMatchPartialFit() {
        String resumeText = "Frontend developer skilled in React, HTML, CSS, and JavaScript.";
        String jdText = "Looking for a Backend Java Engineer experienced in Java, Spring Boot, MySQL, and Docker.";

        List<String> resumeSkills = List.of("react", "html", "css", "javascript");
        List<String> jdRequiredSkills = List.of("java", "spring boot", "mysql", "docker");

        MatchingService.MatchResult result = matchingService.computeMatch(resumeText, jdText, resumeSkills, jdRequiredSkills);

        assertNotNull(result);
        assertEquals(0.0, result.skillsMatchScore);
        assertEquals(0, result.matchedSkills.size());
        assertEquals(4, result.missingSkills.size());
    }

    @Test
    void testTfIdfCosineSimilarity() {
        String text1 = "Java Spring Boot microservices backend API design";
        String text2 = "Java Spring Boot microservices backend API architecture";

        double sim = matchingService.computeTfIdfCosineSimilarity(text1, text2);
        assertTrue(sim > 50.0, "Cosine similarity for similar texts should be high");

        double simIdentical = matchingService.computeTfIdfCosineSimilarity(text1, text1);
        assertEquals(100.0, simIdentical, 0.5);
    }
}
