#!/usr/bin/env bash
#
# EC2(Ubuntu) 최초 1회 세팅 스크립트.
# Java 17 + MySQL 설치, DB/유저 생성, 앱 디렉터리/서비스 등록까지 수행한다.
#
# 사용법 (EC2에 SSH 접속 후, 이 저장소를 clone 하거나 deploy/ 폴더를 업로드한 뒤):
#   cd qinnotek-demo
#   CORS_ORIGIN="https://<your-app>.vercel.app" bash deploy/ec2-setup.sh
#
# 환경변수:
#   DB_PASSWORD  - 미지정 시 임의 생성 후 출력
#   CORS_ORIGIN  - Vercel 프론트 도메인 (기본값: * 대신 임시 placeholder)
#
set -euo pipefail

APP_DIR=/opt/photo-submission
ENV_FILE=/etc/photo-submission.env
DB_NAME=photodb
DB_USER=photo
DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -hex 12)}"
CORS_ORIGIN="${CORS_ORIGIN:-https://your-frontend.vercel.app}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> 1) 패키지 설치 (Java 17, MySQL)"
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jre-headless mysql-server

echo "==> 2) MySQL 시작 및 DB/유저 생성"
sudo systemctl enable --now mysql
sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

echo "==> 3) 앱 디렉터리 및 업로드 폴더 생성"
sudo mkdir -p "${APP_DIR}/uploads"
sudo chown -R ubuntu:ubuntu "${APP_DIR}"

echo "==> 4) 환경변수 파일 생성 (${ENV_FILE})"
sudo tee "${ENV_FILE}" >/dev/null <<ENV
DB_HOST=localhost
DB_PORT=3306
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
UPLOAD_DIR=${APP_DIR}/uploads
CORS_ALLOWED_ORIGINS=${CORS_ORIGIN}
ENV
sudo chmod 600 "${ENV_FILE}"

echo "==> 5) systemd 서비스 등록"
sudo cp "${SCRIPT_DIR}/photo-submission.service" /etc/systemd/system/photo-submission.service
sudo systemctl daemon-reload
sudo systemctl enable photo-submission

echo ""
echo "=========================================================="
echo " 세팅 완료!"
echo "  - DB:        ${DB_NAME} / user=${DB_USER}"
echo "  - DB_PASSWORD: ${DB_PASSWORD}"
echo "  - CORS origin: ${CORS_ORIGIN}"
echo "  - 서비스:    photo-submission (아직 jar 없음 → 첫 배포 후 자동 기동)"
echo ""
echo " 다음 단계:"
echo "  1) 필요 시 ${ENV_FILE} 의 CORS_ALLOWED_ORIGINS 를 실제 Vercel 도메인으로 수정"
echo "  2) GitHub main 브랜치에 push 하면 Actions가 jar를 배포/기동합니다."
echo "  3) 상태 확인: sudo systemctl status photo-submission"
echo "=========================================================="
