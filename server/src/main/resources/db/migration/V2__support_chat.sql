-- Support chat: one conversation per driver account, and its messages.
CREATE TABLE support_conversations (
    id         UUID PRIMARY KEY,
    account_id UUID        NOT NULL UNIQUE REFERENCES accounts (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE support_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id UUID        NOT NULL REFERENCES support_conversations (id) ON DELETE CASCADE,
    sender          TEXT        NOT NULL CHECK (sender IN ('Driver', 'Agent')),
    text            TEXT        NOT NULL,
    client_id       TEXT,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX support_messages_by_conversation ON support_messages (conversation_id, id);
-- a message sent again after a lost connection (same client_id) is stored once
CREATE UNIQUE INDEX support_messages_once ON support_messages (conversation_id, client_id) WHERE client_id IS NOT NULL;
CREATE INDEX support_conversations_by_activity ON support_conversations (updated_at DESC);
