#!/usr/bin/env bash
# Smoke: csv → filter via RabbitMQ stage queues (IO_MODE=queue).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

AMQP_URL="${AMQP_URL:-amqp://pipeline:pipeline@rabbitmq:5672/}"
Q_IN="smoke.pipelet.csv.in"
Q_MID="smoke.pipelet.filter.in"
Q_OUT="smoke.pipelet.filter.out"
NETWORK="${DOCKER_NETWORK:-dashpipe_default}"
CSV_IMAGE="${CSV_IMAGE:-dashpipe/plet-csv-to-json:local}"
FILTER_IMAGE="${FILTER_IMAGE:-dashpipe/plet-python-filter:local}"

echo "==> Building pipelet images (if needed)"
docker build -f pipelets/plet-csv-to-json/Dockerfile -t "$CSV_IMAGE" pipelets >/dev/null
docker build -f pipelets/plet-python-filter/Dockerfile -t "$FILTER_IMAGE" pipelets >/dev/null

echo "==> Declaring queues on RabbitMQ"
for q in "$Q_IN" "$Q_MID" "$Q_OUT"; do
  docker run --rm --network "$NETWORK" curlimages/curl:8.5.0 -sS -u pipeline:pipeline \
    -H 'content-type: application/json' \
    -X PUT "http://rabbitmq:15672/api/queues/%2F/${q}" \
    -d '{"durable":true}' >/dev/null || true
  docker run --rm --network "$NETWORK" curlimages/curl:8.5.0 -sS -u pipeline:pipeline \
    -X DELETE "http://rabbitmq:15672/api/queues/%2F/${q}/contents" >/dev/null || true
done

echo "==> Publishing kickoff CSV message"
python3 - <<'PY' | docker run --rm -i --network "$NETWORK" curlimages/curl:8.5.0 -sS -u pipeline:pipeline \
  -H 'content-type: application/json' \
  -X POST "http://rabbitmq:15672/api/exchanges/%2F/amq.default/publish" \
  -d @-
import json
print(json.dumps({
  "properties": {"delivery_mode": 2},
  "routing_key": "smoke.pipelet.csv.in",
  "payload": json.dumps({"csv": "sku,qty\nA,5\nB,0\nC,2\n"}),
  "payload_encoding": "string",
}))
PY

echo "==> Running csv-to-json (queue)"
docker run --rm --network "$NETWORK" \
  -e IO_MODE=queue \
  -e AMQP_URL="$AMQP_URL" \
  -e INPUT_QUEUE="$Q_IN" \
  -e OUTPUT_QUEUE="$Q_MID" \
  -e DEPLOYMENT_CONFIG='{"region":"us-east-1"}' \
  -e EXECUTION_CONFIG='{"delimiter":",","hasHeader":"true"}' \
  "$CSV_IMAGE"

echo "==> Running python-filter (queue)"
docker run --rm --network "$NETWORK" \
  -e IO_MODE=queue \
  -e AMQP_URL="$AMQP_URL" \
  -e INPUT_QUEUE="$Q_MID" \
  -e OUTPUT_QUEUE="$Q_OUT" \
  -e DEPLOYMENT_CONFIG='{"region":"us-east-1"}' \
  -e EXECUTION_CONFIG='{"expression":"int(qty) > 0"}' \
  "$FILTER_IMAGE"

echo "==> Fetching result"
RESULT="$(docker run --rm --network "$NETWORK" curlimages/curl:8.5.0 -sS -u pipeline:pipeline \
  -H 'content-type: application/json' \
  -X POST "http://rabbitmq:15672/api/queues/%2F/${Q_OUT}/get" \
  -d '{"count":1,"ackmode":"ack_requeue_false","encoding":"auto"}')"

echo "$RESULT" | python3 -c '
import json,sys
raw=sys.stdin.read()
msgs=json.loads(raw)
assert msgs, "no message on output queue"
body=json.loads(msgs[0]["payload"])
assert body.get("recordCount")==2, body
print("OK queue smoke recordCount=", body["recordCount"])
'

echo "==> Stdio chain smoke"
echo '{"csv":"sku,qty\nA,1\n"}' | docker run --rm -i \
  -e IO_MODE=stdio \
  -e DEPLOYMENT_CONFIG='{"region":"us-east-1"}' \
  -e EXECUTION_CONFIG='{"delimiter":",","hasHeader":"true"}' \
  "$CSV_IMAGE" | docker run --rm -i \
  -e IO_MODE=stdio \
  -e DEPLOYMENT_CONFIG='{"region":"us-east-1"}' \
  -e EXECUTION_CONFIG='{"expression":"int(qty) > 0"}' \
  "$FILTER_IMAGE" | python3 -c 'import json,sys; b=json.load(sys.stdin); assert b["recordCount"]==1; print("OK stdio smoke")'
