package com.ayushmaan.resumeanalyzer.repository;

import com.ayushmaan.resumeanalyzer.model.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
    List<JobDescription> findByUserIdOrderByCreatedAtDesc(Long userId);
}
