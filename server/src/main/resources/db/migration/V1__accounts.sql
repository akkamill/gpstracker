-- Driver accounts. Passwords are never stored, only a salted PBKDF2 hash of them.
CREATE TABLE accounts (
    id            UUID PRIMARY KEY,
    email         TEXT        NOT NULL UNIQUE,
    name          TEXT        NOT NULL,
    password_hash TEXT        NOT NULL,
    salt          TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
