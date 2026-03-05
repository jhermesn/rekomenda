package com.rekomenda.api.domain.movie;

import com.rekomenda.api.domain.recommendation.dto.MovieResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Returns movie details for the given TMDB ID.
     * Results are cached in Redis for 7 days to avoid TMDB rate limits.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Retorna detalhes de um filme pelo ID do TMDB (Redis-cached, TTL 7 dias)")
    public ResponseEntity<MovieResponse> getById(@PathVariable long id) {
        MovieResponse movie = movieService.getById(id);
        if (movie == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(movie);
    }
}
