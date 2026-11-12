import client from './client'

// 모든 고객 요청은 제출 링크 토큰 아래에서 이뤄진다 (서버가 토큰으로 기업을 확정)
const base = (token) => `/api/customer/links/${encodeURIComponent(token)}`

export const customerApi = {
  open(token) {
    return client.get(base(token)).then((r) => r.data)
  },
  uploadPhoto(token, itemId, file, onProgress) {
    const form = new FormData()
    form.append('file', file)
    return client
      .post(`${base(token)}/items/${itemId}/photo`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000, // 느린 회선에서 대용량 업로드 여유
        onUploadProgress: onProgress
      })
      .then((r) => r.data)
  },
  submit(token) {
    return client.post(`${base(token)}/submit`).then((r) => r.data)
  },

  // --- 문서: 분류를 위해 먼저 서버로 올린 뒤, 확정은 전송 단계에서 한다 ---
  stageDocument(token, file, onProgress) {
    const form = new FormData()
    form.append('file', file)
    return client
      .post(`${base(token)}/documents`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 180000, // 기본 30초로는 문서 업로드에 부족
        onUploadProgress: onProgress
      })
      .then((r) => r.data)
  },
  listStagedDocuments(token) {
    return client.get(`${base(token)}/documents`).then((r) => r.data)
  },
  deleteStagedDocument(token, stagedId) {
    return client.delete(`${base(token)}/documents/${stagedId}`).then((r) => r.data)
  },
  attachDocument(token, itemId, stagedId) {
    return client
      .post(`${base(token)}/items/${itemId}/attach-document`, { stagedId })
      .then((r) => r.data)
  }
}
