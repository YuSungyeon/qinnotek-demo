import axios from 'axios'

// 개발: '' (vite proxy), 운영: VITE_API_BASE (EC2 백엔드)
export const API_BASE = import.meta.env.VITE_API_BASE || ''

const client = axios.create({
  baseURL: API_BASE,
  timeout: 30000
})

// 백엔드가 내려주는 에러 메시지를 그대로 사용
client.interceptors.response.use(
  (res) => res,
  (err) => {
    const message =
      err.response?.data?.message ||
      err.message ||
      '요청 처리 중 오류가 발생했습니다.'
    return Promise.reject(new Error(message))
  }
)

/** 백엔드가 준 상대 경로(/api/files/xxx)를 절대 URL로 변환 */
export function fileUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return API_BASE + path
}

export default client
