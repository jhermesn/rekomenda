package com.rekomenda.api.domain.movie;

import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import com.rekomenda.api.infrastructure.tmdb.TmdbClient;
import com.rekomenda.api.infrastructure.tmdb.dto.TmdbMovie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fetches movie details from TMDB, using Redis as a read-through cache.
 *
 * Key pattern : tmdb:movie:{tmdbId}
 * TTL : configurable via app.tmdb.movie-cache-ttl-days (default 7 days)
 *
 * Both getById (for API responses) and getTmdbMovieById (for internal genre
 * weight calculations) share the same cache entry to avoid duplicate TMDB
 * calls.
 */
@Service
public class MovieService {

    private static final String CACHE_PREFIX = "tmdb:movie:";

    private final TmdbClient tmdbClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration cacheTtl;

    public MovieService(
            TmdbClient tmdbClient,
            RedisTemplate<String, Object> redisTemplate,
            @Value("${app.tmdb.movie-cache-ttl-days:7}") long cacheTtlDays) {
        this.tmdbClient = tmdbClient;
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofDays(cacheTtlDays);
    }

    /**
     * Returns movie details for the given TMDB ID as an API response object.
     * Checks Redis first; on cache miss, fetches from TMDB and stores the result.
     */
    public MovieResponse getById(long tmdbId) {
        TmdbMovie movie = getTmdbMovieById(tmdbId);
        return movie != null ? MovieResponse.from(movie) : null;
    }

    /**
     * Returns the raw TmdbMovie (with genreIds) for the given TMDB ID.
     * Used internally by RatingService for genre weight updates.
     * Shares the same Redis cache entry as getById().
     */
    public TmdbMovie getTmdbMovieById(long tmdbId) {
        String key = CACHE_PREFIX + tmdbId;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof TmdbMovie movie) {
            return movie;
        }

        TmdbMovie movie = tmdbClient.fetchById(tmdbId);
        if (movie != null) {
            redisTemplate.opsForValue().set(key, movie, cacheTtl);
        }
        return movie;
    }
}
