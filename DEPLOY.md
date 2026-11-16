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

## 사전 0: EC2 인스턴스 (권장 사양 + 고정 IP)

- **인스턴스 타입**: `t3.small`(2GB) 권장. `t2.micro`(1GB)는 MySQL+JVM 동시 구동 시
  메모리 부족(OOM)으로 SSH까지 먹통될 수 있음 — 부득이 t2.micro면 `ec2-setup.sh`의
  스왑/힙 튜닝(자동 포함)으로 완화.
- **AMI**: Ubuntu 22.04/24.04
- **Elastic IP 필수**: 인스턴스 생성 후 **탄력적 IP(Elastic IP)를 할당·연결**하세요.
  안 붙이면 인스턴스를 **중지(Stop)** 할 때 퍼블릭 IP가 바뀌어 DNS/인증서가 모두 깨집니다.
  (재부팅(Reboot)은 IP 유지, Stop/Start는 EIP 없으면 IP 변경)

> ⚠️ OOM으로 SSH가 막히면 콘솔에서 **재부팅(Reboot)** 으로 복구하세요. **중지(Stop)는 금물**(EIP 없을 때 IP 변경).

## 사전 1: DNS 레코드 (Cafe24 등 도메인 관리 콘솔)

| 타입 | 호스트 | 값 |
| --- | --- | --- |
| **A** | `api.qinnotek` | `<Elastic IP>` |

> ⚠️ 반드시 **A 레코드**로. CNAME에 IP를 넣으면 로컬 `dig`는 되어도 certbot이 NXDOMAIN으로 실패합니다.
> `api` 레코드는 **certbot 인증서 발급 전에 전파 완료**되어 있어야 합니다.
> 확인: `dig api.qinnotek.minisub.store +noall +answer` → `IN A <Elastic IP>`

---

## 1. GitHub Secret / Variable

저장소 → **Settings → Secrets and variables → Actions**

**Secrets** 탭 → New repository secret:
| Name | Value |
| --- | --- |
| `EC2_SSH_KEY` | EC2 접속용 **개인키(.pem) 전체 내용** |

**Variables** 탭 → New repository variable:
| Name | Value |
| --- | --- |
| `EC2_HOST` | EC2 **Elastic IP** (예: `43.202.255.127`) |

> 유저(`ubuntu`)만 워크플로에 하드코딩. IP는 Variable로 관리하므로 인스턴스가 바뀌어도 값만 수정하면 됩니다.

## 2. EC2 보안그룹 인바운드

| 포트 | 소스 | 용도 |
| --- | --- | --- |
| 22 | 0.0.0.0/0 | GitHub Actions 배포 SSH |
| 80 | 0.0.0.0/0 | Let's Encrypt 인증 + HTTP→HTTPS 리다이렉트 |
| 443 | 0.0.0.0/0 | 백엔드 HTTPS |

> `8080`은 nginx가 내부에서만 프록시하므로 **외부 개방 불필요**.

## 3. EC2 세팅 (최초 1회)

```bash
ssh -i your-key.pem ubuntu@43.202.255.127

git clone https://github.com/YuSungyeon/qinnotek-demo.git
cd qinnotek-demo

# (a) Java17 + MySQL + systemd 서비스
bash deploy/ec2-setup.sh              # 출력되는 DB_PASSWORD 보관

# (b) nginx 리버스 프록시 + HTTPS  (DNS A 레코드 전파 후 실행)
API_DOMAIN=api.qinnotek.minisub.store LE_EMAIL=your-email@example.com \
  bash deploy/nginx-setup.sh

# (c) 문서 → PDF 변환기 (워드/한글/엑셀/PPT 를 받으려면 필수)
bash deploy/libreoffice-setup.sh
```

- `ec2-setup.sh`: CORS 기본값이 `https://qinnotek.minisub.store` 로 설정됨 (`/etc/photo-submission.env`)
- `nginx-setup.sh`: `api.qinnotek.minisub.store` 에 대해 nginx 프록시 + certbot 인증서 발급/자동갱신
- `libreoffice-setup.sh`: LibreOffice headless + **한글 폰트** 설치 후 한글 변환 스모크 테스트까지 수행.
  **미설치여도 앱은 정상 동작**하며, 문서는 원본만 저장되고 PDF 사본은 만들어지지 않는다.

## 4. 백엔드 첫 배포

`backend/` 변경을 main에 push하거나, **Actions → Deploy Backend to EC2 → Run workflow** 로 실행.

배포되면 Actions가:
1. 테스트 실행 → 하나라도 실패하면 배포 중단
2. `app.jar` 빌드 → EC2로 scp
3. `/opt/photo-submission/app.jar` 교체 → `systemctl restart photo-submission`

확인:
```bash
curl https://api.qinnotek.minisub.store/actuator/health   # {"status":"UP"}
```

> 운영(prod)에서는 Swagger(`/swagger-ui.html`, `/v3/api-docs`)를 꺼 두었다. 관리자 로그인이 붙기 전까지
> API 구조를 외부에 보여주지 않기 위해서다. API 명세는 로컬 실행(`http://localhost:8080/swagger-ui.html`)에서 본다.
>
> 프론트 도메인이 `https://qinnotek.minisub.store` 가 아니면 `/etc/photo-submission.env` 에 `PUBLIC_URL` 을 넣는다.
> 관리자가 문자로 보내는 제출 링크 주소를 이 값으로 만든다.

## 5. 프론트 (Vercel)

1. Vercel → **Add New → Project** → 이 저장소 import
2. **Root Directory** = `frontend`
3. **Environment Variables**: `VITE_API_BASE = https://api.qinnotek.minisub.store`
4. Deploy 후 **Settings → Domains** 에서 `qinnotek.minisub.store` 커스텀 도메인 연결 (Vercel이 안내하는 DNS 레코드 추가)

> 프론트 도메인을 바꾸면 EC2 `/etc/photo-submission.env` 의 `CORS_ALLOWED_ORIGINS` 도 맞추고
> `sudo systemctl restart photo-submission` 하세요.

---

## 체크리스트

- [ ] DNS A: `api.qinnotek.minisub.store` → `43.202.255.127`
- [ ] GitHub Secret `EC2_SSH_KEY`
- [ ] 보안그룹 22 / 80 / 443
- [ ] `deploy/ec2-setup.sh` 실행 (DB_PASSWORD 보관)
- [ ] DNS 전파 확인 후 `deploy/nginx-setup.sh` 실행 (HTTPS 발급)
- [ ] main push / Run workflow → `https://api.qinnotek.minisub.store/actuator/health` 가 `UP` 인지 확인
- [ ] Vercel 연결 + `VITE_API_BASE` + 커스텀 도메인 `qinnotek.minisub.store`
- [ ] `deploy/libreoffice-setup.sh` 실행 (문서 PDF 변환) — `soffice --version` 과 한글 스모크 테스트 통과 확인

---

## 문서(워드·한글·엑셀·PPT) 제출

고객이 문서를 올리면 서버가 **PDF 사본을 만들어 원본과 함께 보관**하고, 내용을 읽어 요구 항목에
자동 배정한다. 관리자 화면에서는 PDF 를 인라인으로 보고 원본·PDF 를 각각 내려받을 수 있다.

**설정**

| 항목 | 위치 | 비고 |
| --- | --- | --- |
| 항목 유형(사진/문서/무관) | 관리자 → 요청 자료 관리 | '문서'면 고객 화면에 파일 선택기가 뜬다 |
| 문서 분류 키워드 | 같은 화면 | 쉼표 구분. 문서 제목에 들어가는 말이 가장 정확하다 |
| `SOFFICE_BIN` | `/etc/photo-submission.env` | 비우면 자동 탐지 |
| `DOC_CONVERT_CONCURRENCY` | 같은 파일 | soffice 는 인스턴스당 200~400MB. 작은 EC2 에서는 **1 유지** |
| `DOC_CONVERT_TIMEOUT_SEC` | 같은 파일 | 기본 90 |

**로컬 개발**: `brew install --cask libreoffice` (없어도 앱은 뜬다 — 변환만 실패로 강등)

**알려진 제약**
- 스톡 LibreOffice 의 한글 필터는 `writer_MIZI_Hwp_97` 뿐이라 **한글 97(.hwp v3)만** 열린다.
  요즘 쓰는 HWP 5.0 과 `.hwpx` 는 [H2Orestart](https://github.com/ebandal/H2Orestart) 확장을 깔아야 한다
  (`sudo unopkg add --shared H2Orestart.oxt` 후 서비스 재시작). 확장 없이도 원본은 그대로 보관·다운로드된다.
- 스캔 PDF(텍스트 레이어 없음)는 **파일명**으로만 분류한다. 판단이 서지 않으면 고객에게 직접 고르게 한다.
- 원본 + PDF 를 함께 보관하므로 디스크 사용량이 약 2배다. `du -sh /opt/photo-submission/uploads` 로 확인.

---

## 바이러스 검사 · NAS 보관

고객 업로드 → 확장자·크기·파일 시그니처 검사 → **ClamAV 바이러스 검사** → 서버 저장 →
**NAS `{ARCHIVE_DIR}/{기업명}/{요청자료명}.{확장자}` 로 복사**. 기업을 만들면 NAS 에 폴더가 생기고, 이름을 바꾸면 폴더도 바뀐다.

| 환경변수 | 비고 |
| --- | --- |
| `VIRUS_SCAN_ENABLED` | `true` 면 검사. clamd 가 응답하지 않으면 **업로드를 거부**한다(503) — 검사 안 된 파일을 받지 않는다 |
| `CLAMD_HOST` / `CLAMD_PORT` | 기본 `localhost` / `3310` |
| `ARCHIVE_DIR` | 서버에 마운트한 NAS 경로. 비우면 NAS 보관을 끈다 |

**ClamAV (리눅스)**
```bash
sudo apt install -y clamav-daemon clamav-freshclam      # freshclam 이 바이러스 DB 를 자동 갱신
sudo tee -a /etc/clamav/clamd.conf >/dev/null <<'CONF'
TCPSocket 3310
TCPAddr 127.0.0.1
StreamMaxLength 50M
CONF
sudo systemctl restart clamav-daemon
echo PING | nc localhost 3310                            # PONG 이면 정상
```
- `StreamMaxLength` 는 업로드 상한(문서 30MB)보다 커야 한다. 기본 25M 이면 큰 문서가 503 으로 막힌다.
- 윈도우 서버: ClamAV 윈도우판을 설치하고 `clamd.conf` 에 같은 세 줄을 넣은 뒤 `freshclam` 실행, clamd 를 서비스로 등록한다.
- 매크로가 든 오피스 문서까지 막으려면 `clamd.conf` 에 `AlertOLE2Macros yes` (정상 문서도 막힐 수 있음).

**NAS 마운트 (리눅스)** — `/etc/fstab`
```
//NAS주소/공유폴더  /mnt/nas  cifs  credentials=/etc/nas-cred,uid=앱실행계정,iocharset=utf8,soft,_netdev  0 0
```
`ARCHIVE_DIR=/mnt/nas/제출자료`. `soft` 는 NAS 가 꺼졌을 때 파일 작업이 무한정 멈추지 않게 한다.
윈도우 서버는 서비스 계정에 드라이브 문자(Z:)가 보이지 않으므로 `ARCHIVE_DIR=//NAS주소/공유폴더/제출자료` 처럼 UNC 경로로 쓴다.

**동작**
- NAS 복사는 업로드 직후 별도 스레드에서 한다. NAS 가 꺼져 있어도 업로드는 성공하고 로그에 `NAS 보관 실패` 가 남는다(자동 재시도 없음).
- 같은 항목을 다시 올리면 NAS 사본을 덮어쓴다. 기업 삭제·요청 자료 해제 때 NAS 사본은 **지우지 않는다**.
- 기능을 켜기 전에 올라온 파일은 NAS 로 옮겨지지 않는다.

---

## SMS(Solapi) 알림 활성화

고객이 사진을 제출하면 **관리자 설정 페이지(`/admin/settings`)에 저장한 번호**로 알림 문자가 발송됩니다.
(from·to 모두 해당 번호 — Solapi에 **발신번호로 사전 등록**되어 있어야 함)

EC2에서 키를 등록하고 서비스 재기동:
```bash
sudo tee -a /etc/photo-submission.env >/dev/null <<'ENV'
SMS_ENABLED=true
SOLAPI_API_KEY=NCSQTOF2QYQ9TNME
SOLAPI_API_SECRET=발급받은_시크릿
ENV
# (이미 빈 값으로 들어가 있으면 해당 줄을 수정)
sudo systemctl restart photo-submission
```

확인:
- `/admin/settings` 에서 **SMS 연동: 활성화됨** 표시 + 관리자 번호 저장
- 고객이 제출 → 문자 수신, 실패 시 로그: `sudo journalctl -u photo-submission | grep SMS`

> 키/시크릿은 서버 env에만 두고 UI엔 노출하지 않습니다. 발송 실패는 로그로만 남고 제출 흐름은 정상 진행됩니다.

## DB 스키마 변경 (Flyway)

스키마는 `backend/src/main/resources/db/migration/V*.sql` 로만 바꾼다. 앱이 기동할 때 아직 적용하지 않은 파일을
순서대로 실행하고, Hibernate 는 엔티티와 DB 가 맞는지 검사만 한다(`ddl-auto: validate`). 작성 규칙은 같은 폴더의 `README.md`.

- **Flyway 도입 후 첫 배포**: 기존 운영 DB 는 V1(기준선)을 실행하지 않고 "적용됨"으로만 기록한 뒤 V2 부터 실행한다
  (`spring.flyway.baseline-on-migrate`). 기존 데이터는 그대로 남는다.
- **V2** 는 Flyway 도입 전 운영에서 만들어지지 못한 `staged_document` 테이블(문서 임시 올리기)을 만든다.
  이 테이블이 없어서 문서 올리기가 500 으로 실패하던 문제가 이 배포로 고쳐진다.
- 마이그레이션이나 스키마 검사가 실패하면 **앱이 기동하지 않는다.** 배포 워크플로가 헬스체크로 이를 잡아
  배포를 실패로 표시하고 최근 로그를 출력한다. 서버에서 직접 볼 때:

```bash
sudo journalctl -u photo-submission -n 200 --no-pager | grep -iE "flyway|Schema-validation|ERROR"
mysql -u root -p photodb -e "SELECT version, description, success, installed_on FROM flyway_schema_history;"
```

## 헬스체크 · 장애 알림

### 헬스체크 주소

`GET https://api.qinnotek.minisub.store/actuator/health`

| 응답 | 뜻 |
| --- | --- |
| `200` `{"status":"UP", ...}` | 정상 |
| `503` `{"status":"DOWN"}` | 앱은 떠 있지만 아래 중 하나가 실패: DB 연결, 디스크 여유(업로드 폴더 디스크 500MB 미만), 업로드 폴더 쓰기 |
| `502` / `504` (nginx) | 앱 프로세스가 죽었거나 기동 중 |
| 연결 실패 / 타임아웃 | EC2 또는 nginx 가 내려감 |

- 외부에는 상태만 보이고 세부 내용(디스크 용량, DB 종류 등)은 숨긴다. 원인은 서버 로그에 WARN 으로 남는다:
  `sudo journalctl -u photo-submission -n 200 --no-pager | grep -i "health"`
- `/actuator/health/liveness`(프로세스 생존), `/actuator/health/readiness`(요청 받을 준비)도 있다.
- `health` 외의 Actuator 엔드포인트(`env`, `beans` 등)는 웹에 노출하지 않는다.
- 배포 워크플로는 재기동 뒤 이 주소가 200 이 될 때까지 최대 120초 기다린다.

### 장애 알림 설정 (외부 가동 모니터링, 최초 1회)

앱이 죽으면 앱 스스로는 알릴 수 없으므로 **바깥에서 주기적으로 확인하는 서비스**를 쓴다.
아래는 무료인 UptimeRobot 기준이다 (Better Stack, Cronitor 등도 같은 방식).

1. https://uptimerobot.com 가입 → **Add New Monitor**
2. Monitor Type: **HTTP(s)** (또는 응답 본문까지 보려면 **Keyword**, 키워드 `"UP"`, *exists* 일 때 정상)
3. URL: `https://api.qinnotek.minisub.store/actuator/health`
4. Monitoring Interval: **5분** (무료 플랜 최소값)
5. Alert Contacts: 담당자 이메일 / 휴대폰 앱 푸시 / Slack 등 추가
6. (선택) 프론트도 같은 방식으로 `https://qinnotek.minisub.store` 를 추가

알림을 받으면: `sudo systemctl status photo-submission` → `sudo journalctl -u photo-submission -n 200 --no-pager`
순서로 원인을 본다. 앱 로그에서 `ERROR` 는 서버 쪽 문제만 남는다(요청 오류는 4xx·WARN 이하로 분리돼 있음).

## 운영 명령 모음

```bash
sudo systemctl status photo-submission      # 앱 상태
sudo journalctl -u photo-submission -f       # 앱 로그
curl -s localhost:8080/actuator/health       # 헬스체크 (서버 안에서)
sudo systemctl restart photo-submission      # 재기동
sudo nginx -t && sudo systemctl reload nginx # nginx 설정 검증/reload
sudo certbot renew --dry-run                 # 인증서 갱신 테스트
```
