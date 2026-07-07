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
      
      <div class="custom-room-section">
        <div class="divider"><span>或</span></div>
        
        <div class="room-actions">
          <button class="btn-primary create-btn" @click="onCreateRoom">创建私人房间</button>
          
          <div class="join-box">
            <input v-model="roomIdInput" placeholder="输入6位房间号" maxLength="6" />
            <div class="join-btns">
              <button class="btn-secondary" @click="onJoinRoom">加入对局</button>
              <button class="btn-secondary" @click="onSpectateRoom">进入观战</button>
            </div>
          </div>
        </div>
      </div>

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
const roomIdInput = ref('')
const ws = useWebSocket()
const matchBtn = ref<InstanceType<typeof MatchButton> | null>(null)

async function submitAuth(mode: 'login' | 'register') {
  if (!username.value || !password.value) {
    error.value = '请输入用户名和密码'
    return
  }
  try {
    const err = mode === 'login'
      ? await userStore.doLogin(username.value, password.value)
      : await userStore.doRegister(username.value, password.value)
    if (err) { error.value = err; return }
    error.value = null
  } catch (e: any) {
    error.value = '网络请求失败，请检查后端服务是否启动！'
    console.error(e)
  }
}

function onMatch() {
  if (!userStore.userId) return
  ws.connect(userStore.username!, () => {
    ws.joinQueue()
  })
}

function onCreateRoom() {
  if (!userStore.userId) return
  ws.connect(userStore.username!, () => {
    ws.createRoom()
  })
}

function onJoinRoom() {
  if (!userStore.userId) return
  if (!roomIdInput.value || roomIdInput.value.length !== 6) {
    alert('请输入6位有效的房间号')
    return
  }
  ws.connect(userStore.username!, () => {
    ws.joinRoom(roomIdInput.value)
  })
}

function onSpectateRoom() {
  if (!roomIdInput.value || roomIdInput.value.length !== 6) {
    alert('请输入6位有效的房间号')
    return
  }
  router.push(`/game/${roomIdInput.value}?spectate=true`)
}

watch(
  [() => gameStore.gameId, () => gameStore.status],
  ([newId, newStatus]) => {
    if (newId) {
      if (newStatus === 'PLAYING' || newStatus === 'WAITING') {
        ws.subscribeGame(newId)
        router.push(`/game/${newId}`)
      }
    }
  }
)

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
.links { display: flex; gap: 12px; justify-content: center; margin-top: 24px; }
.links a { text-decoration: none; display: inline-block; padding: 10px 20px; }

.custom-room-section {
  margin-top: 24px;
  width: 100%;
}
.divider {
  display: flex;
  align-items: center;
  text-align: center;
  color: #555;
  margin: 20px 0;
}
.divider::before, .divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid #333;
}
.divider span {
  padding: 0 10px;
  font-size: 14px;
}
.room-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 320px;
  margin: 0 auto;
}
.create-btn {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  font-weight: bold;
}
.join-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #16213e;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #1f2937;
}
.join-box input {
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #374151;
  background: #0f172a;
  color: #fff;
  font-size: 14px;
  text-align: center;
}
.join-btns {
  display: flex;
  gap: 10px;
}
.join-btns button {
  flex: 1;
  padding: 8px;
  font-size: 13px;
}
</style>
