#!/usr/bin/env bash
# Optional helper: confirm MySQL + RabbitMQ containers respond (Compose must be up).
set -euo pipefail

echo "==> MySQL ping"
docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -proot --silent
echo "    MySQL OK"

echo "==> RabbitMQ ping"
docker compose exec -T rabbitmq rabbitmq-diagnostics -q ping
echo "    RabbitMQ OK"

echo "==> Compose deps smoke OK"
