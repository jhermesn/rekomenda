package com.rekomenda.api.domain.recommendation.dto;

import com.rekomenda.api.infrastructure.tmdb.dto.TmdbMovie;

public record MovieResponse(
        long id,
        String title,
        String overview,
        String posterUrl,
        String releaseDate,
        double voteAverage
) {
    private static final String TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500";

    public static MovieResponse from(TmdbMovie movie) {
        var poster = movie.posterPath() != null ? TMDB_IMAGE_BASE + movie.posterPath() : null;
        return new MovieResponse(
                movie.id(),
                movie.title(),
                movie.overview(),
                poster,
                movie.releaseDate(),
                movie.voteAverage()
        );
    }
}
