-- =====================================================================
-- V2 staged_document 테이블이 없으면 만든다 - 2026-11-12
-- =====================================================================
-- 왜 필요한가:
--   Flyway 도입 전(ddl-auto:update) 운영 MySQL 에서는 staged_document 생성이 실패해 있었다.
--   extracted_text 가 varchar(20000) 으로 만들어지려다 utf8mb4 varchar 한도(16,383자)를 넘었기 때문이다.
--   ddl-auto:update 는 이 실패를 WARN 로그로만 남기고 기동해서, 문서 임시 올리기 기능이 조용히 깨져 있었다.
--
--   기존 운영 DB 는 V1 을 실행하지 않고 "적용됨"으로만 기록(baseline)하므로, 이 테이블을 여기서 만들어 줘야
--   Hibernate 스키마 검사(ddl-auto:validate)를 통과한다.
--
-- 새 DB(로컬 H2, 빈 MySQL)는 V1 에서 이미 만들었으므로 IF NOT EXISTS 로 아무 일도 하지 않는다.
-- FK 를 CREATE TABLE 안에 함께 적은 것도 같은 이유다 - 표가 이미 있으면 FK 추가도 건너뛰어야 한다.
-- 정의(컬럼·타입·제약 이름)는 V1 의 staged_document 와 똑같이 유지한다.
-- =====================================================================

create table if not exists staged_document (
    company_id bigint not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    size_bytes bigint not null,
    failure_message varchar(500),
    original_file_name varchar(255),
    pdf_file_name varchar(255),
    storage_path varchar(255),
    stored_file_name varchar(255) not null,
    extracted_text text,
    conversion_status enum ('FAILED','PENDING','READY'),
    state enum ('CONSUMED','STAGED') not null,
    text_status enum ('ERROR','NO_TEXT','OK'),
    primary key (id),
    constraint FKmt1sjgsvp8j1a7obx8pyyqgtt foreign key (company_id) references company (id)
) engine=InnoDB;
