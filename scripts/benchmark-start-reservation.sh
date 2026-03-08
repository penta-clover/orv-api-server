#!/usr/bin/env bash

set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Usage: $0 <BASE_URL> <BEARER_TOKEN> <STORYBOARD_ID>"
  exit 1
fi

BASE_URL="$1"
BEARER_TOKEN="$2"
STORYBOARD_ID="$3"
AUTH_HEADER="Authorization: Bearer $BEARER_TOKEN"
RESERVE_API="$BASE_URL/api/v0/reservation/interview"
CONCURRENCY=10

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: '$1' is required"
    exit 1
  fi
}

now_ns() {
  python3 -c 'import time; print(time.time_ns())'
}

reserve_at_for_index() {
  python3 - "$1" <<'PY'
from datetime import datetime, timedelta, timezone
import sys

index = int(sys.argv[1])
scheduled_at = datetime.now(timezone.utc) + timedelta(days=index + 1)
print(scheduled_at.replace(microsecond=0).isoformat().replace("+00:00", "Z"))
PY
}

require_command curl
require_command jq
require_command python3

tmp_dir="$(mktemp -d)"
cleanup() {
  rm -rf "$tmp_dir"
}
trap cleanup EXIT

echo "Base URL: $BASE_URL"
echo "Storyboard ID: $STORYBOARD_ID"
echo "Creating $CONCURRENCY reservations..."

reservation_ids=()
for i in $(seq 0 $((CONCURRENCY - 1))); do
  reserve_at="$(reserve_at_for_index "$i")"
  request_body="$(jq -nc --arg storyboardId "$STORYBOARD_ID" --arg reservedAt "$reserve_at" '{storyboardId: $storyboardId, reservedAt: $reservedAt}')"
  response="$(curl -sS -X POST "$RESERVE_API" \
    -H "$AUTH_HEADER" \
    -H "Content-Type: application/json" \
    -d "$request_body")"

  status_code="$(echo "$response" | jq -r '.statusCode')"
  if [ "$status_code" != "201" ]; then
    echo "Failed to create reservation[$i]: $response"
    exit 1
  fi

  reservation_id="$(echo "$response" | jq -r '.data.id')"
  reservation_ids+=("$reservation_id")
  echo "Created reservation[$i]: $reservation_id (reservedAt=$reserve_at)"
done

echo "Starting concurrent /start requests..."

for reservation_id in "${reservation_ids[@]}"; do
  (
    start_ns="$(now_ns)"
    body_file="$tmp_dir/$reservation_id.body.json"
    http_code="$(curl -sS -o "$body_file" -w "%{http_code}" -X POST "$RESERVE_API/$reservation_id/start" \
      -H "$AUTH_HEADER")"
    end_ns="$(now_ns)"
    elapsed_ns=$((end_ns - start_ns))
    printf '%s\t%s\t%s\n' "$reservation_id" "$http_code" "$elapsed_ns" > "$tmp_dir/$reservation_id.meta"
  ) &
done

wait

echo
echo "Summary"
printf '%-38s %-8s %-10s %-16s\n' "reservationId" "http" "apiStatus" "elapsedNs"

for reservation_id in "${reservation_ids[@]}"; do
  meta_file="$tmp_dir/$reservation_id.meta"
  body_file="$tmp_dir/$reservation_id.body.json"

  IFS=$'\t' read -r _reservation_id http_code elapsed_ns < "$meta_file"
  api_status="$(jq -r '.statusCode // "N/A"' "$body_file")"

  printf '%-38s %-8s %-10s %-16s\n' "$reservation_id" "$http_code" "$api_status" "$elapsed_ns"
done
