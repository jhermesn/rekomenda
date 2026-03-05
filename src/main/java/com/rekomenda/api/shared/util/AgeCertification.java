package com.rekomenda.api.shared.util;

import java.time.LocalDate;
import java.time.Period;

/**
 * Maps user age to TMDB certification for Brazil (BR).
 * Used to filter movies by the youngest participant's age in rooms,
 * or by the user's age in individual recommendations.
 */
public final class AgeCertification {

    private static final String COUNTRY_BR = "BR";
    private static final String LIVRE = "Livre";
    private static final String R10 = "10";
    private static final String R12 = "12";
    private static final String R14 = "14";
    private static final String R16 = "16";
    private static final String R18 = "18";

    private AgeCertification() {}

    public static int ageFrom(LocalDate birthDate) {
        if (birthDate == null) return 18;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Returns the BR certification string for TMDB certification.lte.
     * Livre (0-9), 10 (10-11), 12 (12-13), 14 (14-15), 16 (16-17), 18 (18+).
     */
    public static String certificationLteForAge(int age) {
        if (age < 10) return LIVRE;
        if (age < 12) return R10;
        if (age < 14) return R12;
        if (age < 16) return R14;
        if (age < 18) return R16;
        return R18;
    }

    public static String certificationCountry() {
        return COUNTRY_BR;
    }
}
