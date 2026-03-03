package com.rekomenda.api.domain.rating;

import com.rekomenda.api.domain.rating.dto.CreateRatingRequest;
import com.rekomenda.api.domain.rating.dto.RatingResponse;
import com.rekomenda.api.domain.user.User;
import com.rekomenda.api.domain.user.UserRepository;
import com.rekomenda.api.infrastructure.tmdb.TmdbClient;
import com.rekomenda.api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RatingService {

    /**
     * Dampens the weight update for each subsequent interaction to prevent any single
     * rating from dominating the preference profile.
     */
    private static final double DECAY_FACTOR = 0.9;

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final TmdbClient tmdbClient;

    public RatingService(
            RatingRepository ratingRepository,
            UserRepository userRepository,
            TmdbClient tmdbClient
    ) {
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.tmdbClient = tmdbClient;
    }

    @Transactional
    public RatingResponse rate(String email, CreateRatingRequest request) {
        var user = findUser(email);

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

        return RatingResponse.from(rating);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getHistory(String email) {
        var user = findUser(email);
        return ratingRepository.findByUserIdOrderByDataAvaliacaoDesc(user.getId())
                .stream()
                .map(RatingResponse::from)
                .toList();
    }

    /**
     * Fetches the movie's genre IDs from TMDB and applies the rating delta to the user's
     * genre weight map, then normalises the map to keep values in a stable range.
     */
    private void updateRecommendationWeights(User user, Long conteudoId, RatingType tipo) {
        var details = tmdbClient.getMovieDetails(conteudoId);
        if (details == null) return;

        @SuppressWarnings("unchecked")
        var genres = (List<Map<String, Object>>) details.get("genres");
        if (genres == null || genres.isEmpty()) return;

        var weights = new HashMap<>(user.getRecommendationWeights());
        var delta = tipo.getDelta() * DECAY_FACTOR;

        for (var genre : genres) {
            var genreId = String.valueOf(genre.get("id"));
            weights.compute(genreId, (k, existing) -> (existing == null ? 0.0 : existing) + delta);
        }

        normalise(weights);
        user.setRecommendationWeights(weights);
        userRepository.save(user);
    }

    /**
     * Scales all weights so the maximum absolute value is 100, preserving relative proportions.
     */
    private void normalise(Map<String, Double> weights) {
        if (weights.isEmpty()) return;
        var max = weights.values().stream().filter(Objects::nonNull).mapToDouble(v -> v).map(Math::abs).max().orElse(1.0);
        if (max == 0) return;
        weights.replaceAll((k, v) -> (v / max) * 100.0);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }
}
