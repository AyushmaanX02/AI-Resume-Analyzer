package com.ayushmaan.resumeanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeParsingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extracts raw text from an uploaded resume file (PDF or DOCX).
     */
    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Filename is missing");
        }

        String lower = filename.toLowerCase();
        try (InputStream is = file.getInputStream()) {
            if (lower.endsWith(".pdf")) {
                return extractFromPdf(is);
            } else if (lower.endsWith(".docx")) {
                return extractFromDocx(is);
            } else {
                throw new IllegalArgumentException("Unsupported file type. Use PDF or DOCX.");
            }
        }
    }

    private String extractFromPdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDocx(InputStream is) throws IOException {
        try (XWPFDocument document = new XWPFDocument(is);
             org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * Naive section detection based on common resume headers.
     * Returns a JSON string with detected sections and extracted skills.
     */
    public String parseSections(String rawText) {
        Map<String, Object> sections = new LinkedHashMap<>();

        sections.put("skills", extractSkills(rawText));
        sections.put("hasContactInfo", hasContactInfo(rawText));
        sections.put("hasEducationSection", containsSectionHeader(rawText, "education"));
        sections.put("hasExperienceSection",
                containsSectionHeader(rawText, "experience") || containsSectionHeader(rawText, "work history"));
        sections.put("hasProjectsSection", containsSectionHeader(rawText, "projects"));
        sections.put("hasSkillsSection", containsSectionHeader(rawText, "skills"));
        sections.put("wordCount", rawText.trim().split("\\s+").length);

        try {
            return objectMapper.writeValueAsString(sections);
        } catch (Exception e) {
            return "{}";
        }
    }

    public List<String> extractSkills(String text) {
        String lower = text.toLowerCase();
        List<String> found = new ArrayList<>();
        for (String skill : SkillsDictionary.SKILLS) {
            // word-boundary-ish match; skill may contain special chars like "c++", "c#", ".net"
            if (containsSkill(lower, skill)) {
                found.add(skill);
            }
        }
        return found;
    }

    private boolean containsSkill(String lowerText, String skill) {
        String escaped = Pattern.quote(skill);
        // For alnum-only skills use word boundaries; for symbol-containing skills, plain contains check.
        if (skill.matches("[a-z0-9 ]+")) {
            Pattern p = Pattern.compile("\\b" + escaped + "\\b");
            Matcher m = p.matcher(lowerText);
            return m.find();
        }
        return lowerText.contains(skill);
    }

    private boolean containsSectionHeader(String text, String header) {
        return text.toLowerCase().contains(header.toLowerCase());
    }

    private boolean hasContactInfo(String text) {
        boolean hasEmail = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+").matcher(text).find();
        boolean hasPhone = Pattern.compile("(\\+?\\d[\\d\\s-]{8,}\\d)").matcher(text).find();
        return hasEmail || hasPhone;
    }
}
