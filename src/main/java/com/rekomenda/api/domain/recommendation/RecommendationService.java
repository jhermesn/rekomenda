package com.rekomenda.api.domain.recommendation;

import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import com.rekomenda.api.domain.user.UserRepository;
import com.rekomenda.api.infrastructure.tmdb.TmdbClient;
import com.rekomenda.api.shared.util.AgeCertification;
import com.rekomenda.api.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RecommendationService {

    private static final int TOP_GENRES = 3;
    private static final int DASHBOARD_LIMIT = 20;
    private static final String CACHE_PREFIX = "rec:dashboard:";

    private final UserRepository userRepository;
    private final TmdbClient tmdbClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration cacheTtl;

    public RecommendationService(
            UserRepository userRepository,
            TmdbClient tmdbClient,
            RedisTemplate<String, Object> redisTemplate,
            @Value("${app.recommendation.dashboard-cache-ttl-minutes:5}") long ttlMinutes) {
        this.userRepository = userRepository;
        this.tmdbClient = tmdbClient;
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofMinutes(ttlMinutes);
    }

    /**
     * Returns a personalised list of movies based on the user's top-weighted
     * genres.
     * Results are cached in Redis per user for cacheTtl (default 5 minutes).
     * Cache is invalidated by RatingService whenever the user rates a new movie.
     * Falls back to popular movies when the user has no genre preferences yet.
     */
    @Transactional(readOnly = true)
    public List<MovieResponse> getDashboard(String userId) {
        String key = CACHE_PREFIX + userId;

        // Cache hit
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(item -> {
                if (item instanceof MovieResponse movieResponse) {
                    return movieResponse;
                }
                if (item instanceof Map<?, ?> map) {
                    return new MovieResponse(
                            ((Number) map.get("id")).longValue(),
                            (String) map.get("title"),
                            (String) map.get("overview"),
                            (String) map.get("posterUrl"),
                            (String) map.get("releaseDate"),
                            ((Number) map.get("voteAverage")).doubleValue());
                }
                throw new IllegalStateException("Unexpected type in cache: " + item.getClass());
            }).toList();
        }

        // Cache miss — compute from TMDB
        List<MovieResponse> movies = fetchFromTmdb(userId);

        redisTemplate.opsForValue().set(key, movies, cacheTtl);
        return movies;
    }

    private List<MovieResponse> fetchFromTmdb(String userId) {
        var user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        var weights = user.getRecommendationWeights();
        int age = AgeCertification.ageFrom(user.getDataNascimento());
        String certCountry = AgeCertification.certificationCountry();
        String certLte = AgeCertification.certificationLteForAge(age);

        int seed = userId.hashCode() ^ (int) (System.currentTimeMillis() / 3600000);

        if (weights.isEmpty()) {
            return tmdbClient.discoverByGenres(List.of(), DASHBOARD_LIMIT, seed, certCountry, certLte)
                    .stream().map(MovieResponse::from).toList();
        }

        var topGenreIds = weights.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(TOP_GENRES)
                .map(e -> Long.parseLong(e.getKey()))
                .toList();

        return tmdbClient.discoverByGenres(topGenreIds, DASHBOARD_LIMIT, seed, certCountry, certLte)
                .stream().map(MovieResponse::from).toList();
    }
}
