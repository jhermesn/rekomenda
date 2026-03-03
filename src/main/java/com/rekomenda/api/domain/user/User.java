package com.rekomenda.api.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    /**
     * Genre weights keyed by TMDB genre ID (as String).
     * Updated on every rating to drive personalised recommendations.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendation_weights", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Double> recommendationWeights = new HashMap<>();
}
