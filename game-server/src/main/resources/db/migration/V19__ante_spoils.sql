-- Spoils: cards that changed hands in games played for ante (CR 407). Ante is played "for keeps";
-- outside a Limited tournament (where the card moves between the players' pools instead) keeping
-- means a trophy on the profile. One row per signed-in player per card, from their side: won = true
-- for the winner's copy of the record, false for the loser's. Guests and AI seats get no row. Runs
-- only when accounts are enabled.

CREATE TABLE ante_spoils (
    id            BIGSERIAL   PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id       TEXT        NOT NULL,
    won           BOOLEAN     NOT NULL,
    card_name     TEXT        NOT NULL,
    image_uri     TEXT,
    opponent_name TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ante_spoils_user ON ante_spoils (user_id, created_at DESC);
