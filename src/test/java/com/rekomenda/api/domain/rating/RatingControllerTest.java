package com.rekomenda.api.domain.rating;

import com.rekomenda.api.domain.rating.dto.CreateRatingRequest;
import com.rekomenda.api.domain.rating.dto.RatingResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RatingControllerTest {

    private final RatingService ratingService = mock(RatingService.class);
    private final RatingController controller = new RatingController(ratingService);

    @Test
    @DisplayName("rate delega para RatingService.rate usando subject do Jwt")
    void rate_delegatesToService() {
        var userId = UUID.randomUUID().toString();
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "HS256"), Map.of("sub", userId));
        var request = new CreateRatingRequest(123L, RatingType.GOSTEI);

        when(ratingService.rate(any(), any())).thenReturn(
                new RatingResponse(UUID.randomUUID(), 123L, RatingType.GOSTEI, Instant.now())
        );

        controller.rate(jwt, request);

        verify(ratingService).rate(userId, request);
    }

    @Test
    @DisplayName("getHistory delega para RatingService.getHistory usando subject do Jwt")
    void getHistory_delegatesToService() {
        var userId = UUID.randomUUID().toString();
        Jwt jwt = new Jwt("token", null, null, Map.of("alg", "HS256"), Map.of("sub", userId));

        when(ratingService.getHistory(userId)).thenReturn(List.of());

        controller.getHistory(jwt);

        verify(ratingService).getHistory(userId);
    }
}

