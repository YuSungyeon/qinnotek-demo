// 상태 키 → 칩 클래스. 상태 색(모양)은 이 파일 한 곳에서만 정한다.
// 서버는 상태 키(status)와 라벨(statusLabel)만 내려주고 색은 모른다.
// 칩 클래스의 의미와 모양은 style.css "상태 칩" 주석 참고:
//   chip-review   = 관리자가 지금 결정할 것 (파랑)
//   chip-returned = 고객이 다시 해야 할 것
//   chip-done     = 끝난 것
//   chip-wait     = 기다리는 중
//   chip-none     = 해당 없음

/** 기업 종합 상태 (backend CompanyStatus) */
const COMPANY_CHIP = {
  NONE: 'chip-none',
  SUBMIT_WAITING: 'chip-wait',
  UNDER_REVIEW: 'chip-review',
  PARTIALLY_RETURNED: 'chip-returned',
  COMPLETED: 'chip-done'
}

/** 제출 항목 상태 (backend SubmissionStatus) */
const ITEM_CHIP = {
  PENDING: 'chip-wait',
  SUBMITTED: 'chip-review',
  RETURNED: 'chip-returned',
  APPROVED: 'chip-done'
}

export const companyChip = (status) => COMPANY_CHIP[status] || 'chip-none'
export const itemChip = (status) => ITEM_CHIP[status] || 'chip-none'
