-- Each driver's trip history, so it follows the account to another phone. The trip itself
-- (route, events, score) is kept as JSON; the columns beside it are for sorting and listing.
CREATE TABLE trips (
    account_id UUID        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    id         TEXT        NOT NULL,
    saved_at   TIMESTAMPTZ NOT NULL,
    score      INT         NOT NULL,
    data       JSONB       NOT NULL,
    PRIMARY KEY (account_id, id)
);

CREATE INDEX trips_by_account ON trips (account_id, saved_at DESC);
