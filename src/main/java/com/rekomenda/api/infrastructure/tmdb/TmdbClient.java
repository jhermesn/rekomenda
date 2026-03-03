package com.rekomenda.api.infrastructure.tmdb;

import com.rekomenda.api.infrastructure.tmdb.dto.TmdbMovie;
import com.rekomenda.api.infrastructure.tmdb.dto.TmdbPageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

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
            @Value("${app.tmdb.api-key}") String apiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    /**
     * Fetches a page of movies filtered by genre IDs from TMDB /discover/movie.
     * Limits results to the requested count (TMDB always returns up to 20 per page).
     */
    public List<TmdbMovie> discoverByGenres(List<Long> genreIds, int limit) {
        var genreParam = genreIds.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");

        var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam(PARAM_API_KEY, apiKey)
                        .queryParam(PARAM_LANGUAGE, LANGUAGE)
                        .queryParam("with_genres", genreParam)
                        .queryParam("sort_by", "popularity.desc")
                        .build())
                .retrieve()
                .body(TmdbPageResponse.class);

        if (response == null || response.results() == null) return List.of();
        return response.results().stream().limit(limit).toList();
    }

    /**
     * Searches for movies by keyword using TMDB /search/movie.
     */
    public List<TmdbMovie> searchByKeywords(String query, int limit) {
        var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/movie")
                        .queryParam(PARAM_API_KEY, apiKey)
                        .queryParam(PARAM_LANGUAGE, LANGUAGE)
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .body(TmdbPageResponse.class);

        if (response == null || response.results() == null) return List.of();
        return response.results().stream().limit(limit).toList();
    }

    /**
     * Fetches full movie details including genre list from TMDB /movie/{id}.
     */
    public Map<String, Object> getMovieDetails(long movieId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam(PARAM_API_KEY, apiKey)
                        .queryParam(PARAM_LANGUAGE, LANGUAGE)
                        .build(movieId))
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }
}
