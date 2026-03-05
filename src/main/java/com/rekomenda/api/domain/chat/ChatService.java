package com.rekomenda.api.domain.chat;

import com.rekomenda.api.domain.chat.dto.ChatRequest;
import com.rekomenda.api.domain.chat.dto.ChatResponse;
import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import com.rekomenda.api.infrastructure.ai.GeminiService;
import com.rekomenda.api.infrastructure.tmdb.TmdbClient;
import com.rekomenda.api.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final GeminiService geminiService;
    private final TmdbClient tmdbClient;

    public ChatService(GeminiService geminiService, TmdbClient tmdbClient) {
        this.geminiService = geminiService;
        this.tmdbClient = tmdbClient;
    }

    /**
     * Asks Gemini for a single movie title matching the user's description,
     * then searches TMDB for it. This interaction is intentionally isolated
     * and does NOT modify the user's recommendation weight profile.
     */
    public ChatResponse recommend(ChatRequest request) {
        String suggestedTitle;
        try {
            suggestedTitle = geminiService.recommendForIndividual(request.descricao());
        } catch (Exception ignored) {
            throw new BusinessException("Serviço de recomendação indisponível no momento",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (suggestedTitle.isBlank()) {
            throw new BusinessException("Não foi possível gerar uma recomendação", HttpStatus.SERVICE_UNAVAILABLE);
        }

        var results = tmdbClient.searchByKeywords(suggestedTitle, 1);

        if (results.isEmpty()) {
            throw new BusinessException("Filme sugerido não encontrado na base de dados", HttpStatus.NOT_FOUND);
        }

        return new ChatResponse(MovieResponse.from(results.get(0)));
    }
}
