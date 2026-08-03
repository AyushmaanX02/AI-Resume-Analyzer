package com.ayushmaan.resumeanalyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumeParsingServiceTest {

    private ResumeParsingService parsingService;

    @BeforeEach
    void setUp() {
        parsingService = new ResumeParsingService();
    }

    @Test
    void testExtractSkillsFromText() {
        String sampleText = "Experienced Java software engineer skilled in Spring Boot, MySQL, Docker, React, and Python.";
        List<String> skills = parsingService.extractSkills(sampleText);

        assertNotNull(skills);
        assertTrue(skills.contains("java"));
        assertTrue(skills.contains("spring boot"));
        assertTrue(skills.contains("mysql"));
        assertTrue(skills.contains("docker"));
        assertTrue(skills.contains("react"));
        assertTrue(skills.contains("python"));
    }

    @Test
    void testParseSections() {
        String rawText = "Ayushmaan Kapoor\nEmail: test@example.com Phone: 123-456-7890\n" +
                "EXPERIENCE\nSoftware Engineer at Tech Corp\n" +
                "EDUCATION\nB.S. in Computer Science\n" +
                "SKILLS\nJava, Spring Boot, Docker";

        String json = parsingService.parseSections(rawText);

        assertNotNull(json);
        assertTrue(json.contains("hasContactInfo\":true"));
        assertTrue(json.contains("hasEducationSection\":true"));
        assertTrue(json.contains("hasExperienceSection\":true"));
        assertTrue(json.contains("hasSkillsSection\":true"));
    }

    @Test
    void testExtractTextUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> parsingService.extractText(file));
    }
}
