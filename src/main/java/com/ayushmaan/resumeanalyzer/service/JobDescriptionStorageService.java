package com.ayushmaan.resumeanalyzer.service;

import com.ayushmaan.resumeanalyzer.dto.JobDescriptionRequest;
import com.ayushmaan.resumeanalyzer.model.JobDescription;
import com.ayushmaan.resumeanalyzer.repository.JobDescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobDescriptionStorageService {

    private final JobDescriptionRepository jdRepository;
    private final JobDescriptionService jdParsingService;

    public JobDescription submitAndParse(Long userId, JobDescriptionRequest request) {
        String requirementsJson = jdParsingService.parseRequirements(request.getRawText());

        JobDescription jd = new JobDescription();
        jd.setUserId(userId);
        jd.setTitle(request.getTitle());
        jd.setRawText(request.getRawText());
        jd.setParsedRequirementsJson(requirementsJson);

        return jdRepository.save(jd);
    }

    public JobDescription getById(Long id, Long userId) {
        JobDescription jd = jdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job description not found"));
        if (!jd.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized to access this job description");
        }
        return jd;
    }

    public List<JobDescription> getAllForUser(Long userId) {
        return jdRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
