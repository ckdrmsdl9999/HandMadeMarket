#!/bin/bash
set -euo pipefail

BLUE_PORT=8080
GREEN_PORT=8081
# Docker 컨테이너 이름을 기존 blue/green 서비스 이름과 맞춰 nginx 전환 흐름을 유지함
BLUE_CONTAINER="handmade-market-blue"
GREEN_CONTAINER="handmade-market-green"
CONTAINER_PORT=8080
NGINX_UPSTREAM_FILE="/etc/nginx/conf.d/service-url.inc"
BACKEND_IMAGE="${BACKEND_IMAGE:-handmade-market-backend:latest}"
BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-/home/ubuntu/HandMadeMarket/.env.prod}"

# 현재 nginx가 바라보는 포트를 읽어 다음 배포 대상을 반대편 서비스로 정함
CURRENT_PORT=$(grep -oE '808[01]' "$NGINX_UPSTREAM_FILE" 2>/dev/null | head -n 1 || true)

if [ "$CURRENT_PORT" = "$BLUE_PORT" ]; then
    IDLE_PORT=$GREEN_PORT
    IDLE_CONTAINER=$GREEN_CONTAINER
    ACTIVE_CONTAINER=$BLUE_CONTAINER
elif [ "$CURRENT_PORT" = "$GREEN_PORT" ]; then
    IDLE_PORT=$BLUE_PORT
    IDLE_CONTAINER=$BLUE_CONTAINER
    ACTIVE_CONTAINER=$GREEN_CONTAINER
else
    echo "현재 nginx upstream 포트를 확인할 수 없습니다: $NGINX_UPSTREAM_FILE"
    exit 1
fi

echo "현재 활성: $CURRENT_PORT  배포 대상: $IDLE_PORT"

# 컨테이너에 운영 DB/OAuth/Redis 설정을 주입할 env 파일이 없으면 배포를 중단함
if [ ! -f "$BACKEND_ENV_FILE" ]; then
    echo "운영 환경변수 파일이 없습니다: $BACKEND_ENV_FILE"
    exit 1
fi

# EC2 배포 스크립트는 기존 sudo 권한 흐름을 유지해 runner의 docker 그룹 의존을 줄임
docker_cmd() {
    sudo docker "$@"
}

# idle 컨테이너와 기존 systemd idle 서비스를 정리해 새 Docker 컨테이너가 포트를 잡게 함
docker_cmd rm -f "$IDLE_CONTAINER" 2>/dev/null || true
sudo systemctl stop "$IDLE_CONTAINER" 2>/dev/null || true
sleep 2

echo "$IDLE_CONTAINER 컨테이너 시작..."
docker_cmd run -d \
    --name "$IDLE_CONTAINER" \
    --restart unless-stopped \
    --env-file "$BACKEND_ENV_FILE" \
    -e SPRING_PROFILES_ACTIVE=prod \
    --add-host=host.docker.internal:host-gateway \
    -p "127.0.0.1:$IDLE_PORT:$CONTAINER_PORT" \
    "$BACKEND_IMAGE"

echo "헬스체크 대기..."
for i in $(seq 1 12); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$IDLE_PORT/" 2>/dev/null || echo "000")
    if [ "$STATUS" = "200" ]; then
        echo "헬스체크 통과 (${i}번째 시도)"
        break
    fi

    echo "대기 중... ($i/12, HTTP $STATUS)"
    sleep 5

    if [ "$i" -eq 12 ]; then
        echo "헬스체크 실패. 롤백."
        docker_cmd logs --tail 100 "$IDLE_CONTAINER" || true
        docker_cmd rm -f "$IDLE_CONTAINER" 2>/dev/null || true
        exit 1
    fi
done

# 헬스체크가 끝난 새 포트로 nginx upstream을 전환해 무중단 배포를 완료함
echo "set \$service_url http://127.0.0.1:$IDLE_PORT;" | sudo tee "$NGINX_UPSTREAM_FILE" > /dev/null
sudo nginx -s reload

echo "Nginx 전환 완료"

# 트래픽 전환 뒤 기존 active 서비스를 내려 EC2 자원을 회수함
sleep 3
docker_cmd rm -f "$ACTIVE_CONTAINER" 2>/dev/null || true
sudo systemctl stop "$ACTIVE_CONTAINER" 2>/dev/null || true
echo "배포 완료: 포트 $IDLE_PORT 서비스 중"
