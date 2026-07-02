# 배포 가이드

- **백엔드**: EC2(Ubuntu, `51.20.74.46`) + 로컬 MySQL, GitHub Actions로 main push 시 자동 배포
- **프론트**: Vercel (GitHub 연동, main push 시 자동 배포)

---

## 1. 백엔드 (EC2)

### 1-1. GitHub Secret 등록 (1개)

GitHub 저장소 → **Settings → Secrets and variables → Actions → New repository secret**

| Name | Value |
| --- | --- |
| `EC2_SSH_KEY` | EC2 접속용 **개인키(.pem) 전체 내용** (`-----BEGIN ... KEY-----` 포함) |

> EC2 IP(`51.20.74.46`)와 유저(`ubuntu`)는 워크플로에 이미 하드코딩되어 있어 별도 secret이 필요 없습니다.

### 1-2. EC2 보안그룹(Security Group) 인바운드

| 포트 | 소스 | 용도 |
| --- | --- | --- |
| 22 (SSH) | 0.0.0.0/0 | GitHub Actions 러너가 배포 시 접속 (러너 IP가 고정되지 않음) |
| 8080 (TCP) | 0.0.0.0/0 | 백엔드 API |

> 22번을 전체 개방하는 것이 부담되면, 배포는 [Tailscale/SSM] 또는 고정 IP 러너를 사용하세요. 데모 기준으로는 22 개방으로 충분합니다.

### 1-3. EC2 최초 1회 세팅

EC2에 SSH 접속 후, 저장소를 clone 하고 세팅 스크립트를 실행합니다.

```bash
ssh -i your-key.pem ubuntu@51.20.74.46

# 저장소 가져오기 (또는 deploy/ 폴더만 업로드)
git clone https://github.com/YuSungyeon/qinnotek-demo.git
cd qinnotek-demo

# Java17 + MySQL 설치, DB/유저/서비스 등록 (Vercel 도메인은 나중에 수정 가능)
CORS_ORIGIN="https://your-frontend.vercel.app" bash deploy/ec2-setup.sh
```

스크립트가 출력하는 **DB_PASSWORD**를 잘 보관하세요. (`/etc/photo-submission.env`에 저장됨)

### 1-4. 배포 실행

`backend/` 변경사항이 main에 push되면 GitHub Actions가 자동으로:
1. `./gradlew bootJar` 로 `app.jar` 빌드
2. EC2로 scp 전송
3. `/opt/photo-submission/app.jar` 교체 후 `systemctl restart photo-submission`

수동 실행: GitHub → **Actions → Deploy Backend to EC2 → Run workflow**

배포 확인:
```bash
ssh -i your-key.pem ubuntu@51.20.74.46
sudo systemctl status photo-submission
curl http://localhost:8080/v3/api-docs   # 또는 http://51.20.74.46:8080/swagger-ui.html
```

---

## 2. 프론트 (Vercel)

1. [vercel.com](https://vercel.com) → **Add New → Project** → 이 GitHub 저장소 import
2. **Root Directory** = `frontend`
3. **Environment Variables** 추가:
   | Key | Value |
   | --- | --- |
   | `VITE_API_BASE` | `http://51.20.74.46:8080` |
4. Deploy. 이후 main push마다 Vercel이 자동 재배포합니다.

> 배포 후 발급된 Vercel 도메인을 EC2의 `/etc/photo-submission.env`
> `CORS_ALLOWED_ORIGINS`에 반영하고 `sudo systemctl restart photo-submission` 하세요.
> (브라우저 CORS 통과에 필요)

> ⚠️ Vercel(HTTPS)에서 EC2(HTTP)를 호출하면 브라우저가 **mixed content**로 차단할 수 있습니다.
> 실제 서비스 시에는 EC2 앞에 nginx + HTTPS(도메인/인증서)를 두거나 ALB를 사용하세요.

---

## 요약 체크리스트

- [ ] GitHub Secret `EC2_SSH_KEY` 등록
- [ ] EC2 보안그룹 22, 8080 오픈
- [ ] EC2에서 `deploy/ec2-setup.sh` 1회 실행
- [ ] main push → Actions 배포 확인 (`systemctl status`)
- [ ] Vercel 프로젝트 연결 + `VITE_API_BASE` 설정
- [ ] Vercel 도메인을 EC2 `CORS_ALLOWED_ORIGINS`에 반영
