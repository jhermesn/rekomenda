CREATE TABLE ratings (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    conteudo_id     BIGINT      NOT NULL,
    tipo            VARCHAR(20) NOT NULL,
    data_avaliacao  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_ratings_usuario_conteudo UNIQUE (usuario_id, conteudo_id),
    CONSTRAINT chk_ratings_tipo CHECK (
        tipo IN ('GOSTEI', 'INTERESSANTE', 'NEUTRO', 'NAO_INTERESSANTE', 'NAO_GOSTEI')
    )
);

CREATE INDEX idx_ratings_usuario_id ON ratings (usuario_id);
CREATE INDEX idx_ratings_data       ON ratings (usuario_id, data_avaliacao DESC);
