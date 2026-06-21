#!/bin/bash
set -euo pipefail

BLUE_PORT=8080
GREEN_PORT=8081
BLUE_SERVICE="handmade-market-blue"
GREEN_SERVICE="handmade-market-green"
NGINX_UPSTREAM_FILE="/etc/nginx/conf.d/service-url.inc"

# 현재 nginx가 바라보는 포트를 읽어 다음 배포 대상을 반대편 서비스로 정함
CURRENT_PORT=$(grep -oE '808[01]' "$NGINX_UPSTREAM_FILE")

if [ "$CURRENT_PORT" = "$BLUE_PORT" ]; then
    IDLE_PORT=$GREEN_PORT
    IDLE_SERVICE=$GREEN_SERVICE
    ACTIVE_SERVICE=$BLUE_SERVICE
else
    IDLE_PORT=$BLUE_PORT
    IDLE_SERVICE=$BLUE_SERVICE
    ACTIVE_SERVICE=$GREEN_SERVICE
fi

echo "현재 활성: $CURRENT_PORT  배포 대상: $IDLE_PORT"

# idle 서비스가 남아 있으면 먼저 정리해 새 jar 기준으로 다시 시작하게 함
sudo systemctl stop "$IDLE_SERVICE" 2>/dev/null || true
sleep 2

echo "$IDLE_SERVICE 시작..."
sudo systemctl start "$IDLE_SERVICE"

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
        sudo systemctl stop "$IDLE_SERVICE"
        exit 1
    fi
done

# 헬스체크가 끝난 새 포트로 nginx upstream을 전환해 무중단 배포를 완료함
echo "set \$service_url http://127.0.0.1:$IDLE_PORT;" | sudo tee "$NGINX_UPSTREAM_FILE" > /dev/null
sudo nginx -s reload

echo "Nginx 전환 완료"

# 트래픽 전환 뒤 기존 active 서비스를 내려 EC2 자원을 회수함
sleep 3
sudo systemctl stop "$ACTIVE_SERVICE"
echo "배포 완료: 포트 $IDLE_PORT 서비스 중"
