package com.rekomenda.api.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeminiService {

    private static final String KEYWORD_EXTRACTION_PROMPT = """
            You are a movie recommendation assistant. Based on the following user preferences and descriptions,
            extract a list of relevant movie genre keywords in English suitable for a TMDB API search.
            Participants have "already seen/rated" lists — prefer keywords that suggest NEW movies they haven't seen.
            Return ONLY a comma-separated list of keywords, nothing else.

            User data:
            %s
            """;

    private static final String INDIVIDUAL_RECOMMENDATION_PROMPT = """
            You are a helpful movie recommendation assistant. The user described what they want to watch:
            "%s"
            %s

            Suggest ONE movie or series that best matches and that the user has NOT already seen. Return ONLY the movie title in English, nothing else.
            """;

    private final ChatClient chatClient;

    public GeminiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Given a combined text of user histories and anonymous prompts, asks Gemini to
     * extract relevant genre keywords for a TMDB discovery search.
     */
    public List<String> extractKeywords(String combinedPrompt) {
        var response = chatClient.prompt()
                .user(KEYWORD_EXTRACTION_PROMPT.formatted(combinedPrompt))
                .call()
                .content();

        if (response == null || response.isBlank())
            return List.of();

        return List.of(response.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    /**
     * Handles an individual user's free-text movie request and returns a single suggested title.
     * If excludedTitles is non-empty, the LLM is instructed to avoid suggesting those movies.
     */
    public String recommendForIndividual(String userDescription, List<String> excludedTitles) {
        var excludeClause = excludedTitles != null && !excludedTitles.isEmpty()
                ? "The user has ALREADY SEEN these movies (do NOT suggest any of these): " + String.join(", ", excludedTitles)
                : "";
        var prompt = INDIVIDUAL_RECOMMENDATION_PROMPT.formatted(userDescription, excludeClause);

        var response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return response != null ? response.trim() : "";
    }
}
