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
import com.rekomenda.api.infrastructure.tmdb.dto.TmdbMovie;
import com.rekomenda.api.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class ChatService {

    public static final String EXCLUDED_CACHE_PREFIX = "chat:excluded:";

    private static final int MAX_EXCLUDED_TITLES = 25;
    private static final int MAX_EXCLUDED_TITLES_IN_PROMPT = 12;
    private static final int SEARCH_RESULTS_TO_FETCH = 30;
    private static final int MAX_ATTEMPTS = 3;

    private final GeminiService geminiService;
    private final TmdbClient tmdbClient;
    private final RatingRepository ratingRepository;
    private final MovieService movieService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration excludedCacheTtl;

    public ChatService(
            GeminiService geminiService,
            TmdbClient tmdbClient,
            RatingRepository ratingRepository,
            MovieService movieService,
            UserRepository userRepository,
            RedisTemplate<String, Object> redisTemplate,
            @Value("${app.chat.excluded-cache-ttl-minutes:1}") long excludedCacheTtlMinutes) {
        this.geminiService = geminiService;
        this.tmdbClient = tmdbClient;
        this.ratingRepository = ratingRepository;
        this.movieService = movieService;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.excludedCacheTtl = Duration.ofMinutes(excludedCacheTtlMinutes);
    }

    /**
     * Asks Gemini for a movie title matching the user's description, excluding
     * movies they have already rated and any temporarily blacklisted (e.g. from "more options").
     */
    public ChatResponse recommend(ChatRequest request, String userId) {
        var user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        int userAge = AgeCertification.ageFrom(user.getDataNascimento());

        var ratedIds = getCachedRatedIds(userId);

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
            var excludedForPrompt = attemptExcludedTitles.stream().limit(MAX_EXCLUDED_TITLES_IN_PROMPT).toList();
            String suggestedTitle;
            try {
                suggestedTitle = geminiService.recommendForIndividual(request.descricao(), excludedForPrompt, userAge);
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

        var fallback = tryDiscoverFallback(request.descricao(), excludedIds, userAge, userId);
        if (fallback != null) return fallback;

        throw new BusinessException("Não encontramos uma recomendação para essa descrição. Tente outra.", HttpStatus.NOT_FOUND);
    }

    private List<Long> getCachedRatedIds(String userId) {
        String key = EXCLUDED_CACHE_PREFIX + userId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list && !list.isEmpty()) {
            return list.stream()
                    .filter(Number.class::isInstance)
                    .map(o -> ((Number) o).longValue())
                    .limit(MAX_EXCLUDED_TITLES)
                    .toList();
        }

        var ratedIds = ratingRepository.findByUserIdOrderByDataAvaliacaoDesc(UUID.fromString(userId))
                .stream()
                .limit(MAX_EXCLUDED_TITLES)
                .map(Rating::getConteudoId)
                .toList();

        redisTemplate.opsForValue().set(key, ratedIds, excludedCacheTtl);
        return ratedIds;
    }

    private ChatResponse tryDiscoverFallback(String descricao, Set<Long> excludedIds, int userAge, String userId) {
        try {
            var keywords = geminiService.extractKeywords(descricao);
            if (keywords.isEmpty()) return null;

            var genreMap = tmdbClient.fetchGenreMapForMatching();
            var genreIds = keywords.stream()
                    .map(String::toLowerCase)
                    .flatMap(k -> resolveGenreId(k, genreMap))
                    .distinct()
                    .toList();

            if (genreIds.isEmpty()) return null;

            String certCountry = AgeCertification.certificationCountry();
            String certLte = AgeCertification.certificationLteForAge(userAge);
            int seed = userId.hashCode() ^ (int) System.currentTimeMillis();

            List<TmdbMovie> discovered = tmdbClient.discoverByGenres(genreIds, SEARCH_RESULTS_TO_FETCH, seed, certCountry, certLte);
            Optional<TmdbMovie> first = discovered.stream()
                    .filter(m -> m.overview() != null && !m.overview().isBlank())
                    .filter(m -> !excludedIds.contains(m.id()))
                    .findFirst();

            return first.map(m -> new ChatResponse(MovieResponse.from(m))).orElse(null);
        } catch (Exception _) {
            return null;
        }
    }

    private static Stream<Long> resolveGenreId(String keyword, Map<String, Long> genreMap) {
        var direct = genreMap.get(keyword);
        if (direct != null) return Stream.of(direct);
        for (var word : keyword.split("\\s+")) {
            var id = genreMap.get(word);
            if (id != null) return Stream.of(id);
        }
        return Stream.empty();
    }
}
