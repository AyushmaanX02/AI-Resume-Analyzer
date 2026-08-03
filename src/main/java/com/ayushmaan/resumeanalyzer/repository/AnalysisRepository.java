package com.ayushmaan.resumeanalyzer.repository;

import com.ayushmaan.resumeanalyzer.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findByUserIdOrderByCreatedAtDesc(Long userId);
}
