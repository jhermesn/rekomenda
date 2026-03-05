package com.rekomenda.api.domain.chat;

import com.rekomenda.api.domain.chat.dto.ChatRequest;
import com.rekomenda.api.domain.chat.dto.ChatResponse;
import com.rekomenda.api.domain.movie.MovieService;
import com.rekomenda.api.domain.rating.RatingRepository;
import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import com.rekomenda.api.infrastructure.ai.GeminiService;
import com.rekomenda.api.infrastructure.tmdb.TmdbClient;
import com.rekomenda.api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class ChatService {

    private static final int MAX_EXCLUDED_TITLES = 25;
    private static final int SEARCH_RESULTS_TO_FILTER = 10;

    private final GeminiService geminiService;
    private final TmdbClient tmdbClient;
    private final RatingRepository ratingRepository;
    private final MovieService movieService;

    public ChatService(
            GeminiService geminiService,
            TmdbClient tmdbClient,
            RatingRepository ratingRepository,
            MovieService movieService) {
        this.geminiService = geminiService;
        this.tmdbClient = tmdbClient;
        this.ratingRepository = ratingRepository;
        this.movieService = movieService;
    }

    /**
     * Asks Gemini for a movie title matching the user's description, excluding
     * movies they have already rated. Searches TMDB and filters out rated movies.
     */
    public ChatResponse recommend(ChatRequest request, String userId) {
        var ratedIds = ratingRepository.findByUserIdOrderByDataAvaliacaoDesc(UUID.fromString(userId))
                .stream()
                .limit(MAX_EXCLUDED_TITLES)
                .map(r -> r.getConteudoId())
                .toList();

        var excludedTitles = ratedIds.stream()
                .map(id -> movieService.getTmdbMovieById(id))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get().title())
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .limit(MAX_EXCLUDED_TITLES)
                .toList();

        String suggestedTitle;
        try {
            suggestedTitle = geminiService.recommendForIndividual(request.descricao(), excludedTitles);
        } catch (RuntimeException e) {
            throw new BusinessException("Serviço de recomendação indisponível no momento", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (suggestedTitle.isBlank()) {
            throw new BusinessException("Não foi possível gerar uma recomendação", HttpStatus.SERVICE_UNAVAILABLE);
        }

        var ratedIdSet = Set.copyOf(ratedIds);
        var results = tmdbClient.searchByKeywords(suggestedTitle, SEARCH_RESULTS_TO_FILTER)
                .stream()
                .filter(m -> !ratedIdSet.contains(m.id()))
                .toList();

        if (results.isEmpty()) {
            throw new BusinessException("Filme sugerido não encontrado ou você já avaliou esse filme. Tente outra descrição.", HttpStatus.NOT_FOUND);
        }

        return new ChatResponse(MovieResponse.from(results.getFirst()));
    }
}
