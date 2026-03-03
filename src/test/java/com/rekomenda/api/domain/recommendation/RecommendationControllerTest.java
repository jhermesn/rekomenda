package com.rekomenda.api.domain.recommendation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationControllerTest {

    private final RecommendationService recommendationService = mock(RecommendationService.class);
    private final RecommendationController controller = new RecommendationController(recommendationService);

    @Test
    @DisplayName("getDashboard delega para RecommendationService.getDashboard usando subject do Jwt")
    void getDashboard_delegatesToService() {
        var userId = UUID.randomUUID().toString();
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "HS256"), Map.of("sub", userId));

        when(recommendationService.getDashboard(userId)).thenReturn(List.of());

        controller.getDashboard(jwt);

        verify(recommendationService).getDashboard(userId);
    }
}

