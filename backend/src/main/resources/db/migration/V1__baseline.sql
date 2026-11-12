-- =====================================================================
-- V1 기준선(baseline) 스키마 - 2026-11-12
-- =====================================================================
-- Flyway 를 도입하기 직전의 스키마를 그대로 옮긴 것이다.
-- (지금까지는 Hibernate ddl-auto:update 가 테이블을 만들고 컬럼을 추가해 왔다)
--
-- 이 파일이 실행되는 경우와 안 되는 경우:
--   * 새 DB(로컬 H2, 새 운영 서버)  → 이 파일로 테이블을 만든다.
--   * 이미 테이블이 있는 기존 운영 DB → 실행하지 않는다.
--     application.yml(prod)의 spring.flyway.baseline-on-migrate=true 로
--     "이미 V1 상태"라고 기록만 하고(flyway_schema_history), V2 부터 적용한다.
--
-- 작성 원칙:
--   * 내용은 Hibernate 가 MySQL 방언으로 생성하는 DDL 과 같다. 엔티티와 1:1 로 맞아야
--     spring.jpa.hibernate.ddl-auto=validate 가 통과한다.
--   * 제약 이름(UK..., FK...)은 Hibernate 가 테이블·컬럼명으로 만드는 고정 해시값이다.
--     기존 운영 DB 에도 같은 이름으로 만들어져 있으므로, 이후 마이그레이션에서
--     제약을 지울 때 이름이 어긋나지 않도록 일부러 읽기 쉬운 이름으로 바꾸지 않았다.
--   * 로컬 H2 는 MODE=MySQL 이라 이 MySQL 문법(enum, datetime(6), engine=InnoDB)을 그대로 실행한다.
--
-- ⚠ 이 파일은 이미 적용된 파일이다. 절대 수정하지 말 것.
--   Flyway 는 적용된 파일의 체크섬을 기록하므로, 고치면 기동 시 검증 오류로 앱이 뜨지 않는다.
--   스키마 변경은 새 파일(V2__설명.sql, V3__...)로 추가한다. 작성 규칙은 db/migration/README.md 참고.
-- =====================================================================


create table admin_setting (
    sms_enabled bit not null,
    id bigint not null auto_increment,
    design_id varchar(20),
    primary_color varchar(20),
    theme_id varchar(20),
    primary key (id)
) engine=InnoDB;

create table company (
    id bigint not null auto_increment,
    link_expires_at datetime(6),
    link_issued_at datetime(6),
    link_opened_at datetime(6),
    access_token varchar(40),
    name varchar(255) not null,
    phone_number varchar(255),
    primary key (id)
) engine=InnoDB;

create table company_manager (
    company_id bigint not null,
    id bigint not null auto_increment,
    manager_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table manager (
    id bigint not null auto_increment,
    phone_number varchar(20) not null,
    name varchar(255) not null,
    position varchar(255),
    primary key (id)
) engine=InnoDB;

create table photo_requirement (
    id bigint not null auto_increment,
    classification_hint varchar(200),
    document_keywords varchar(500),
    description varchar(1000),
    example_image_file_name varchar(255),
    name varchar(255) not null,
    file_type enum ('ANY','DOCUMENT','PHOTO'),
    primary key (id)
) engine=InnoDB;

create table staged_document (
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
    primary key (id)
) engine=InnoDB;

create table submission_item (
    company_id bigint not null,
    id bigint not null auto_increment,
    requirement_id bigint not null,
    uploaded_at datetime(6),
    admin_note varchar(1000),
    reject_reason varchar(1000),
    original_file_name varchar(255),
    pdf_file_name varchar(255),
    storage_path varchar(255),
    stored_file_name varchar(255),
    content_kind enum ('DOCUMENT','PHOTO'),
    pdf_status enum ('FAILED','PENDING','READY'),
    status enum ('APPROVED','PENDING','RETURNED','SUBMITTED') not null,
    primary key (id)
) engine=InnoDB;

alter table company 
   add constraint UK650bu0wckpmyau3720kxhr7kt unique (access_token);

alter table company 
   add constraint UKniu8sfil2gxywcru9ah3r4ec5 unique (name);

alter table company 
   add constraint UKb4dc5sccjogsj2l715gs8te9d unique (phone_number);

alter table company_manager 
   add constraint UKayjc1u3ib9jvn2ffyfapknk75 unique (company_id, manager_id);

alter table photo_requirement 
   add constraint UK97e0jcd7188mi73omh48uyg4o unique (name);

alter table submission_item 
   add constraint UKt73n71du15wtdox2ryxwdf82e unique (company_id, requirement_id);

alter table company_manager 
   add constraint FK6limn7kpsbnrp46u23k0s2cba 
   foreign key (company_id) 
   references company (id);

alter table company_manager 
   add constraint FKk0lrhah98cwvn4yhbx62lnorl 
   foreign key (manager_id) 
   references manager (id);

alter table staged_document 
   add constraint FKmt1sjgsvp8j1a7obx8pyyqgtt 
   foreign key (company_id) 
   references company (id);

alter table submission_item 
   add constraint FK9v41anl4brbuqll099wpo8vpt 
   foreign key (company_id) 
   references company (id);

alter table submission_item 
   add constraint FK5kv8esl1tsy2ek1o5t5gk0s22 
   foreign key (requirement_id) 
   references photo_requirement (id);
