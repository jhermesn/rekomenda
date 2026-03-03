package com.rekomenda.api.domain.rating;

/**
 * Five-level rating scale. The delta value drives the weight update formula:
 * peso[genreId] += delta * decayFactor
 */
public enum RatingType {
    GOSTEI(2.0),
    INTERESSANTE(1.0),
    NEUTRO(0.0),
    NAO_INTERESSANTE(-1.0),
    NAO_GOSTEI(-2.0);

    private final double delta;

    RatingType(double delta) {
        this.delta = delta;
    }

    public double getDelta() {
        return delta;
    }
}
