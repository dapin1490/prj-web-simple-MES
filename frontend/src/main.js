import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

const THEME_STORAGE_KEY = 'mes-theme-mode'
const savedThemeMode = localStorage.getItem(THEME_STORAGE_KEY)
const initialThemeMode = savedThemeMode === 'light' || savedThemeMode === 'dark' ? savedThemeMode : 'light'
document.documentElement.setAttribute('data-theme', initialThemeMode)

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
