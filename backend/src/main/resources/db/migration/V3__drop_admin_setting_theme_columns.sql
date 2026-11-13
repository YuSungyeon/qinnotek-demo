-- =====================================================================
-- V3 관리자 설정의 디자인/테마 컬럼 삭제 - 2026-11-13
-- =====================================================================
-- 관리자·고객 화면을 design.md 모노크롬 하나로 고정하면서 색상 프리셋/디자인 팩 선택 기능을 없앴다.
-- (frontend theme.js · /api/theme · AdminSetting.designId/themeId/primaryColor 제거와 같은 배포)
--
-- 컬럼마다 한 문장씩 쓴다: 여러 컬럼을 한 번에 지우는 문법은 MySQL 과 H2 가 달라서.
-- 되돌릴 일이 생기면 이 파일을 고치지 말고 새 파일(V4__...)로 컬럼을 다시 추가한다.
-- =====================================================================

alter table admin_setting drop column design_id;
alter table admin_setting drop column theme_id;
alter table admin_setting drop column primary_color;
