# 배포 가이드

| 구성 | 주소 | 인프라 |
| --- | --- | --- |
| 프론트 | `https://qinnotek.minisub.store` | Vercel (커스텀 도메인) |
| 백엔드 | `https://api.qinnotek.minisub.store` | EC2 nginx(443) → 내부 Spring Boot(8080) |
| DB | localhost:3306 | EC2 로컬 MySQL |

- 백엔드는 main push(backend 변경) 시 GitHub Actions가 EC2로 자동 배포
- 프론트는 Vercel이 main push 시 자동 배포
- **HTTPS↔HTTPS** 구조라 mixed-content 문제 없음

---

## 사전: DNS 레코드 (도메인 제공업체에서)

| 타입 | 이름 | 값 |
| --- | --- | --- |
| A | `api.qinnotek.minisub.store` | `51.20.74.46` |

> 프론트(`qinnotek.minisub.store`)는 Vercel 안내에 따라 CNAME/A로 연결합니다(아래 4번).
> `api` 레코드는 **certbot 인증서 발급 전에 전파 완료**되어 있어야 합니다. (`dig api.qinnotek.minisub.store` 로 확인)

---

## 1. GitHub Secret (1개)

저장소 → **Settings → Secrets and variables → Actions → New repository secret**

| Name | Value |
| --- | --- |
| `EC2_SSH_KEY` | EC2 접속용 **개인키(.pem) 전체 내용** |

> IP(`51.20.74.46`), 유저(`ubuntu`)는 워크플로에 하드코딩되어 있어 별도 secret 불필요.

## 2. EC2 보안그룹 인바운드

| 포트 | 소스 | 용도 |
| --- | --- | --- |
| 22 | 0.0.0.0/0 | GitHub Actions 배포 SSH |
| 80 | 0.0.0.0/0 | Let's Encrypt 인증 + HTTP→HTTPS 리다이렉트 |
| 443 | 0.0.0.0/0 | 백엔드 HTTPS |

> `8080`은 nginx가 내부에서만 프록시하므로 **외부 개방 불필요**.

## 3. EC2 세팅 (최초 1회)

```bash
ssh -i your-key.pem ubuntu@51.20.74.46

git clone https://github.com/YuSungyeon/qinnotek-demo.git
cd qinnotek-demo

# (a) Java17 + MySQL + systemd 서비스
bash deploy/ec2-setup.sh              # 출력되는 DB_PASSWORD 보관

# (b) nginx 리버스 프록시 + HTTPS  (DNS A 레코드 전파 후 실행)
API_DOMAIN=api.qinnotek.minisub.store LE_EMAIL=your-email@example.com \
  bash deploy/nginx-setup.sh
```

- `ec2-setup.sh`: CORS 기본값이 `https://qinnotek.minisub.store` 로 설정됨 (`/etc/photo-submission.env`)
- `nginx-setup.sh`: `api.qinnotek.minisub.store` 에 대해 nginx 프록시 + certbot 인증서 발급/자동갱신

## 4. 백엔드 첫 배포

`backend/` 변경을 main에 push하거나, **Actions → Deploy Backend to EC2 → Run workflow** 로 실행.

배포되면 Actions가:
1. `app.jar` 빌드 → EC2로 scp
2. `/opt/photo-submission/app.jar` 교체 → `systemctl restart photo-submission`

확인:
```bash
curl https://api.qinnotek.minisub.store/v3/api-docs
# 브라우저: https://api.qinnotek.minisub.store/swagger-ui.html
```

## 5. 프론트 (Vercel)

1. Vercel → **Add New → Project** → 이 저장소 import
2. **Root Directory** = `frontend`
3. **Environment Variables**: `VITE_API_BASE = https://api.qinnotek.minisub.store`
4. Deploy 후 **Settings → Domains** 에서 `qinnotek.minisub.store` 커스텀 도메인 연결 (Vercel이 안내하는 DNS 레코드 추가)

> 프론트 도메인을 바꾸면 EC2 `/etc/photo-submission.env` 의 `CORS_ALLOWED_ORIGINS` 도 맞추고
> `sudo systemctl restart photo-submission` 하세요.

---

## 체크리스트

- [ ] DNS A: `api.qinnotek.minisub.store` → `51.20.74.46`
- [ ] GitHub Secret `EC2_SSH_KEY`
- [ ] 보안그룹 22 / 80 / 443
- [ ] `deploy/ec2-setup.sh` 실행 (DB_PASSWORD 보관)
- [ ] DNS 전파 확인 후 `deploy/nginx-setup.sh` 실행 (HTTPS 발급)
- [ ] main push / Run workflow → `https://api.qinnotek.minisub.store/swagger-ui.html` 확인
- [ ] Vercel 연결 + `VITE_API_BASE` + 커스텀 도메인 `qinnotek.minisub.store`

---

## 운영 명령 모음

```bash
sudo systemctl status photo-submission      # 앱 상태
sudo journalctl -u photo-submission -f       # 앱 로그
sudo systemctl restart photo-submission      # 재기동
sudo nginx -t && sudo systemctl reload nginx # nginx 설정 검증/reload
sudo certbot renew --dry-run                 # 인증서 갱신 테스트
```
