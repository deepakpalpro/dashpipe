#!/usr/bin/env bash
# W0-US01 LocalStack smoke: create/list S3 bucket and SQS queue (idempotent).
set -euo pipefail

# Default host port 4567 (see docker-compose.yml LOCALSTACK_HOST_PORT).
ENDPOINT="${LOCALSTACK_ENDPOINT:-http://localhost:4567}"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"
BUCKET="${SMOKE_S3_BUCKET:-pp-wave0-smoke}"
QUEUE="${SMOKE_SQS_QUEUE:-pp-wave0-smoke}"

export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"
export AWS_DEFAULT_REGION="$REGION"

aws_local() {
  if command -v awslocal >/dev/null 2>&1; then
    awslocal "$@"
  else
    aws --endpoint-url="$ENDPOINT" "$@"
  fi
}

echo "==> Waiting for LocalStack at ${ENDPOINT} ..."
for i in $(seq 1 60); do
  if curl -sf "${ENDPOINT}/_localstack/health" >/dev/null 2>&1; then
    echo "    LocalStack is up (attempt ${i})"
    break
  fi
  if [[ "$i" -eq 60 ]]; then
    echo "ERROR: LocalStack did not become healthy within 60s" >&2
    exit 1
  fi
  sleep 1
done

echo "==> S3: ensure bucket ${BUCKET}"
if aws_local s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
  echo "    bucket already exists"
else
  aws_local s3api create-bucket --bucket "$BUCKET" >/dev/null
  echo "    bucket created"
fi
aws_local s3api list-buckets --query "Buckets[?Name=='${BUCKET}'].Name" --output text | grep -qx "$BUCKET"

echo "==> SQS: ensure queue ${QUEUE}"
QUEUE_URL=$(aws_local sqs create-queue --queue-name "$QUEUE" --query 'QueueUrl' --output text)
echo "    queue url: ${QUEUE_URL}"
aws_local sqs get-queue-url --queue-name "$QUEUE" --query 'QueueUrl' --output text | grep -q "$QUEUE"

echo "==> Smoke OK (S3 + SQS via LocalStack)"
