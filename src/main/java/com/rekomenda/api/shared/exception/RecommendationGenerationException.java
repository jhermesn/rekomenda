package com.rekomenda.api.shared.exception;

/**
 * Thrown when collective recommendation generation fails (e.g. timeout, execution error).
 * Room state is rolled back and clients are notified via RECOMMENDATIONS_FAILED before this is thrown.
 */
public class RecommendationGenerationException extends RuntimeException {

    public RecommendationGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
