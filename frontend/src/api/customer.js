import client from './client'

export const customerApi = {
  lookup(phoneNumber) {
    return client.post('/api/customer/lookup', { phoneNumber }).then((r) => r.data)
  },
  uploadPhoto(itemId, file) {
    const form = new FormData()
    form.append('file', file)
    return client
      .post(`/api/customer/items/${itemId}/photo`, form, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then((r) => r.data)
  },
  submit(companyId) {
    return client.post('/api/customer/submit', { companyId }).then((r) => r.data)
  }
}
