package com.ayushmaan.resumeanalyzer.service;

import java.util.Set;

/**
 * Static dictionary of common technical/soft skills used for keyword extraction.
 * v1 approach: dictionary matching. Can be swapped for an NLP/embedding-based
 * extractor later without changing callers.
 */
public class SkillsDictionary {

    public static final Set<String> SKILLS = Set.of(
        // Languages
        "java", "python", "javascript", "typescript", "c++", "c#", "go", "golang", "rust",
        "kotlin", "swift", "php", "ruby", "scala", "sql", "html", "css",
        // Frameworks / libraries
        "spring boot", "spring security", "spring", "react", "angular", "vue", "node.js", "nodejs",
        "express", "django", "flask", "fastapi", "hibernate", "jpa", ".net", "next.js",
        // Databases
        "mysql", "postgresql", "postgres", "mongodb", "redis", "oracle", "sqlite", "cassandra",
        "dynamodb", "elasticsearch",
        // Cloud / DevOps
        "aws", "azure", "gcp", "docker", "kubernetes", "jenkins", "ci/cd", "terraform", "ansible",
        "git", "github", "gitlab", "railway", "heroku", "linux",
        // Concepts
        "rest api", "restful", "microservices", "jwt", "oauth", "rbac", "graphql", "grpc",
        "message queue", "kafka", "rabbitmq", "unit testing", "junit", "tdd", "agile", "scrum",
        "system design", "data structures", "algorithms", "oop", "multithreading",
        // Tools
        "postman", "maven", "gradle", "jira", "confluence", "vs code", "intellij",
        // Soft skills
        "communication", "teamwork", "leadership", "problem solving", "collaboration",
        "time management", "adaptability", "critical thinking"
    );

    private SkillsDictionary() {}
}
