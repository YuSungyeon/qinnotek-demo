import client from './client'

export const customerApi = {
  lookup(phoneNumber) {
    return client.post('/api/customer/lookup', { phoneNumber }).then((r) => r.data)
  },
  uploadPhoto(itemId, file, onProgress) {
    const form = new FormData()
    form.append('file', file)
    return client
      .post(`/api/customer/items/${itemId}/photo`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000, // 느린 회선에서 대용량 업로드 여유
        onUploadProgress: onProgress
      })
      .then((r) => r.data)
  },
  submit(companyId) {
    return client.post('/api/customer/submit', { companyId }).then((r) => r.data)
  }
}
