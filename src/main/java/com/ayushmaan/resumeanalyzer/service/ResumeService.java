package com.ayushmaan.resumeanalyzer.service;

import com.ayushmaan.resumeanalyzer.model.Resume;
import com.ayushmaan.resumeanalyzer.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParsingService parsingService;

    public Resume uploadAndParse(Long userId, MultipartFile file) throws IOException {
        String rawText = parsingService.extractText(file);
        String sectionsJson = parsingService.parseSections(rawText);

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setFilename(file.getOriginalFilename());
        resume.setRawText(rawText);
        resume.setParsedSectionsJson(sectionsJson);

        return resumeRepository.save(resume);
    }

    public Resume getById(Long id, Long userId) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        if (!resume.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized to access this resume");
        }
        return resume;
    }

    public List<Resume> getAllForUser(Long userId) {
        return resumeRepository.findByUserIdOrderByUploadedAtDesc(userId);
    }
}
