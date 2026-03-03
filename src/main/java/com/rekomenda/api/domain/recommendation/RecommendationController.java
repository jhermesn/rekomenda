package com.rekomenda.api.domain.recommendation;

import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Retorna filmes recomendados baseados nos pesos de gênero do usuário")
    public List<MovieResponse> getDashboard(@AuthenticationPrincipal Jwt jwt) {
        return recommendationService.getDashboard(jwt.getSubject());
    }
}
