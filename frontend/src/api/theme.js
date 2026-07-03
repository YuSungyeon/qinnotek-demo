import client from './client'

export const themeApi = {
  get() {
    return client.get('/api/theme').then((r) => r.data)
  },
  update({ themeId, primaryColor }) {
    return client
      .put('/api/admin/settings/theme', { themeId, primaryColor })
      .then((r) => r.data)
  }
}
