CREATE TABLE password_reset_tokens (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token           UUID        NOT NULL,
    data_expiracao  TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_password_reset_tokens_token UNIQUE (token)
);

CREATE INDEX idx_prt_token      ON password_reset_tokens (token);
CREATE INDEX idx_prt_usuario_id ON password_reset_tokens (usuario_id);
