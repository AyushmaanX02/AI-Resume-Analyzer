package com.ayushmaan.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps calls to the Anthropic Messages API to generate qualitative resume
 * suggestions and ATS-formatting flags. Falls back to a rule-based response
 * if the API key isn't configured or the call fails, so the app degrades
 * gracefully instead of breaking the analysis flow.
 */
@Service
public class LlmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.llm.api-key}")
    private String apiKey;

    @Value("${app.llm.model}")
    private String model;

    @Value("${app.llm.api-url}")
    private String apiUrl;

    public LlmService() {
        this.restClient = RestClient.create();
    }

    public static class LlmAnalysisResult {
        public List<String> suggestions;
        public List<String> atsFlags;

        public LlmAnalysisResult(List<String> suggestions, List<String> atsFlags) {
            this.suggestions = suggestions;
            this.atsFlags = atsFlags;
        }
    }

    public LlmAnalysisResult generateSuggestions(String resumeText, String jdText, List<String> missingSkills) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackResult(missingSkills);
        }

        try {
            String prompt = buildPrompt(resumeText, jdText, missingSkills);

            String requestBody = objectMapper.writeValueAsString(new AnthropicRequest(
                    model,
                    1000,
                    List.of(new Message("user", prompt))
            ));

            String response = restClient.post()
                    .uri(apiUrl)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response, missingSkills);

        } catch (Exception e) {
            // Degrade gracefully — don't let LLM failure break the whole analysis
            return fallbackResult(missingSkills);
        }
    }

    private String buildPrompt(String resumeText, String jdText, List<String> missingSkills) {
        return """
            You are a resume reviewer. Compare the RESUME against the JOB DESCRIPTION below.
            Missing skills already identified: %s

            Respond ONLY with valid JSON, no markdown fences, in this exact shape:
            {
              "suggestions": ["specific actionable suggestion 1", "suggestion 2", ...],
              "atsFlags": ["formatting issue 1", "issue 2", ...]
            }

            Rules:
            - suggestions: 3-5 specific, actionable ways to better reflect the JD requirements
              using the candidate's EXISTING experience (do not invent experience).
            - atsFlags: formatting/structural issues that could hurt ATS parsing
              (e.g. missing sections, unclear headers, no contact info). If none found, return [].

            RESUME:
            %s

            JOB DESCRIPTION:
            %s
            """.formatted(missingSkills, resumeText, jdText);
    }

    private LlmAnalysisResult parseResponse(String rawResponse, List<String> missingSkills) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String text = root.path("content").get(0).path("text").asText();
            text = text.trim();
            // Strip markdown fences if the model added them anyway
            if (text.startsWith("```")) {
                text = text.replaceAll("```json", "").replaceAll("```", "").trim();
            }

            JsonNode parsed = objectMapper.readTree(text);
            List<String> suggestions = new ArrayList<>();
            List<String> atsFlags = new ArrayList<>();

            parsed.path("suggestions").forEach(n -> suggestions.add(n.asText()));
            parsed.path("atsFlags").forEach(n -> atsFlags.add(n.asText()));

            return new LlmAnalysisResult(suggestions, atsFlags);
        } catch (Exception e) {
            return fallbackResult(missingSkills);
        }
    }

    private LlmAnalysisResult fallbackResult(List<String> missingSkills) {
        List<String> suggestions = new ArrayList<>();
        if (!missingSkills.isEmpty()) {
            suggestions.add("Consider highlighting experience related to: " + String.join(", ", missingSkills));
        }
        suggestions.add("Quantify achievements with metrics where possible (%, time saved, scale).");
        suggestions.add("Ensure each bullet starts with a strong action verb.");
        return new LlmAnalysisResult(suggestions, new ArrayList<>());
    }

    // --- Minimal request DTOs for Anthropic API ---
    private record AnthropicRequest(String model, int max_tokens, List<Message> messages) {}
    private record Message(String role, String content) {}
}
