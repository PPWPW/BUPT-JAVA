import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import HomeView from './views/HomeView.vue'
import GameView from './views/GameView.vue'
import NotationView from './views/NotationView.vue'
import ReplayView from './views/ReplayView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/game/:id', component: GameView },
    { path: '/notations', component: NotationView },
    { path: '/replay/:id', component: ReplayView },
  ],
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
