package com.rekomenda.api.domain.rating;

import com.rekomenda.api.domain.movie.MovieService;
import com.rekomenda.api.domain.rating.dto.CreateRatingRequest;
import com.rekomenda.api.domain.rating.dto.RatingResponse;
import com.rekomenda.api.domain.user.User;
import com.rekomenda.api.domain.user.UserRepository;
import com.rekomenda.api.shared.exception.BusinessException;
import com.rekomenda.api.domain.chat.ChatService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RatingService {

    private static final double DECAY_FACTOR = 0.9;

    /** Must match the key prefix used by RecommendationService */
    private static final String DASHBOARD_CACHE_PREFIX = "rec:dashboard:";

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final MovieService movieService;
    private final RedisTemplate<String, Object> redisTemplate;

    public RatingService(
            RatingRepository ratingRepository,
            UserRepository userRepository,
            MovieService movieService,
            RedisTemplate<String, Object> redisTemplate) {
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.movieService = movieService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public RatingResponse rate(String userId, CreateRatingRequest request) {
        var user = findUser(userId);

        var existingRating = ratingRepository.findByUserIdAndConteudoId(user.getId(), request.conteudoId());

        Rating rating;
        if (existingRating.isPresent()) {
            rating = existingRating.get();
            rating.setTipo(request.tipo());
        } else {
            rating = Rating.builder()
                    .user(user)
                    .conteudoId(request.conteudoId())
                    .tipo(request.tipo())
                    .build();
        }

        ratingRepository.save(rating);
        updateRecommendationWeights(user, request.conteudoId(), request.tipo());

        redisTemplate.delete(DASHBOARD_CACHE_PREFIX + userId);
        redisTemplate.delete(ChatService.EXCLUDED_CACHE_PREFIX + userId);

        return RatingResponse.from(rating);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getHistory(String userId) {
        var user = findUser(userId);
        return ratingRepository.findByUserIdOrderByDataAvaliacaoDesc(user.getId())
                .stream()
                .map(RatingResponse::from)
                .toList();
    }

    /**
     * Fetches the movie's genre IDs via MovieService (Redis-cached) and applies
     * the rating delta to the user's genre weight map, then normalises the map.
     */
    private void updateRecommendationWeights(User user, Long conteudoId, RatingType tipo) {
        var movieOpt = movieService.getTmdbMovieById(conteudoId);
        if (movieOpt.isEmpty()) return;
        var movie = movieOpt.get();
        if (movie.genreIds() == null || movie.genreIds().isEmpty()) return;

        var weights = new HashMap<>(user.getRecommendationWeights());
        var delta = tipo.getDelta() * DECAY_FACTOR;

        for (var genreId : movie.genreIds()) {
            var key = String.valueOf(genreId);
            weights.compute(key, (k, existing) -> (existing == null ? 0.0 : existing) + delta);
        }

        normalise(weights);
        user.setRecommendationWeights(weights);
        userRepository.saveAndFlush(user);
    }

    private void normalise(Map<String, Double> weights) {
        if (weights.isEmpty())
            return;
        var max = weights.values().stream().mapToDouble(v -> v == null ? 0.0 : Math.abs(v)).max().orElse(1.0);
        if (max == 0)
            return;
        weights.replaceAll((k, v) -> (v / max) * 100.0);
    }

    private User findUser(String userId) {
        return userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }
}
