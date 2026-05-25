<template>
  <div class="notation-view">
    <h2>棋谱浏览</h2>
    <div v-if="notations.length === 0" class="empty">暂无棋谱记录</div>
    <div v-for="n in notations" :key="n.gameId" class="notation-card" @click="router.push(`/replay/${n.gameId}`)">
      <div class="players">{{ n.redPlayerName || '红方' }} vs {{ n.blackPlayerName || '黑方' }}</div>
      <div class="meta">
        <span>{{ n.result || '*' }}</span>
        <span>{{ n.gameDate }}</span>
        <span>{{ n.moveCount }} 手</span>
      </div>
    </div>
    <button class="btn-secondary back-btn" @click="router.push('/')">返回大厅</button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNotations } from '../services/api'

const router = useRouter()
const notations = ref<any[]>([])

onMounted(async () => {
  try { notations.value = await getNotations() } catch (e) { console.error(e) }
})
</script>

<style scoped>
.notation-view { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
h2 { text-align: center; margin-bottom: 24px; color: #e94560; }
.empty { text-align: center; color: #666; padding: 40px; }
.notation-card { background: #16213e; border-radius: 10px; padding: 16px 20px; margin-bottom: 12px; cursor: pointer; transition: background 0.2s; }
.notation-card:hover { background: #1a2d4a; }
.players { font-size: 16px; margin-bottom: 4px; }
.meta { display: flex; gap: 16px; font-size: 13px; color: #888; }
.back-btn { margin-top: 20px; display: block; margin-left: auto; margin-right: auto; }
</style>
