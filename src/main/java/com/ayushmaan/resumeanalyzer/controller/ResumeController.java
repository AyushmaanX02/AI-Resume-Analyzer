package com.ayushmaan.resumeanalyzer.controller;

import com.ayushmaan.resumeanalyzer.model.Resume;
import com.ayushmaan.resumeanalyzer.service.CurrentUserService;
import com.ayushmaan.resumeanalyzer.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final CurrentUserService currentUserService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Resume> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Long userId = currentUserService.getCurrentUserId();
        Resume resume = resumeService.uploadAndParse(userId, file);
        return ResponseEntity.ok(resume);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resume> getById(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(resumeService.getById(id, userId));
    }

    @GetMapping
    public ResponseEntity<List<Resume>> getAll() {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(resumeService.getAllForUser(userId));
    }
}
