package com.rekomenda.api.domain.rating;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    List<Rating> findByUserIdOrderByDataAvaliacaoDesc(UUID userId);

    Optional<Rating> findByUserIdAndConteudoId(UUID userId, Long conteudoId);
}
