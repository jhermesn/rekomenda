package com.rekomenda.api.domain.recommendation;

import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import com.rekomenda.api.domain.user.UserRepository;
import com.rekomenda.api.infrastructure.tmdb.TmdbClient;
import com.rekomenda.api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private static final int TOP_GENRES = 3;
    private static final int DASHBOARD_LIMIT = 20;

    private final UserRepository userRepository;
    private final TmdbClient tmdbClient;

    public RecommendationService(UserRepository userRepository, TmdbClient tmdbClient) {
        this.userRepository = userRepository;
        this.tmdbClient = tmdbClient;
    }

    /**
     * Returns a personalised list of movies based on the user's top-weighted genres.
     * Falls back to popular movies when the user has no genre preferences yet.
     */
    @Transactional(readOnly = true)
    public List<MovieResponse> getDashboard(String userId) {
        var user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        var weights = user.getRecommendationWeights();

        if (weights.isEmpty()) {
            return tmdbClient.discoverByGenres(List.of(), DASHBOARD_LIMIT)
                    .stream().map(MovieResponse::from).toList();
        }

        var topGenreIds = weights.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TOP_GENRES)
                .map(e -> Long.parseLong(e.getKey()))
                .toList();

        return tmdbClient.discoverByGenres(topGenreIds, DASHBOARD_LIMIT)
                .stream().map(MovieResponse::from).toList();
    }
}
