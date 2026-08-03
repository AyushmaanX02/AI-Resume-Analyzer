package com.ayushmaan.resumeanalyzer.controller;

import com.ayushmaan.resumeanalyzer.dto.JobDescriptionRequest;
import com.ayushmaan.resumeanalyzer.model.JobDescription;
import com.ayushmaan.resumeanalyzer.service.CurrentUserService;
import com.ayushmaan.resumeanalyzer.service.JobDescriptionStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-descriptions")
@RequiredArgsConstructor
public class JobDescriptionController {

    private final JobDescriptionStorageService jdStorageService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<JobDescription> submit(@Valid @RequestBody JobDescriptionRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(jdStorageService.submitAndParse(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDescription> getById(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(jdStorageService.getById(id, userId));
    }

    @GetMapping
    public ResponseEntity<List<JobDescription>> getAll() {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(jdStorageService.getAllForUser(userId));
    }
}
