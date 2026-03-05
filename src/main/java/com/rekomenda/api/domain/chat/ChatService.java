package com.rekomenda.api.domain.chat;

import com.rekomenda.api.domain.chat.dto.ChatRequest;
import com.rekomenda.api.domain.chat.dto.ChatResponse;
import com.rekomenda.api.domain.movie.MovieService;
import com.rekomenda.api.domain.rating.Rating;
import com.rekomenda.api.domain.rating.RatingRepository;
import com.rekomenda.api.domain.user.UserRepository;
import com.rekomenda.api.shared.util.AgeCertification;
import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import com.rekomenda.api.infrastructure.ai.GeminiService;
import com.rekomenda.api.infrastructure.tmdb.TmdbClient;
import com.rekomenda.api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatService {

    private static final int MAX_EXCLUDED_TITLES = 25;
    private static final int SEARCH_RESULTS_TO_FETCH = 30;
    private static final int MAX_ATTEMPTS = 3;

    private final GeminiService geminiService;
    private final TmdbClient tmdbClient;
    private final RatingRepository ratingRepository;
    private final MovieService movieService;
    private final UserRepository userRepository;

    public ChatService(
            GeminiService geminiService,
            TmdbClient tmdbClient,
            RatingRepository ratingRepository,
            MovieService movieService,
            UserRepository userRepository) {
        this.geminiService = geminiService;
        this.tmdbClient = tmdbClient;
        this.ratingRepository = ratingRepository;
        this.movieService = movieService;
        this.userRepository = userRepository;
    }

    /**
     * Asks Gemini for a movie title matching the user's description, excluding
     * movies they have already rated and any temporarily blacklisted (e.g. from "more options").
     */
    public ChatResponse recommend(ChatRequest request, String userId) {
        var user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        int userAge = AgeCertification.ageFrom(user.getDataNascimento());

        var ratedIds = ratingRepository.findByUserIdOrderByDataAvaliacaoDesc(UUID.fromString(userId))
                .stream()
                .limit(MAX_EXCLUDED_TITLES)
                .map(Rating::getConteudoId)
                .toList();

        var excludedIds = new HashSet<>(ratedIds);
        excludedIds.addAll(request.excludedMovieIds());

        var excludedTitles = excludedIds.stream()
                .map(id -> movieService.getTmdbMovieById(id.longValue()))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().title())
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .limit(MAX_EXCLUDED_TITLES)
                .toList();

        var attemptExcludedTitles = new java.util.ArrayList<>(excludedTitles);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String suggestedTitle;
            try {
                suggestedTitle = geminiService.recommendForIndividual(request.descricao(), attemptExcludedTitles, userAge);
            } catch (RuntimeException _) {
                throw new BusinessException("Serviço de recomendação indisponível no momento", HttpStatus.SERVICE_UNAVAILABLE);
            }

            if (suggestedTitle.isBlank()) {
                throw new BusinessException("Não foi possível gerar uma recomendação", HttpStatus.SERVICE_UNAVAILABLE);
            }

            int seed = userId.hashCode() ^ (int) System.currentTimeMillis() ^ attempt;
            var results = tmdbClient.searchByKeywords(suggestedTitle, SEARCH_RESULTS_TO_FETCH, seed)
                    .stream()
                    .filter(m -> !excludedIds.contains(m.id()))
                    .toList();

            if (!results.isEmpty()) {
                return new ChatResponse(MovieResponse.from(results.getFirst()));
            }

            attemptExcludedTitles.add(suggestedTitle.trim());
        }

        throw new BusinessException("Filme sugerido não encontrado ou você já avaliou esse filme. Tente outra descrição.", HttpStatus.NOT_FOUND);
    }
}
