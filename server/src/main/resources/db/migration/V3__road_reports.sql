-- Road reports shared between drivers, and who answered "still there?" for each.
CREATE TABLE road_reports (
    id                TEXT PRIMARY KEY,
    reporter_id       UUID REFERENCES accounts (id) ON DELETE SET NULL,
    type              TEXT             NOT NULL,
    latitude          DOUBLE PRECISION NOT NULL,
    longitude         DOUBLE PRECISION NOT NULL,
    heading_deg       DOUBLE PRECISION,
    road_name         TEXT,
    created_at        TIMESTAMPTZ      NOT NULL,
    expires_at        TIMESTAMPTZ      NOT NULL,
    still_there_votes INT              NOT NULL DEFAULT 0,
    gone_votes        INT              NOT NULL DEFAULT 0
);

CREATE INDEX road_reports_active ON road_reports (expires_at);

CREATE TABLE road_report_votes (
    report_id   TEXT    NOT NULL REFERENCES road_reports (id) ON DELETE CASCADE,
    account_id  UUID    NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    still_there BOOLEAN NOT NULL,
    PRIMARY KEY (report_id, account_id)
);
