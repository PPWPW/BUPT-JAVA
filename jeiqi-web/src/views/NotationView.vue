<template>
  <div class="notation-view">
    <h2>棋谱浏览</h2>
    <div v-if="notations.length > 0" class="clear-bar">
      <button class="clear-btn" @click="clearRecords">清空对弈记录</button>
    </div>
    <div v-if="notations.length === 0" class="empty">暂无棋谱记录</div>
    <div v-for="n in notations" :key="n.gameId" class="notation-card" @click="router.push(`/replay/${n.gameId}`)">
      <div class="players">{{ n.redPlayerName || '红方' }} vs {{ n.blackPlayerName || '黑方' }}</div>
      <div class="meta">
        <span>{{ n.result || '*' }}</span>
        <span>{{ formatDate(n.gameDate) }}</span>
        <span>{{ n.moveCount }} 手</span>
      </div>
    </div>
    <button class="btn-secondary back-btn" @click="router.push('/')">返回大厅</button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNotations, clearNotations } from '../services/api'

const router = useRouter()
const notations = ref<any[]>([])

function formatDate(timestamp: any): string {
  if (!timestamp) return ''
  const date = new Date(Number(timestamp))
  if (isNaN(date.getTime())) return String(timestamp)
  
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

async function clearRecords() {
  if (!confirm('确定要清空所有对局记录和棋谱吗？')) return
  try {
    const success = await clearNotations()
    if (success) {
      notations.value = []
      alert('清空成功！')
    } else {
      alert('清空失败，请稍后重试。')
    }
  } catch (e) {
    console.error(e)
    alert('请求出错，请重试。')
  }
}

onMounted(async () => {
  try { notations.value = await getNotations() } catch (e) { console.error(e) }
})
</script>

<style scoped>
.notation-view { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
h2 { text-align: center; margin-bottom: 24px; color: #e94560; }
.clear-bar { display: flex; justify-content: flex-end; margin-bottom: 16px; }
.clear-btn { background: #e94560; color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 13px; font-weight: bold; transition: background 0.2s; }
.clear-btn:hover { background: #c8193c; }
.empty { text-align: center; color: #666; padding: 40px; }
.notation-card { background: #16213e; border-radius: 10px; padding: 16px 20px; margin-bottom: 12px; cursor: pointer; transition: background 0.2s; }
.notation-card:hover { background: #1a2d4a; }
.players { font-size: 16px; margin-bottom: 4px; }
.meta { display: flex; gap: 16px; font-size: 13px; color: #888; }
.back-btn { margin-top: 20px; display: block; margin-left: auto; margin-right: auto; }
</style>
