package com.ayushmaan.resumeanalyzer.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    public static class MatchResult {
        public double overallScore;
        public double skillsMatchScore;
        public double experienceMatchScore;
        public double keywordScore;
        public List<String> matchedSkills;
        public List<String> missingSkills;

        public MatchResult(double overallScore, double skillsMatchScore, double experienceMatchScore,
                           double keywordScore, List<String> matchedSkills, List<String> missingSkills) {
            this.overallScore = overallScore;
            this.skillsMatchScore = skillsMatchScore;
            this.experienceMatchScore = experienceMatchScore;
            this.keywordScore = keywordScore;
            this.matchedSkills = matchedSkills;
            this.missingSkills = missingSkills;
        }
    }

    /**
     * Computes a hybrid match score based on:
     * 1. Hard Skill Overlap (50% weight)
     * 2. Full-text TF-IDF Cosine Similarity / Experience Fit (30% weight)
     * 3. Keyword Overlap (20% weight)
     */
    public MatchResult computeMatch(String resumeText, String jdText, List<String> resumeSkills, List<String> jdRequiredSkills) {
        Set<String> resumeSkillSet = new HashSet<>(lower(resumeSkills));
        Set<String> jdSkillSet = new LinkedHashSet<>(lower(jdRequiredSkills));

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        if (!jdSkillSet.isEmpty()) {
            for (String skill : jdSkillSet) {
                if (resumeSkillSet.contains(skill)) {
                    matchedSkills.add(skill);
                } else {
                    missingSkills.add(skill);
                }
            }
        }

        double skillsScore = jdSkillSet.isEmpty() ? 100.0 : ((double) matchedSkills.size() / jdSkillSet.size()) * 100.0;

        // 2. Compute TF-IDF Cosine Similarity for Experience / Domain Context fit
        double experienceScore = computeTfIdfCosineSimilarity(resumeText, jdText);

        // 3. Compute Keyword Overlap Score
        double keywordScore = computeKeywordOverlap(resumeText, jdText);

        // Weighted Overall Score: 50% Hard Skills, 30% Experience/TF-IDF, 20% Keywords
        double overallScore = (skillsScore * 0.50) + (experienceScore * 0.30) + (keywordScore * 0.20);

        return new MatchResult(
                round(overallScore),
                round(skillsScore),
                round(experienceScore),
                round(keywordScore),
                matchedSkills,
                missingSkills
        );
    }

    /**
     * Calculates TF-IDF Cosine Similarity between two raw text documents.
     */
    public double computeTfIdfCosineSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isBlank() || text2.isBlank()) {
            return 0.0;
        }

        List<String> doc1Tokens = tokenize(text1);
        List<String> doc2Tokens = tokenize(text2);

        if (doc1Tokens.isEmpty() || doc2Tokens.isEmpty()) {
            return 0.0;
        }

        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(doc1Tokens);
        vocabulary.addAll(doc2Tokens);

        Map<String, Integer> tf1 = getTermFrequency(doc1Tokens);
        Map<String, Integer> tf2 = getTermFrequency(doc2Tokens);

        // Calculate IDF for each term across the two documents
        Map<String, Double> idfMap = new HashMap<>();
        int totalDocs = 2;
        for (String term : vocabulary) {
            int docFreq = 0;
            if (tf1.containsKey(term)) docFreq++;
            if (tf2.containsKey(term)) docFreq++;
            // Smooth IDF
            double idf = Math.log((double) (totalDocs + 1) / (docFreq + 1)) + 1.0;
            idfMap.put(term, idf);
        }

        // Vectorize
        double[] vec1 = new double[vocabulary.size()];
        double[] vec2 = new double[vocabulary.size()];

        int i = 0;
        for (String term : vocabulary) {
            double idf = idfMap.get(term);
            vec1[i] = (tf1.getOrDefault(term, 0) / (double) doc1Tokens.size()) * idf;
            vec2[i] = (tf2.getOrDefault(term, 0) / (double) doc2Tokens.size()) * idf;
            i++;
        }

        // Cosine similarity
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int j = 0; j < vec1.length; j++) {
            dotProduct += vec1[j] * vec2[j];
            norm1 += vec1[j] * vec1[j];
            norm2 += vec2[j] * vec2[j];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
        return Math.min(100.0, Math.max(0.0, similarity * 100.0));
    }

    private double computeKeywordOverlap(String resumeText, String jdText) {
        Set<String> jdTokens = new HashSet<>(tokenize(jdText));
        if (jdTokens.isEmpty()) return 0.0;

        Set<String> resumeTokens = new HashSet<>(tokenize(resumeText));
        int matchCount = 0;
        for (String token : jdTokens) {
            if (resumeTokens.contains(token)) {
                matchCount++;
            }
        }

        return Math.min(100.0, ((double) matchCount / jdTokens.size()) * 100.0);
    }

    private List<String> tokenize(String text) {
        String lower = text.toLowerCase();
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("[a-z0-9#+.]++").matcher(lower);
        while (m.find()) {
            String token = m.group();
            while (token.startsWith(".")) token = token.substring(1);
            while (token.endsWith(".")) token = token.substring(0, token.length() - 1);
            if (token.length() > 1 && !isStopWord(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
                "and", "the", "for", "with", "that", "this", "from", "you", "are", "have", "has",
                "will", "been", "was", "were", "who", "which", "our", "their", "can", "able", "to",
                "in", "on", "at", "by", "an", "a", "or", "is", "of", "as", "be", "about"
        );
        return stopWords.contains(word);
    }

    private List<String> lower(List<String> input) {
        if (input == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String s : input) out.add(s.toLowerCase().trim());
        return out;
    }

    private Map<String, Integer> getTermFrequency(List<String> tokens) {
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) {
            tf.put(t, tf.getOrDefault(t, 0) + 1);
        }
        return tf;
    }

    private double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
