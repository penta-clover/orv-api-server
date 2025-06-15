FROM nginx:latest

# openssl 설치 (Debian 기반 nginx 이미지는 apt-get 사용 가능)
RUN apt-get update && apt-get install -y openssl

# 인증서가 없다면 self-signed 인증서를 생성하거나 기존 인증서를 복사합니다.
# 예시로 self-signed 인증서 생성 (실제 환경에서는 적절한 인증서를 사용)
RUN mkdir -p /etc/nginx/certs && \
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -subj "/CN=localstack.example.com" \
        -keyout /etc/nginx/certs/localstack.key \
        -out /etc/nginx/certs/localstack.crt

COPY localstack-nginx.conf /etc/nginx/conf.d/

EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]
