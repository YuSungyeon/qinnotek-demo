import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  optimizeDeps: {
    // onnxruntime-web 번들 이슈 회피 (Transformers.js)
    exclude: ['@huggingface/transformers']
  },
  server: {
    port: 5173,
    // 로컬 개발 시 백엔드로 프록시 (VITE_API_BASE 미설정 시 사용)
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
