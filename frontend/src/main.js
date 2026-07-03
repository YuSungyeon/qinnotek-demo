import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'
import { applyTheme, loadThemeLocal, saveThemeLocal } from './theme'
import { themeApi } from './api/theme'

// 1) 로컬 캐시 테마 즉시 적용(깜빡임 방지)
const cached = loadThemeLocal()
if (cached) applyTheme(cached)

createApp(App).use(router).mount('#app')

// 2) 서버 테마로 동기화 (관리자가 바꾼 최신값)
themeApi
  .get()
  .then((cfg) => {
    applyTheme(cfg)
    saveThemeLocal(cfg)
  })
  .catch(() => {})
