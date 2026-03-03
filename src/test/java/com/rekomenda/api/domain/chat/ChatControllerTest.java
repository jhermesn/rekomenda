package com.rekomenda.api.domain.chat;

import com.rekomenda.api.domain.chat.dto.ChatRequest;
import com.rekomenda.api.domain.chat.dto.ChatResponse;
import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerTest {

    private final ChatService chatService = mock(ChatService.class);
    private final ChatController controller = new ChatController(chatService);

    @Test
    @DisplayName("recommend delega para ChatService.recommend")
    void recommend_delegatesToService() {
        var request = new ChatRequest("Quero um filme de ação leve");
        var movie = new MovieResponse(1L, "Titulo", "Overview", null, "2024-01-01", 8.0);
        var expected = new ChatResponse(movie);

        when(chatService.recommend(any(ChatRequest.class))).thenReturn(expected);

        var response = controller.recommend(request);

        assert response.equals(expected);
        verify(chatService).recommend(request);
    }
}

