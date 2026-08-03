package com.ayushmaan.resumeanalyzer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobDescriptionRequest {
    private String title;

    @NotBlank
    private String rawText;
}
