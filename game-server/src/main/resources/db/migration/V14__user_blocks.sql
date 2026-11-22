-- Blocking another account. A block is one-directional (blocker -> blocked) but its effects are
-- mutual: matchmaking never pairs the two, neither can send the other a friend request or rematch,
-- and the blocked player's emotes stop reaching the blocker. Blocking also ends any friendship or
-- pending request between them. Guests can block too, but only for their session — those blocks
-- live in memory and never reach this table. Runs only when accounts are enabled.

CREATE TABLE user_blocks (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_blocks_distinct    CHECK (blocker_id <> blocked_id),
    CONSTRAINT user_blocks_unique_pair UNIQUE (blocker_id, blocked_id)
);
CREATE INDEX idx_user_blocks_blocker ON user_blocks (blocker_id);
CREATE INDEX idx_user_blocks_blocked ON user_blocks (blocked_id);
