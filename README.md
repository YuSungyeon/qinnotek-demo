# 사진 제출 및 검수 시스템

기업별로 필요한 사진을 고객에게 요청하고, 고객은 휴대폰에서 사진을 제출하며,
관리자는 제출된 사진을 검수(통과/반환)하는 시스템입니다.

- **고객 사이트**: 전화번호로 기업 조회 → 사진 업로드 → 제출
- **관리자 페이지**: 기업/요구사진 관리, 전화번호 등록, 필요 사진 지정, 제출 사진 검수

## 기술 스택

| 구분 | 스택 |
| --- | --- |
| 백엔드 | Spring Boot 3.3, Java 17, Spring Data JPA, MySQL(운영) / H2(로컬), springdoc-openapi(Swagger) |
| 프론트 | Vue 3, Vite, Vue Router, Axios |
| 파일 저장 | EC2 로컬 파일시스템 (UUID 파일명) |
| 배포 | 프론트 → Vercel, 백엔드 → EC2 |

## 프로젝트 구조

```
qinnotek-demo/
├── backend/                 # Spring Boot API
│   └── src/main/java/com/qinnotek/photo/
│       ├── config/          # WebConfig(CORS), OpenApiConfig(Swagger), AppProperties, DataInitializer
│       ├── domain/          # Company, PhotoRequirement, SubmissionItem, 상태 enum
│       ├── dto/             # customer/, admin/ 요청·응답 DTO
│       ├── repository/      # JPA repository
│       ├── service/         # 비즈니스 로직 (Customer, Company, Requirement, Submission, File, Sms)
│       ├── controller/      # REST 컨트롤러
│       └── exception/       # 전역 예외 처리
└── frontend/                # Vue 앱 ( / 고객, /admin 관리자 )
    └── src/
        ├── api/             # axios client, customer/admin API
        ├── router/          # 라우팅 (고객/관리자 분리)
        ├── components/      # PhotoUploadCard
        └── views/           # customer/, admin/
```

## 로컬 실행

### 1. 백엔드 (기본: H2 인메모리, 별도 DB 설정 불필요)

```bash
cd backend
./gradlew bootRun
```

- API: http://localhost:8080
- **Swagger UI: http://localhost:8080/swagger-ui.html**
- OpenAPI 문서: http://localhost:8080/v3/api-docs
- 데모 데이터 자동 생성 — 조회 테스트 전화번호: `01012345678`, `01055556666`

### 2. 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

- 고객: http://localhost:5173/
- 관리자: http://localhost:5173/admin
- 개발 모드에서는 Vite 프록시(`/api` → `localhost:8080`)를 사용합니다.

## 상태 흐름

**개별 사진 항목**: `PENDING`(미제출) → `SUBMITTED`(검수 대기) → `APPROVED`(통과) / `RETURNED`(반환)

**기업 종합 상태**(관리자 목록, 색상 구분):
`요청 없음` · `제출 대기` · `검수 중` · `일부 반환` · `완료`

주요 규칙:
- 항목당 사진 1장, 항상 최신 제출본만 유지(UUID 저장, 원본/경로/일시 DB 기록)
- 반환 시 반환 사유 필수, 기존 사진 삭제, 고객은 반환 항목만 재제출
- 제출 완료 시 관리자에게 알림 문자 발송(`SmsService` — 실제 API는 별도 연동, 현재 로그 스텁)

## 배포

### 백엔드 (EC2 + MySQL)

1. MySQL 준비 후 스키마 생성:
   ```sql
   CREATE DATABASE photodb CHARACTER SET utf8mb4;
   ```
2. 빌드:
   ```bash
   cd backend
   ./gradlew clean bootJar    # build/libs/photo-submission-0.0.1-SNAPSHOT.jar
   ```
3. EC2에서 `prod` 프로필로 실행 (환경변수로 DB/업로드 경로/CORS 지정):
   ```bash
   DB_HOST=... DB_NAME=photodb DB_USERNAME=... DB_PASSWORD=... \
   UPLOAD_DIR=/var/app/uploads \
   CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app \
   java -jar photo-submission-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```
4. 보안그룹에서 8080 포트 오픈(또는 nginx 리버스 프록시 + HTTPS 권장).

### 프론트엔드 (Vercel)

1. Vercel 프로젝트의 Root Directory를 `frontend`로 지정.
2. 환경변수 `VITE_API_BASE`에 EC2 백엔드 주소 설정 (예: `https://api.example.com`).
3. Build Command `npm run build`, Output Directory `dist` (기본값). `vercel.json`에 SPA 라우팅 rewrite 포함.

> ⚠️ 백엔드 `CORS_ALLOWED_ORIGINS`에 Vercel 도메인을 반드시 추가해야 합니다.

## 주요 API

전체 명세는 Swagger UI에서 확인할 수 있습니다.

| 구분 | 메서드 | 경로 | 설명 |
| --- | --- | --- | --- |
| 고객 | POST | `/api/customer/lookup` | 전화번호로 기업/제출상태 조회 |
| 고객 | POST | `/api/customer/items/{itemId}/photo` | 개별 사진 업로드 |
| 고객 | POST | `/api/customer/submit` | 최종 제출 |
| 관리자 | GET | `/api/admin/companies` | 기업 목록/검색(상태 포함) |
| 관리자 | POST | `/api/admin/companies` | 기업 등록 |
| 관리자 | PUT | `/api/admin/companies/{id}/phone` | 전화번호 등록/수정 |
| 관리자 | PUT | `/api/admin/companies/{id}/requirements` | 필요 사진 지정 |
| 관리자 | GET/POST/PUT/DELETE | `/api/admin/requirements` | 요구 사진(마스터) 관리 |
| 관리자 | GET | `/api/admin/companies/{id}/submissions` | 제출 사진 조회 |
| 관리자 | POST | `/api/admin/submissions/{itemId}/approve` | 통과 |
| 관리자 | POST | `/api/admin/submissions/{itemId}/return` | 반환(사유 필수) |
| 공통 | GET | `/api/files/{fileName}` | 업로드 이미지 제공 |
