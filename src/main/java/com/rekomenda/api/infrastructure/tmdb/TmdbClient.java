package com.rekomenda.api.infrastructure.tmdb;

import com.rekomenda.api.infrastructure.tmdb.dto.TmdbMovie;
import com.rekomenda.api.infrastructure.tmdb.dto.TmdbPageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TmdbClient {

    private static final String LANGUAGE = "pt-BR";
    private static final String PARAM_API_KEY = "api_key";
    private static final String PARAM_LANGUAGE = "language";

    private final RestClient restClient;
    private final String apiKey;

    public TmdbClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.tmdb.base-url}") String baseUrl,
            @Value("${app.tmdb.api-key}") String apiKey) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
    }

    private static final String[] DISCOVER_SORT_OPTIONS = {
            "popularity.desc", "vote_average.desc", "primary_release_date.desc", "vote_count.desc"
    };

    /**
     * Fetches movies filtered by genre IDs from TMDB /discover/movie.
     * Uses varied pages (spread across 1-100) and sort options for higher variety.
     */
    public List<TmdbMovie> discoverByGenres(List<Long> genreIds, int limit, int seed) {
        var genreParam = genreIds.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");

        var rng = new java.util.Random(seed);
        String sortBy = DISCOVER_SORT_OPTIONS[rng.nextInt(DISCOVER_SORT_OPTIONS.length)];

        int[] pageBands = {1, 15, 30, 50, 75, 100};
        int basePage = pageBands[rng.nextInt(pageBands.length)];
        var all = new ArrayList<TmdbMovie>();

        for (int i = 0; i < 4; i++) {
            int page = Math.min(basePage + i, 500);
            var response = restClient.get()
                    .uri(ub -> {
                        var b = ub.path("/discover/movie")
                                .queryParam(PARAM_API_KEY, apiKey)
                                .queryParam(PARAM_LANGUAGE, LANGUAGE)
                                .queryParam("sort_by", sortBy)
                                .queryParam("page", page);
                        if (!genreParam.isBlank()) {
                            b.queryParam("with_genres", genreParam);
                        }
                        return b.build();
                    })
                    .retrieve()
                    .body(TmdbPageResponse.class);

            if (response != null && response.results() != null && !response.results().isEmpty()) {
                all.addAll(response.results());
            } else {
                break;
            }
        }

        Collections.shuffle(all, new java.util.Random(seed));
        return all.stream().limit(limit).toList();
    }

    /**
     * Searches for movies by keyword. Fetches multiple pages and shuffles for variety.
     * @param seed used to vary which pages are fetched and shuffle order
     */
    public List<TmdbMovie> searchByKeywords(String query, int limit, int seed) {
        var rng = new java.util.Random(seed);
        int startPage = rng.nextInt(5) + 1;
        var all = new ArrayList<TmdbMovie>();

        for (int p = 0; p < 3; p++) {
            int page = Math.min(startPage + p, 500);
            var response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/movie")
                            .queryParam(PARAM_API_KEY, apiKey)
                            .queryParam(PARAM_LANGUAGE, LANGUAGE)
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .body(TmdbPageResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty())
                break;
            all.addAll(response.results());
        }

        Collections.shuffle(all, new java.util.Random(seed));
        return all.stream().limit(limit).toList();
    }

    /**
     * Fetches full movie details including genre list from TMDB /movie/{id}.
     * Returns a typed TmdbMovie suitable for caching.
     */
    public TmdbMovie fetchById(long movieId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam(PARAM_API_KEY, apiKey)
                        .queryParam(PARAM_LANGUAGE, LANGUAGE)
                        .build(movieId))
                .retrieve()
                .body(TmdbMovie.class);
    }

    /**
     * Fetches the list of official TMDB genres and returns a map from
     * lowercase genre name -> genre id. Uses English so Gemini's English keywords match.
     */
    public Map<String, Long> fetchGenreMapForMatching() {
        return fetchGenreMapWithLanguage("en");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> fetchGenreMapWithLanguage(String language) {
        var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/genre/movie/list")
                        .queryParam(PARAM_API_KEY, apiKey)
                        .queryParam(PARAM_LANGUAGE, language)
                        .build())
                .retrieve()
                .body(LinkedHashMap.class);

        if (response == null || !response.containsKey("genres"))
            return Map.of();

        var genres = (List<Map<String, Object>>) response.get("genres");
        if (genres == null)
            return Map.of();

        return genres.stream()
                .collect(Collectors.toMap(
                        g -> ((String) g.get("name")).toLowerCase(),
                        g -> ((Number) g.get("id")).longValue(),
                        (a, b) -> a));
    }
}
