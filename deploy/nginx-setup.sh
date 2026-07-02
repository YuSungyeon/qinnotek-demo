#!/usr/bin/env bash
#
# EC2(Ubuntu)에 nginx 리버스 프록시 + Let's Encrypt HTTPS 설정.
# 백엔드(localhost:8080)를 https://api.qinnotek.minisub.store 로 노출한다.
#
# 사전 조건:
#   - DNS A 레코드: api.qinnotek.minisub.store  ->  51.20.74.46  (전파 완료)
#   - 보안그룹 인바운드 80, 443 개방
#
# 사용법:
#   API_DOMAIN=api.qinnotek.minisub.store LE_EMAIL=you@example.com bash deploy/nginx-setup.sh
#
set -euo pipefail

API_DOMAIN="${API_DOMAIN:-api.qinnotek.minisub.store}"
LE_EMAIL="${LE_EMAIL:-}"

echo "==> 1) nginx, certbot 설치"
sudo apt-get update -y
sudo apt-get install -y nginx certbot python3-certbot-nginx

echo "==> 2) 리버스 프록시 사이트 설정 (${API_DOMAIN})"
sudo tee /etc/nginx/sites-available/photo-submission >/dev/null <<NGINX
server {
    listen 80;
    server_name ${API_DOMAIN};

    # 이미지 업로드 대비 (백엔드 multipart 최대 20MB)
    client_max_body_size 25M;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host              \$host;
        proxy_set_header X-Real-IP         \$remote_addr;
        proxy_set_header X-Forwarded-For   \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINX

sudo ln -sf /etc/nginx/sites-available/photo-submission /etc/nginx/sites-enabled/photo-submission
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx

echo "==> 3) HTTPS 인증서 발급 (Let's Encrypt)"
if [ -n "${LE_EMAIL}" ]; then
  sudo certbot --nginx -d "${API_DOMAIN}" --non-interactive --agree-tos -m "${LE_EMAIL}" --redirect
  echo ""
  echo "완료! https://${API_DOMAIN}/swagger-ui.html 로 확인하세요."
else
  echo ""
  echo "LE_EMAIL 미설정 → 아래를 수동 실행하세요:"
  echo "  sudo certbot --nginx -d ${API_DOMAIN} --redirect"
fi

echo ""
echo "certbot 자동 갱신 타이머 상태:"
sudo systemctl status certbot.timer --no-pager 2>/dev/null | head -3 || true
