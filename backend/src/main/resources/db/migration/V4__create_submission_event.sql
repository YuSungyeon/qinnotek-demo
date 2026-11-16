-- =====================================================================
-- V4 처리 이력(submission_event) - 2026-11-16
-- =====================================================================
-- 기업별로 제출·재제출·통과·반환·반환 안내·제출 링크 발급 같은 일을 시간순으로 쌓는다.
-- 이력은 지우지 않는다(기업을 삭제할 때만 함께 지운다).
--
-- item_id 에는 FK 를 걸지 않는다. 요청 자료 지정을 해제해 제출 항목이 지워져도 이력은 남아야 하므로,
-- 항목 이름(item_name)을 함께 저장해 두고 화면에는 그 이름을 보여준다.
--
-- stored_file_name / pdf_file_name: 그 시점의 제출 파일. 반환한 파일도 지우지 않고 여기서 참조한다
-- (재제출을 검수할 때 이전 반환본과 비교).
-- notify_pending: 반환 안내를 보내야 하는 반환인지. 검수를 다 마치면 모아서 한 번 보내고 지운다.
-- =====================================================================

create table submission_event (
    company_id bigint not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    item_id bigint,
    notify_pending bit not null,
    submit_round integer,
    content_kind enum ('DOCUMENT','PHOTO'),
    event_type enum ('APPROVED','LINK_ISSUED','LINK_REVOKED','LINK_SMS_SENT','NOTICE_FAILED','NOTICE_SENT','RETURNED','SUBMITTED') not null,
    recipient varchar(30),
    item_name varchar(255),
    pdf_file_name varchar(255),
    stored_file_name varchar(255),
    detail varchar(1000),
    reason varchar(1000),
    primary key (id),
    constraint fk_submission_event_company foreign key (company_id) references company (id)
) engine=InnoDB;

create index idx_submission_event_company on submission_event (company_id, id);
create index idx_submission_event_item on submission_event (item_id);
