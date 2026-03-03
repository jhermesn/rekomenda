package com.rekomenda.api.domain.rating;

import com.rekomenda.api.domain.rating.dto.CreateRatingRequest;
import com.rekomenda.api.domain.rating.dto.RatingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@Tag(name = "Ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra ou atualiza uma avaliação e recalcula os pesos do perfil")
    public RatingResponse rate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRatingRequest request
    ) {
        return ratingService.rate(jwt.getSubject(), request);
    }

    @GetMapping("/history")
    @Operation(summary = "Retorna o histórico de avaliações em ordem cronológica reversa")
    public List<RatingResponse> getHistory(@AuthenticationPrincipal Jwt jwt) {
        return ratingService.getHistory(jwt.getSubject());
    }
}
