import client from './client'

export const themeApi = {
  get() {
    return client.get('/api/theme').then((r) => r.data)
  },
  update({ designId, themeId, primaryColor }) {
    return client
      .put('/api/admin/settings/theme', { designId, themeId, primaryColor })
      .then((r) => r.data)
  }
}
