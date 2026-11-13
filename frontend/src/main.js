import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
// 전역 디자인 토큰·공통 컴포넌트 (design.md 모노크롬). 화면 색은 이 파일에서만 바꾼다.
import './style.css'

createApp(App).use(router).mount('#app')
