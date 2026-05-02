import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { latex } from './directives/latex'
import './style.css'

createApp(App).use(router).directive('latex', latex).mount('#app')
