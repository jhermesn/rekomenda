package com.rekomenda.api.infrastructure.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TmdbPageResponse(
        int page,
        List<TmdbMovie> results,
        @JsonProperty("total_results") int totalResults,
        @JsonProperty("total_pages") int totalPages
) {}
