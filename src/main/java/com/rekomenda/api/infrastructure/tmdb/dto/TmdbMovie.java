package com.rekomenda.api.infrastructure.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbMovie(
        long id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        String overview,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("vote_average") double voteAverage,
        @JsonProperty("genre_ids") List<Long> genreIds
) {}
