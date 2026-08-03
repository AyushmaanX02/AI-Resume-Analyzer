package com.ayushmaan.resumeanalyzer.controller;

import com.ayushmaan.resumeanalyzer.dto.AnalysisRequest;
import com.ayushmaan.resumeanalyzer.dto.AnalysisResponse;
import com.ayushmaan.resumeanalyzer.model.Analysis;
import com.ayushmaan.resumeanalyzer.service.AnalysisService;
import com.ayushmaan.resumeanalyzer.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalysisRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        AnalysisResponse response = analysisService.runAnalysis(userId, request.getResumeId(), request.getJdId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponse> getById(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(analysisService.getById(id, userId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AnalysisResponse>> history() {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(analysisService.getHistoryResponses(userId));
    }
}
