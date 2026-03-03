package com.rekomenda.api.domain.rating;

import com.rekomenda.api.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "conteudo_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "conteudo_id", nullable = false)
    private Long conteudoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatingType tipo;

    @CreationTimestamp
    @Column(name = "data_avaliacao", nullable = false, updatable = false)
    private Instant dataAvaliacao;
}
