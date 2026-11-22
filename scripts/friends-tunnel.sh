#!/usr/bin/env bash
# Serve this checkout to friends over the internet through a Cloudflare quick tunnel.
#
#   scripts/friends-tunnel.sh            # build the client, start server + preview + tunnel
#   PROFILE= scripts/friends-tunnel.sh   # the production AI instead of the gameplay pilot
#
# What runs: the game server on :8080 (production Spring profile — no dev endpoints, no debug mode,
# no AI insight panel, no easter eggs), the production web-client bundle under `vite preview` on
# $PORT (it proxies /api and /game to the server, websockets included), and `cloudflared tunnel
# --url`, which prints a random https://*.trycloudflare.com address to share. Nothing is opened on
# the router; the URL dies with the tunnel. Ctrl-C stops all three.
#
# Games live in server memory (no Redis, no accounts) and die with the server. Finished games are
# still exported to $GAME_REPLAY_EXPORT_DIR. The admin replay page is behind a random password,
# printed once at start.
set -euo pipefail
cd "$(dirname "$0")/.."

PROFILE="${PROFILE-raceclock+timing+correction-actions+determinize+fixing+grants+locked+idle+eot+rollout}"
MODEL_DIR="${MODEL_DIR:-$HOME/mtg/models/rc2_actions}"
PORT="${PORT:-4173}"
CLOUDFLARED="${CLOUDFLARED:-$(command -v cloudflared || echo "$HOME/.local/bin/cloudflared")}"
ADMIN_PASSWORD="${GAME_ADMIN_PASSWORD:-$(head -c 12 /dev/urandom | od -An -tx1 | tr -d ' \n')}"
REPLAYS="${GAME_REPLAY_EXPORT_DIR:-$HOME/mtg/artifacts/engine_games/$(date +%Y-%m-%d)/replays}"

[ -x "$CLOUDFLARED" ] || { echo "cloudflared not found (set CLOUDFLARED=/path/to/cloudflared)" >&2; exit 1; }
if [ -n "$PROFILE" ] && [ ! -d "$MODEL_DIR" ]; then
  echo "pilot correction not found at $MODEL_DIR (set MODEL_DIR, or PROFILE= for the production AI)" >&2; exit 1
fi
for p in 8080 "$PORT"; do
  if (exec 3<>"/dev/tcp/127.0.0.1/$p") 2>/dev/null; then echo "port $p is already in use" >&2; exit 1; fi
done

echo "== building the web client"
(cd web-client && npm run build)

pids=()
cleanup() { kill "${pids[@]}" 2>/dev/null || true; wait 2>/dev/null || true; }
trap cleanup EXIT INT TERM

echo "== starting the game server (AI profile: ${PROFILE:-production})"
GAME_AI_PROFILE="$PROFILE" \
GAME_DEV_ENDPOINTS_ENABLED=false \
GAME_EASTER_EGGS_ENABLED=false \
GAME_ADMIN_PASSWORD="$ADMIN_PASSWORD" \
GAME_REPLAY_EXPORT_DIR="$REPLAYS" \
  ./gradlew :game-server:bootRun --console=plain -Dargentum.ai.apprentice.dir="$MODEL_DIR" &
pids+=($!)

echo "== waiting for the server on :8080"
for _ in $(seq 1 180); do
  (exec 3<>/dev/tcp/127.0.0.1/8080) 2>/dev/null && break
  sleep 2
done
(exec 3<>/dev/tcp/127.0.0.1/8080) 2>/dev/null || { echo "server did not come up" >&2; exit 1; }

echo "== serving the client on :$PORT"
(cd web-client && VITE_PREVIEW_ALLOWED_HOSTS=".trycloudflare.com" npx vite preview --port "$PORT" --strictPort) &
pids+=($!)

echo "== admin password for /admin this run: $ADMIN_PASSWORD"
echo "== opening the tunnel; share the https://….trycloudflare.com URL it prints"
"$CLOUDFLARED" tunnel --no-autoupdate --url "http://localhost:$PORT" &
pids+=($!)
wait -n "${pids[@]}"
