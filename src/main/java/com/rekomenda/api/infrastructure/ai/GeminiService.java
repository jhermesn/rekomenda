package com.rekomenda.api.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class GeminiService {

    private static final String KEYWORD_EXTRACTION_PROMPT = """
            You are a movie recommendation assistant. Extract genre keywords for TMDB API search.
            TMDB genres (use ONLY these exact names when applicable): Action, Adventure, Animation, Comedy, Crime, Documentary, Drama, Family, Fantasy, History, Horror, Music, Mystery, Romance, Science Fiction, Thriller, War, Western.

            CRITICAL: Extract keywords ONLY from the CURRENT DESIRES below. IGNORE any movie titles or history — they are for exclusion only, NOT for inferring genre. If someone wants "comedy without drama", return Comedy — never Horror or Drama.

            Current desires:
            %s

            Return ONLY a comma-separated list: genre names from the list above, then optional specific terms. Nothing else.
            """;

    private static final String INDIVIDUAL_RECOMMENDATION_PROMPT = """
            You are a movie recommendation assistant.

            USER'S CURRENT REQUEST (match this exactly — it has highest priority):
            "%s"
            %s
            %s

            Rules:
            - Suggest ONE well-known movie that closely matches the user's request above. Do NOT infer genre from the exclusion list.
            - If they ask for comedy, suggest comedy. If they ask for horror, suggest horror. Never suggest a genre they did not ask for.
            - For adult-oriented requests, NEVER suggest kids/family films (e.g. Monster High).
            - Return ONLY the exact movie title in English, nothing else.
            """;

    private static final String AGE_RESTRICTION_CLAUSE = """
            The user is %d years old. Only suggest movies appropriate for their age (Brazil rating: Livre for 0-9, 10 for 10-11, 12 for 12-13, 14 for 14-15, 16 for 16-17, 18 for 18+).
            """;

    private static final String MOVIE_SELECTION_PROMPT = """
            You are a movie recommendation assistant. Below are movies from a database. The group wants:
            %s

            Movies (id | title | overview):
            %s

            Return ONLY a comma-separated list of movie IDs in order of best match (most relevant first). Use the exact IDs above. Nothing else.
            """;

    private static final int OVERVIEW_TRUNCATE = 120;
    private static final int MAX_CANDIDATES_FOR_LLM = 35;

    private final ChatClient chatClient;

    public GeminiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Given a combined text of user histories and anonymous prompts, asks Gemini to
     * extract relevant genre keywords for a TMDB discovery search.
     */
    public List<String> extractKeywords(String combinedPrompt) {
        var keywordsResponse = chatClient.prompt()
                .user(KEYWORD_EXTRACTION_PROMPT.formatted(combinedPrompt))
                .call()
                .content();

        if (keywordsResponse == null || keywordsResponse.isBlank())
            return List.of();

        return List.of(keywordsResponse.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    /**
     * Handles an individual user's free-text movie request and returns a single suggested title.
     * If excludedTitles is non-empty, the LLM is instructed to avoid suggesting those movies.
     * If userAge is present (non-null), the LLM is instructed to suggest only age-appropriate movies.
     */
    public String recommendForIndividual(String userDescription, List<String> excludedTitles, Integer userAge) {
        var excludeClause = excludedTitles != null && !excludedTitles.isEmpty()
                ? "Do NOT suggest these (already seen; use only for exclusion, NOT for genre): " + String.join(", ", excludedTitles)
                : "";
        var ageClause = userAge != null ? AGE_RESTRICTION_CLAUSE.formatted(userAge) : "";
        var userPrompt = INDIVIDUAL_RECOMMENDATION_PROMPT.formatted(userDescription, excludeClause, ageClause);

        String suggestedTitle = chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();

        return suggestedTitle != null ? suggestedTitle.trim() : "";
    }

    /**
     * Given TMDB candidates and group desires, asks the LLM to rank them by relevance.
     * Returns the same movies reordered by LLM preference.
     */
    public List<Long> selectMovieIdsByRelevance(String desires, List<MovieCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        var limited = candidates.stream().limit(MAX_CANDIDATES_FOR_LLM).toList();
        var movieLines = limited.stream()
                .map(c -> "%d | %s | %s".formatted(
                        c.id(),
                        c.title() != null ? c.title() : "",
                        truncate(c.overview(), OVERVIEW_TRUNCATE)))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        var prompt = MOVIE_SELECTION_PROMPT.formatted(desires, movieLines);
        String response = chatClient.prompt().user(prompt).call().content();
        if (response == null || response.isBlank()) return limited.stream().map(MovieCandidate::id).toList();

        var idOrder = parseIdOrder(response);
        var byId = new LinkedHashMap<Long, MovieCandidate>();
        limited.forEach(c -> byId.put(c.id(), c));

        var result = new ArrayList<Long>();
        for (long id : idOrder) {
            if (byId.containsKey(id)) {
                result.add(id);
                byId.remove(id);
            }
        }
        byId.keySet().forEach(result::add);
        return result;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static List<Long> parseIdOrder(String response) {
        return List.of(response.split("[,\\s]+")).stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException _) {
                        return null;
                    }
                })
                .filter(id -> id != null && id > 0)
                .toList();
    }

    public record MovieCandidate(long id, String title, String overview) {}
}
