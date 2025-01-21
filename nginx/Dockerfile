FROM nginx:stable-alpine

# 기본 nginx 설정 파일 제거
RUN rm /etc/nginx/conf.d/default.conf

# 사용자 정의 nginx 설정 복사
COPY conf/nginx.conf /etc/nginx/nginx.conf

# SSL 인증서를 위한 디렉토리 생성
RUN mkdir -p /etc/nginx/ssl

# 로그 디렉토리 생성
RUN mkdir -p /var/log/nginx