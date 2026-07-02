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
  updatePhone(id, phoneNumber) {
    return client.put(`/api/admin/companies/${id}/phone`, { phoneNumber }).then((r) => r.data)
  },
  assignRequirements(id, requirementIds) {
    return client
      .put(`/api/admin/companies/${id}/requirements`, { requirementIds })
      .then((r) => r.data)
  },

  // 요구 사진(마스터)
  listRequirements() {
    return client.get('/api/admin/requirements').then((r) => r.data)
  },
  createRequirement({ name, description, exampleImage }) {
    const form = new FormData()
    form.append('name', name)
    if (description) form.append('description', description)
    if (exampleImage) form.append('exampleImage', exampleImage)
    return client
      .post('/api/admin/requirements', form, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then((r) => r.data)
  },
  updateRequirement(id, { name, description, exampleImage }) {
    const form = new FormData()
    form.append('name', name)
    if (description) form.append('description', description)
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

  // 제출/검수
  getSubmissions(companyId) {
    return client.get(`/api/admin/companies/${companyId}/submissions`).then((r) => r.data)
  },
  approve(itemId) {
    return client.post(`/api/admin/submissions/${itemId}/approve`).then((r) => r.data)
  },
  markReturned(itemId, reason) {
    return client
      .post(`/api/admin/submissions/${itemId}/return`, { reason })
      .then((r) => r.data)
  }
}
