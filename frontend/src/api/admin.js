import client from './client'

export const adminApi = {
  // 기업
  searchCompanies(keyword) {
    return client
      .get('/api/admin/companies', { params: keyword ? { keyword } : {} })
      .then((r) => r.data)
  },
  getCompany(id) {
    return client.get(`/api/admin/companies/${id}`).then((r) => r.data)
  },
  createCompany(name) {
    return client.post('/api/admin/companies', { name }).then((r) => r.data)
  },
  updateName(id, name) {
    return client.put(`/api/admin/companies/${id}/name`, { name }).then((r) => r.data)
  },
  deleteCompany(id) {
    return client.delete(`/api/admin/companies/${id}`).then((r) => r.data)
  },
  // 제출 링크 — validDays 가 null 이면 기한 없음
  issueLink(id, validDays) {
    return client.post(`/api/admin/companies/${id}/link`, { validDays }).then((r) => r.data)
  },
  revokeLink(id) {
    return client.delete(`/api/admin/companies/${id}/link`).then((r) => r.data)
  },
  // 링크 주소는 서버가 직접 만든다 (받는 번호만 보낸다)
  sendLinkSms(id, to) {
    return client.post(`/api/admin/companies/${id}/link/sms`, { to }).then((r) => r.data)
  },
  updatePhone(id, phoneNumber) {
    return client.put(`/api/admin/companies/${id}/phone`, { phoneNumber }).then((r) => r.data)
  },
  assignRequirements(id, requirementIds) {
    return client
      .put(`/api/admin/companies/${id}/requirements`, { requirementIds })
      .then((r) => r.data)
  },
  assignManagers(id, managerIds) {
    return client
      .put(`/api/admin/companies/${id}/managers`, { managerIds })
      .then((r) => r.data)
  },

  // 담당자(마스터)
  listManagers() {
    return client.get('/api/admin/managers').then((r) => r.data)
  },
  createManager(payload) {
    return client.post('/api/admin/managers', payload).then((r) => r.data)
  },
  updateManager(id, payload) {
    return client.put(`/api/admin/managers/${id}`, payload).then((r) => r.data)
  },
  deleteManager(id) {
    return client.delete(`/api/admin/managers/${id}`).then((r) => r.data)
  },

  // 요청 자료(마스터)
  listRequirements() {
    return client.get('/api/admin/requirements').then((r) => r.data)
  },
  createRequirement({ name, description, classificationHint, fileType, documentKeywords, exampleImage }) {
    const form = new FormData()
    form.append('name', name)
    if (description) form.append('description', description)
    if (classificationHint) form.append('classificationHint', classificationHint)
    if (fileType) form.append('fileType', fileType)
    if (documentKeywords) form.append('documentKeywords', documentKeywords)
    if (exampleImage) form.append('exampleImage', exampleImage)
    return client
      .post('/api/admin/requirements', form, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then((r) => r.data)
  },
  updateRequirement(id, { name, description, classificationHint, fileType, documentKeywords, exampleImage }) {
    const form = new FormData()
    form.append('name', name)
    if (description) form.append('description', description)
    if (classificationHint) form.append('classificationHint', classificationHint)
    if (fileType) form.append('fileType', fileType)
    if (documentKeywords) form.append('documentKeywords', documentKeywords)
    if (exampleImage) form.append('exampleImage', exampleImage)
    return client
      .put(`/api/admin/requirements/${id}`, form, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then((r) => r.data)
  },
  deleteRequirement(id) {
    return client.delete(`/api/admin/requirements/${id}`).then((r) => r.data)
  },

  // 알림 배지 (폴링용 경량 카운트)
  getNotifications() {
    return client.get('/api/admin/notifications').then((r) => r.data)
  },

  // 관리자 설정
  getSettings() {
    return client.get('/api/admin/settings').then((r) => r.data)
  },
  updateSmsEnabled(enabled) {
    return client.put('/api/admin/settings/sms', { enabled }).then((r) => r.data)
  },
  updateAdminPhone(adminPhoneNumber) {
    return client
      .put('/api/admin/settings/admin-phone', { adminPhoneNumber })
      .then((r) => r.data)
  },

  // 제출/검수
  getSubmissions(companyId) {
    return client.get(`/api/admin/companies/${companyId}/submissions`).then((r) => r.data)
  },
  approve(itemId) {
    return client.post(`/api/admin/submissions/${itemId}/approve`).then((r) => r.data)
  },
  // 통과·반환 응답: { item, notice } - 이 처리로 검수를 마쳐 반환 안내를 보냈으면 notice 에 결과
  markReturned(itemId, reason, notifyCustomer = true) {
    return client
      .post(`/api/admin/submissions/${itemId}/return`, { reason, notifyCustomer })
      .then((r) => r.data)
  },
  getHistory(companyId) {
    return client.get(`/api/admin/companies/${companyId}/history`).then((r) => r.data)
  },
  // 지금 반환된 항목을 모아 고객에게 안내 문자를 (다시) 보낸다
  sendReturnNotice(companyId) {
    return client.post(`/api/admin/companies/${companyId}/return-notice`).then((r) => r.data)
  },
  updateItemNote(itemId, note) {
    return client
      .put(`/api/admin/submissions/${itemId}/note`, { note })
      .then((r) => r.data)
  }
}
