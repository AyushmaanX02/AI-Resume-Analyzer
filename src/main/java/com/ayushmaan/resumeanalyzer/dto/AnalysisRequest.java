package com.ayushmaan.resumeanalyzer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnalysisRequest {
    @NotNull
    private Long resumeId;

    @NotNull
    private Long jdId;
}
