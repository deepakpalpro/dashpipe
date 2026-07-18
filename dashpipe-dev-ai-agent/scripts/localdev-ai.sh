#!/usr/bin/env bash
# dashpipe-dev-ai-agent — local stack (Ollama + API + Streamlit)
#
# Usage:
#   ./dashpipe-dev-ai-agent/scripts/localdev-ai.sh start
#   ./dashpipe-dev-ai-agent/scripts/localdev-ai.sh stop
#   ./dashpipe-dev-ai-agent/scripts/localdev-ai.sh status
#
# Ollama modes (auto-selected on start):
#   - Host Ollama already on :11434 → containers use host.docker.internal:11434
#   - Port 11434 busy (non-Ollama) → bundled Ollama on OLLAMA_HOST_PORT (default 11435)
#   - Port 11434 free → bundled Ollama container on :11434
#
# Override: USE_HOST_OLLAMA=1 | OLLAMA_HOST_PORT=11435 | API_HOST_PORT=8091 | STREAMLIT_HOST_PORT=8502
#
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT/docker-compose.ai.yml"
CHAT_MODEL="${CHAT_MODEL:-llama3.2:1b}"
CODE_MODEL="${CODE_MODEL:-qwen2.5-coder:1.5b}"

log() { printf '==> %s\n' "$*"; }
warn() { printf '==> warn: %s\n' "$*"; }

port_in_use() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

pick_free_port() {
  local preferred="$1"
  local fallback="$2"
  if ! port_in_use "$preferred"; then
    echo "$preferred"
    return
  fi
  if ! port_in_use "$fallback"; then
    warn "Port $preferred in use — using $fallback instead" >&2
    echo "$fallback"
    return
  fi
  echo "$preferred"
}

host_ollama_up() {
  curl -sf "http://127.0.0.1:11434/api/tags" >/dev/null 2>&1
}

pull_models_host() {
  if command -v ollama >/dev/null 2>&1; then
    ollama pull "$CHAT_MODEL" || warn "chat model pull failed"
    ollama pull "$CODE_MODEL" || warn "code model pull failed"
    return
  fi
  warn "ollama CLI not found; pull models manually: ollama pull $CHAT_MODEL && ollama pull $CODE_MODEL"
}

pull_models_container() {
  docker exec df-ai-ollama ollama pull "$CHAT_MODEL" || warn "chat model pull failed"
  docker exec df-ai-ollama ollama pull "$CODE_MODEL" || warn "code model pull failed"
}

resolve_ports() {
  export API_HOST_PORT="$(pick_free_port "${API_HOST_PORT:-8090}" "8091")"
  export STREAMLIT_HOST_PORT="$(pick_free_port "${STREAMLIT_HOST_PORT:-8501}" "8502")"
  API_URL="${API_URL:-http://localhost:${API_HOST_PORT}}"
  UI_URL="${UI_URL:-http://localhost:${STREAMLIT_HOST_PORT}}"
}

resolve_ollama_mode() {
  export OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://ollama:11434}"
  COMPOSE_PROFILE=()
  OLLAMA_MODE="container"

  if [[ "${USE_HOST_OLLAMA:-}" == "1" ]] || host_ollama_up; then
    log "Using host Ollama at http://127.0.0.1:11434"
    export OLLAMA_BASE_URL="http://host.docker.internal:11434"
    OLLAMA_MODE="host"
    return
  fi

  if port_in_use 11434; then
    export OLLAMA_HOST_PORT="$(pick_free_port "${OLLAMA_HOST_PORT:-11435}" "11436")"
    log "Port 11434 is in use — starting bundled Ollama on host port ${OLLAMA_HOST_PORT}"
  else
    export OLLAMA_HOST_PORT="${OLLAMA_HOST_PORT:-11434}"
    log "Starting bundled Ollama on host port ${OLLAMA_HOST_PORT}"
  fi

  COMPOSE_PROFILE=(--profile bundled-ollama)
}

cmd="${1:-start}"
case "$cmd" in
  start)
    resolve_ports
    resolve_ollama_mode
    log "Starting API + Streamlit${OLLAMA_MODE:+ (+ Ollama: $OLLAMA_MODE)}"
    compose_args=(-f docker-compose.ai.yml)
    if ((${#COMPOSE_PROFILE[@]})); then
      compose_args+=("${COMPOSE_PROFILE[@]}")
    fi
    (cd "$ROOT" && docker compose "${compose_args[@]}" up -d --build)
    for i in $(seq 1 120); do
      curl -sf "$API_URL/health" >/dev/null 2>&1 && break
      sleep 1
    done
    log "Pulling chat model $CHAT_MODEL and code model $CODE_MODEL"
    if [[ "${OLLAMA_MODE:-container}" == "host" ]]; then
      pull_models_host
    else
      pull_models_container
    fi
    log "API:        $API_URL"
    log "Streamlit:  $UI_URL"
    if [[ "${OLLAMA_MODE:-}" == "host" ]]; then
      log "Ollama:     http://127.0.0.1:11434 (host / shared)"
    else
      log "Ollama:     http://127.0.0.1:${OLLAMA_HOST_PORT:-11434} (container)"
    fi
    log "Tip: start dashpipe-api first (./dashpipe-ci_cd/scripts/localdev.sh start)"
    ;;
  stop)
    (cd "$ROOT" && docker compose -f docker-compose.ai.yml --profile bundled-ollama down)
    ;;
  status)
    resolve_ports
    curl -sf "$API_URL/health" && log "API healthy at $API_URL" || log "API not reachable at $API_URL"
    curl -sf "$UI_URL" >/dev/null && log "Streamlit reachable at $UI_URL" || log "Streamlit not reachable at $UI_URL"
    host_ollama_up && log "Host Ollama reachable on :11434" || log "Host Ollama not reachable on :11434"
    docker ps --filter name=df-ai-ollama --format '{{.Names}}' 2>/dev/null | grep -q df-ai-ollama \
      && log "Bundled Ollama container running" \
      || log "No bundled Ollama container"
    ;;
  *)
    echo "Usage: $0 {start|stop|status}" >&2
    exit 1
    ;;
esac
