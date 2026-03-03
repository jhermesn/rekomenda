package com.rekomenda.api.domain.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(UUID token);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(UUID userId);
}
