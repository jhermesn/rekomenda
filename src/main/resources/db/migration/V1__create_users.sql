CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                   VARCHAR(100) NOT NULL,
    username               VARCHAR(30)  NOT NULL,
    email                  VARCHAR(255) NOT NULL,
    data_nascimento        DATE         NOT NULL,
    senha_hash             VARCHAR(255) NOT NULL,
    recommendation_weights JSONB        NOT NULL DEFAULT '{}',

    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_username ON users (username);
