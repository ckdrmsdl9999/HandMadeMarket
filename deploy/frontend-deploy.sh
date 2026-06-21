#!/bin/bash
set -euo pipefail

APP_DIR="/home/ubuntu/HandMadeMarket"
FRONTEND_DIR="$APP_DIR/frontend"
DEPLOY_ROOT="/var/www/handmade-frontend"
RELEASES_DIR="$DEPLOY_ROOT/releases"
RELEASE_DIR="$RELEASES_DIR/$(date +%Y%m%d%H%M%S)"
CURRENT_LINK="$DEPLOY_ROOT/current"

# EC2의 최신 frontend 소스에서 React 운영 번들을 생성함
cd "$FRONTEND_DIR"
npm ci
npm run build

# 새 릴리즈 디렉터리를 먼저 완성해 current 전환 전 깨진 파일 노출을 막음
sudo mkdir -p "$RELEASE_DIR"
sudo cp -r dist/. "$RELEASE_DIR/"

# nginx가 바라보는 current 링크만 교체해 프론트 배포를 빠르게 반영함
sudo ln -sfn "$RELEASE_DIR" "$CURRENT_LINK"

# nginx 설정 검증 후 reload해 React 정적 파일 경로 변경을 적용함
sudo nginx -t
sudo nginx -s reload

# 최근 5개 릴리즈만 남겨 프론트 정적 파일이 EC2 디스크를 계속 차지하지 않게 함
sudo find "$RELEASES_DIR" -mindepth 1 -maxdepth 1 -type d | sort -r | tail -n +6 | sudo xargs -r rm -rf

echo "Frontend deploy complete: $RELEASE_DIR"
