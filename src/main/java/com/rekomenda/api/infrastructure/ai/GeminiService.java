package com.rekomenda.api.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeminiService {

    private static final String KEYWORD_EXTRACTION_PROMPT = """
            You are a movie recommendation assistant. Extract genre keywords for TMDB API search.
            TMDB genres (use ONLY these exact names when applicable): Action, Adventure, Animation, Comedy, Crime, Documentary, Drama, Family, Fantasy, History, Horror, Music, Mystery, Romance, Science Fiction, Thriller, War, Western.
            For compound requests (e.g. "horror underground"), include the base genre (Horror) plus 1-2 specific search terms (e.g. underground, subterranean, cave).
            Participants have "already seen/rated" lists — prefer keywords that suggest NEW movies.
            Return ONLY a comma-separated list: first genre names from the list above, then optional specific terms. Nothing else.

            User data:
            %s
            """;

    private static final String INDIVIDUAL_RECOMMENDATION_PROMPT = """
            You are a movie recommendation assistant. The user described what they want to watch (may be in Portuguese or English):
            "%s"
            %s

            Rules:
            - Suggest ONE well-known movie or series that closely matches the description.
            - For horror + underground/cave: think The Descent, As Above So Below, The Cave, etc. — NOT children's content.
            - For adult-oriented requests, NEVER suggest kids/family films (e.g. Monster High, animated children's movies).
            - Return ONLY the exact movie title in English, nothing else.
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
