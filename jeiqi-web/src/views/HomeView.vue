<template>
  <div class="home">
    <h1>揭棋对弈</h1>
    <div v-if="!userStore.userId" class="auth-box">
      <input v-model="username" placeholder="用户名" @keyup.enter="submitAuth('login')" />
      <input v-model="password" type="password" placeholder="密码" @keyup.enter="submitAuth('login')" />
      <div class="auth-btns">
        <button class="btn-primary" @click="submitAuth('login')">登录</button>
        <button class="btn-secondary" @click="submitAuth('register')">注册</button>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
    </div>

    <div v-else class="lobby">
      <p class="welcome">欢迎, {{ userStore.username }}</p>
      <MatchButton ref="matchBtn" @match="onMatch" @cancel="onCancelMatch" />
      <div class="links">
        <router-link to="/notations" class="btn-secondary">棋谱浏览</router-link>
        <button class="btn-secondary" @click="userStore.logout()">退出</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import { useGameStore } from '../stores/gameStore'
import { useWebSocket } from '../composables/useWebSocket'
import MatchButton from '../components/lobby/MatchButton.vue'

const router = useRouter()
const userStore = useUserStore()
const gameStore = useGameStore()

const username = ref('')
const password = ref('')
const error = ref<string | null>(null)
const ws = useWebSocket()
const matchBtn = ref<InstanceType<typeof MatchButton> | null>(null)

async function submitAuth(mode: 'login' | 'register') {
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  const err = mode === 'login'
    ? await userStore.doLogin(username.value, password.value)
    : await userStore.doRegister(username.value, password.value)
  if (err) { error.value = err; return }
  error.value = null
}

function onMatch() {
  if (!userStore.userId) return
  ws.connect(userStore.userId!, () => {
    ws.joinQueue()
  })
}

watch(() => gameStore.gameId, (newId) => {
  if (newId && gameStore.status === 'PLAYING') {
    ws.subscribeGame(newId)
    router.push(`/game/${newId}`)
  }
})

function onCancelMatch() {
  ws.leaveQueue()
  ws.disconnect()
}
</script>

<style scoped>
.home { display: flex; flex-direction: column; align-items: center; padding-top: 80px; min-height: 100vh; }
h1 { font-size: 36px; color: #e94560; margin-bottom: 40px; }
.auth-box { display: flex; flex-direction: column; gap: 12px; width: 300px; }
.auth-btns { display: flex; gap: 12px; }
.auth-btns button { flex: 1; }
.error { color: #e74c3c; font-size: 14px; text-align: center; }
.welcome { font-size: 20px; margin-bottom: 20px; }
.lobby { text-align: center; }
.links { display: flex; gap: 12px; justify-content: center; margin-top: 16px; }
.links a { text-decoration: none; display: inline-block; padding: 10px 20px; }
</style>
