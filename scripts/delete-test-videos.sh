#!/bin/bash

# Usage: ./scripts/delete-test-videos.sh <SESSION_COOKIE_VALUE_OR_HEADER> [API_URL] [COOKIE_NAME]
# Example: ./scripts/delete-test-videos.sh "ORVSESSION=abc..." https://api.orv.im
# Example: ./scripts/delete-test-videos.sh "abc..." https://api.orv.im ORVSESSION

if [ -z "$1" ]; then
  echo "Error: session cookie is required"
  echo "Usage: ./scripts/delete-test-videos.sh <SESSION_COOKIE_VALUE_OR_HEADER> [API_URL] [COOKIE_NAME]"
  exit 1
fi

SESSION_COOKIE="$1"
API_URL="${2:-https://api.orv.im}"
COOKIE_NAME="${3:-ORVSESSION}"
ADMIN_API="$API_URL/api/admin"

if [[ "$SESSION_COOKIE" == *"="* ]]; then
  COOKIE_HEADER="Cookie: $SESSION_COOKIE"
else
  COOKIE_HEADER="Cookie: $COOKIE_NAME=$SESSION_COOKIE"
fi

echo "Using API URL: $ADMIN_API"
echo "Fetching members with provider='test'..."

# 1. provider가 'test'인 멤버들 조회
members=$(curl -s -H "$COOKIE_HEADER" "$ADMIN_API/members?provider=test" | jq -r '.data[].id')

if [ -z "$members" ]; then
  echo "No members found with provider='test'"
  exit 0
fi

member_count=$(echo "$members" | wc -l | tr -d ' ')
echo "Found $member_count members"

total_deleted=0

for member_id in $members; do
  echo ""
  echo "Processing member: $member_id"

  # 2. 해당 멤버의 video 목록 조회
  videos=$(curl -s -H "$COOKIE_HEADER" "$ADMIN_API/archive/videos?memberId=$member_id" | jq -r '.data[].id')

  if [ -z "$videos" ]; then
    echo "  No videos found"
    continue
  fi

  video_count=$(echo "$videos" | wc -l | tr -d ' ')
  echo "  Found $video_count videos"

  for video_id in $videos; do
    echo "  Deleting video: $video_id"
    result=$(curl -s -H "$COOKIE_HEADER" -X DELETE "$ADMIN_API/archive/video/$video_id")
    status=$(echo "$result" | jq -r '.message')
    echo "    Result: $status"
    ((total_deleted++))
  done
done

echo ""
echo "Done! Total deleted: $total_deleted videos"
