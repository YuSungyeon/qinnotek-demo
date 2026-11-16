# 자료 제출 및 검수 시스템

기업별로 필요한 자료(사진·문서·기타 파일)를 고객에게 요청하고, 고객은 휴대폰에서 자료를 제출하며,
관리자는 제출된 자료를 검수(통과/반환)하는 시스템입니다.

- **고객 사이트**: 관리자가 보낸 제출 링크(`/s/{토큰}`)로 접속 → 자료 올리기 → 제출
- **관리자 페이지**: 기업/요청 자료 관리, 제출 링크 발급(복사·문자 발송·재발급·끄기), 요청 자료 지정, 제출 자료 검수

## 용어

화면·코드·DB 가 같은 개념을 같은 이름으로 부르도록 아래 표를 기준으로 한다.
새 코드에서 '사진(photo)'은 **실제로 이미지일 때만** 쓴다 (카메라 촬영, 이미지 압축, CLIP 분류 등).
종류와 상관없는 제출물은 '자료'(화면) / `file`(코드)로 부른다.

| 화면 | 코드 | DB 테이블 | 뜻 |
| --- | --- | --- | --- |
| 요청 자료 | `Requirement` | `photo_requirement` ※ | 관리자가 정의하는 "무엇을 받을지" (마스터). 명칭·설명·받는 파일 유형·예시 이미지 |
| 받는 파일 유형 (사진/문서/무관) | `RequirementFileType` (`PHOTO`/`DOCUMENT`/`ANY`) | 컬럼 `file_type` | 요청 자료가 허용하는 입력 |
| 제출 항목 | `SubmissionItem` | `submission_item` | 기업 × 요청 자료 한 칸. 상태와 최신 제출 파일 1개를 가진다 |
| 올린 파일 종류 | `ContentKind` (`PHOTO`/`DOCUMENT`) | 컬럼 `content_kind` | 제출 항목에 실제로 들어온 파일이 사진인지 문서인지 |
| 임시 문서 | `StagedDocument` | `staged_document` | 분류를 위해 먼저 올려둔 문서. 전송하면 제출 항목에 확정된다 |
| 제출 링크 | `Company.accessToken` | 컬럼 `access_token` | 고객이 접속하는 `/s/{토큰}` |
| 제출 자료 검수 | `AdminSubmissionService` | — | 통과 / 반환(사유 필수) |

※ `photo_requirement` 는 사진만 받던 시절의 테이블명이다. 바꾸려면 Flyway 마이그레이션
(`RENAME TABLE photo_requirement TO requirement`)과 `@Table` 변경을 한 배포에 함께 넣는다
(`backend/src/main/resources/db/migration/README.md`). 서버의 서비스·경로 이름(`photo-submission`)은 서버 설정과 함께 바꿔야 해서 유지한다.

## 기술 스택

| 구분 | 스택 |
| --- | --- |
| 백엔드 | Spring Boot 3.3, Java 17, Spring Data JPA, MySQL(운영) / H2(로컬), Flyway(스키마 마이그레이션), Actuator(헬스체크), springdoc-openapi(Swagger) |
| 프론트 | Vue 3, Vite, Vue Router, Axios |
| 파일 저장 | EC2 로컬 파일시스템 (UUID 파일명) |
| 배포 | 프론트 → Vercel, 백엔드 → EC2 |

## 프로젝트 구조

```
qinnotek-demo/
├── backend/                 # Spring Boot API
│   └── src/main/java/com/qinnotek/submission/
│       ├── config/          # WebConfig(CORS), OpenApiConfig(Swagger), AppProperties, DataInitializer
│       ├── domain/          # Company, Requirement, SubmissionItem, StagedDocument, 상태 enum
│       ├── dto/             # customer/, admin/ 요청·응답 DTO
│       ├── repository/      # JPA repository
│       ├── service/         # 비즈니스 로직 (Customer, Company, Requirement, Submission, File, Sms)
│       ├── controller/      # REST 컨트롤러
│       └── exception/       # 전역 예외 처리
└── frontend/                # Vue 앱 ( / 고객, /admin 관리자 )
    └── src/
        ├── api/             # axios client, customer/admin API
        ├── router/          # 라우팅 (고객/관리자 분리)
        ├── components/      # SubmissionItemCard, DocPreview
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
- 헬스체크: http://localhost:8080/actuator/health
- DB 스키마는 기동 시 Flyway 가 `db/migration/V*.sql` 로 만든다 (Hibernate 는 검사만 함)
- OpenAPI 문서: http://localhost:8080/v3/api-docs
- 데모 데이터 자동 생성 — 기업별 제출 링크가 기동 로그에 출력됨 (`초기 데이터 생성 완료 - 제출 링크: …`)

### 2. 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

- 고객: http://localhost:5173/s/{토큰} (관리자 기업 상세 > 제출 링크에서 복사)
- 관리자: http://localhost:5173/admin
- 개발 모드에서는 Vite 프록시(`/api` → `localhost:8080`)를 사용합니다.

## 상태 흐름

**제출 항목**: `PENDING`(미제출) → `SUBMITTED`(검수 대기) → `APPROVED`(통과) / `RETURNED`(반환)

**기업 종합 상태**(관리자 목록, 색상 구분):
`요청 없음` · `제출 대기` · `검수 중` · `일부 반환` · `완료`

주요 규칙:
- 항목당 파일 1개, 항상 최신 제출본만 유지(UUID 저장, 원본/경로/일시 DB 기록)
- 반환 시 반환 사유 필수, 기존 파일 삭제, 고객은 반환 항목만 재제출
- 부분 제출: 일부만 채우고 전송하면 먼저 보낼지 묻고(기본은 빈 항목 채우기), 채운 항목만 검수 대기로 넘긴다.
  낼 항목(미제출·반환)이 남아 있으면 검수 중이어도 고객 화면을 잠그지 않는다
- 제출할 때마다 관리자에게 알림 문자 발송 — 이번에 낸 건수 포함(`SmsService` — Solapi, 관리자 설정에서 켜고 끔)

## 배포

| 구성 | 주소 | 인프라 |
| --- | --- | --- |
| 프론트 | `https://qinnotek.minisub.store` | Vercel (main push 자동배포) |
| 백엔드 | `https://api.qinnotek.minisub.store` | EC2 nginx(443, HTTPS) → Spring Boot(8080), main push 시 GitHub Actions 자동배포 |
| DB | EC2 로컬 MySQL | — |

- CI/CD: `.github/workflows/deploy-backend.yml` (backend 변경 push → EC2 배포)
- EC2 세팅 스크립트: `deploy/ec2-setup.sh`(Java·MySQL·systemd), `deploy/nginx-setup.sh`(nginx·HTTPS)
- **상세 절차·체크리스트는 [DEPLOY.md](DEPLOY.md) 참고**

## 주요 API

전체 명세는 로컬 실행 시 Swagger UI에서 확인할 수 있습니다. (운영에서는 관리자 로그인이 붙기 전까지 꺼 둔다)

| 구분 | 메서드 | 경로 | 설명 |
| --- | --- | --- | --- |
| 고객 | GET | `/api/customer/links/{token}` | 제출 링크 열기 (기업·제출 상태·항목). 없는 링크 404, 기간 지난 링크 410 |
| 고객 | POST | `/api/customer/links/{token}/items/{itemId}/photo` | 사진 올리기 (이미지 전용) |
| 고객 | POST/GET/DELETE | `/api/customer/links/{token}/documents` | 문서 임시 올리기·목록·취소 (자동 분류용) |
| 고객 | POST | `/api/customer/links/{token}/items/{itemId}/attach-document` | 임시 문서를 제출 항목에 확정 |
| 고객 | POST | `/api/customer/links/{token}/submit` | 제출 — `{itemIds}` 로 고른 항목만 (일부 먼저 제출 가능) |
| 관리자 | GET | `/api/admin/companies` | 기업 목록/검색(상태 포함) |
| 관리자 | POST | `/api/admin/companies` | 기업 등록 |
| 관리자 | POST/DELETE | `/api/admin/companies/{id}/link` | 제출 링크 발급·재발급 / 끄기 |
| 관리자 | POST | `/api/admin/companies/{id}/link/sms` | 제출 링크 문자 발송 |
| 관리자 | PUT | `/api/admin/companies/{id}/requirements` | 요청 자료 지정 |
| 관리자 | GET/POST/PUT/DELETE | `/api/admin/requirements` | 요청 자료(마스터) 관리 |
| 관리자 | GET | `/api/admin/companies/{id}/submissions` | 제출 자료 조회 |
| 관리자 | POST | `/api/admin/submissions/{itemId}/approve` | 통과 |
| 관리자 | POST | `/api/admin/submissions/{itemId}/return` | 반환(사유 필수) |
| 공통 | GET | `/api/files/{fileName}` | 업로드 파일 제공 |
