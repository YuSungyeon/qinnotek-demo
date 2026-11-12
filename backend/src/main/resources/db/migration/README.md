# DB 마이그레이션 (Flyway)

스키마는 이 폴더의 SQL 파일로만 바꾼다. Hibernate 는 스키마를 만들거나 고치지 않고,
엔티티와 DB 가 맞는지 **검사만** 한다 (`spring.jpa.hibernate.ddl-auto: validate`).

## 동작

앱이 기동할 때 Flyway 가 JPA 보다 먼저 실행된다.

1. `flyway_schema_history` 테이블에서 이미 적용한 버전을 확인한다.
2. 아직 적용하지 않은 `V{n}__*.sql` 을 번호 순서대로 실행한다.
3. 그다음 Hibernate 가 엔티티와 스키마가 맞는지 검사한다. 맞지 않으면 **앱이 기동하지 않는다**.
   그래서 엔티티와 스키마가 어긋난 채로 배포되는 일이 없다.

| DB | 처음 기동할 때 |
| --- | --- |
| 로컬 H2 (기본 프로필) | 매번 빈 DB → V1 부터 전부 실행 |
| 기존 운영 MySQL (Flyway 도입 전 테이블 있음) | `baseline-on-migrate` 로 "V1 적용됨"으로만 기록하고 V2 부터 실행 |
| 새 운영 MySQL (빈 DB) | V1 부터 전부 실행 |

## 스키마를 바꾸는 순서

1. 엔티티를 고친다.
2. 같은 변경을 SQL 로 쓴 새 파일을 추가한다: `V2__add_submission_history.sql`, `V3__...`
   - 번호는 1씩 올린다. 이름은 영문 소문자와 밑줄(`__` 두 개 뒤에 설명)로 쓴다.
   - SQL 은 **MySQL 문법**으로 쓴다. 로컬 H2 는 MySQL 호환 모드라 대부분 그대로 실행된다.
     H2 가 지원하지 않는 문법(예: `ALTER TABLE ... MODIFY` 의 일부 형태)은 피하고, 막히면 둘 다 되는 문법으로 바꾼다.
   - 컬럼 타입은 Hibernate 가 기대하는 타입과 같아야 한다. 헷갈리면 아래 "엔티티 기준 DDL 확인"으로 확인한다.
3. 로컬에서 `./gradlew test` 와 `./gradlew bootRun` 이 통과하는지 본다. (V1 부터 새로 적용하고 validate 까지 거친다)
4. 배포한다. 운영에서는 새 파일만 실행된다.

## 하지 말 것

- **이미 적용된 파일을 고치거나 지우지 않는다.** Flyway 가 체크섬을 기록해 두므로 기동이 막힌다.
  잘못 만든 변경은 새 파일로 되돌린다.
- **`ddl-auto` 를 다시 `update` 로 돌리지 않는다.** 마이그레이션 파일 없이 스키마가 바뀌면,
  다른 환경(새 서버, 로컬)과 운영 DB 가 조용히 어긋난다.

## 예: 테이블 이름 바꾸기

`photo_requirement` 는 사진만 받던 시절 이름이다. 바꾸려면 아래 둘을 **한 배포에** 넣는다.

```sql
-- V2__rename_photo_requirement.sql
RENAME TABLE photo_requirement TO requirement;
```

```java
@Table(name = "requirement") // Requirement.java
```

## 엔티티 기준 DDL 확인

엔티티에서 Hibernate 가 기대하는 DDL 을 파일로 뽑아 볼 수 있다 (DB 에는 아무것도 하지 않는다).

```bash
./gradlew bootJar
java -jar build/libs/app.jar --spring.main.web-application-type=none \
  --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=none \
  --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=build/schema.sql \
  "--spring.jpa.properties.hibernate.hbm2ddl.delimiter=;"
```

`build/schema.sql` 이 생기면 Ctrl+C 로 멈춘다. (V1 은 이 방법으로 만들었다)
