# GitHub 환경 이름
ENV_NAME="test"

while IFS='=' read -r key value || [ -n "$key" ]; do
  # 빈 줄이나 주석(#)은 건너뛰기
  if [[ -z "$key" || "$key" =~ ^# ]]; then
    continue
  fi

  echo "Setting secret: $key"
  # 해당 secret을 환경에 등록 (이미 존재하면 덮어씌워짐)
  gh secret set "$key" --body "$value" --env "$ENV_NAME"
done < .env.test

