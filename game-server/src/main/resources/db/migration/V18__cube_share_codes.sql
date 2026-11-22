-- Shareable cube links. A cube's owner can mint an unguessable code; anyone holding it can read the
-- cube (`GET /api/cubes/shared/{code}`) — always the owner's latest saved version, so a link handed to
-- friends follows the cube as it is tuned. Null = not shared; revoking clears it, so old links stop
-- resolving. Runs only when accounts are enabled.

ALTER TABLE cubes ADD COLUMN share_code TEXT;
CREATE UNIQUE INDEX idx_cubes_share_code ON cubes (share_code) WHERE share_code IS NOT NULL;
