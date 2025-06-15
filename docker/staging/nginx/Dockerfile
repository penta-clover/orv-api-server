# nginx/Dockerfile
FROM nginx:latest

# openssl 설치 (Debian 기반 nginx 이미지는 apt-get 사용 가능)
RUN apt-get update && apt-get install -y openssl

# 인증서와 키를 저장할 디렉토리 생성
RUN mkdir -p /etc/nginx/certs

# self-signed 인증서 생성
RUN openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -subj "/CN=staging.api.orv.im" \
    -keyout /etc/nginx/certs/staging.api.orv.im.key \
    -out /etc/nginx/certs/staging.api.orv.im.crt

# nginx 설정 파일을 복사 (로컬의 staging.conf 사용)
COPY staging.conf /etc/nginx/conf.d/

EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]
