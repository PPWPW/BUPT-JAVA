<template>
  <div class="replay-view">
    <h2>棋谱回放</h2>
    <div v-if="notation" class="replay-content">
      <div class="players">
        {{ notation.redPlayerName || '红方' }} vs {{ notation.blackPlayerName || '黑方' }}
        — {{ notation.result || '*' }}
      </div>

      <div class="replay-controls">
        <button class="btn-secondary" @click="prevMove" :disabled="step <= 0">上一步</button>
        <span class="step">{{ step }} / {{ notation.moves.length }}</span>
        <button class="btn-secondary" @click="nextMove" :disabled="step >= notation.moves.length">下一步</button>
        <button class="btn-secondary" @click="step = 0">重置</button>
      </div>

      <div class="move-text">
        <div v-for="(m, i) in notation.moves" :key="i" class="move" :class="{ active: i < step }">
          {{ i + 1 }}. {{ m.notation || m.source + m.destination }}
        </div>
      </div>
    </div>
    <div v-else class="empty">加载中...</div>
    <button class="btn-secondary back-btn" @click="router.push('/notations')">返回棋谱列表</button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNotation } from '../services/api'

const route = useRoute()
const router = useRouter()
const notation = ref<any>(null)
const step = ref(0)

onMounted(async () => {
  try {
    notation.value = await getNotation(route.params.id as string)
    step.value = notation.value.moves?.length || 0
  } catch (e) { console.error(e) }
})

function prevMove() { if (step.value > 0) step.value-- }
function nextMove() { if (notation.value && step.value < notation.value.moves.length) step.value++ }
</script>

<style scoped>
.replay-view { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
h2 { text-align: center; margin-bottom: 24px; color: #e94560; }
.empty { text-align: center; color: #666; padding: 40px; }
.replay-content { background: #16213e; border-radius: 10px; padding: 20px; }
.players { font-size: 16px; margin-bottom: 16px; text-align: center; }
.replay-controls { display: flex; align-items: center; gap: 12px; justify-content: center; margin-bottom: 16px; }
.step { color: #aaa; font-size: 14px; min-width: 60px; text-align: center; }
.replay-controls button:disabled { opacity: 0.4; cursor: not-allowed; }
.move-text { display: flex; flex-wrap: wrap; gap: 4px; }
.move { font-size: 13px; color: #555; padding: 2px 4px; }
.move.active { color: #e0e0e0; }
.back-btn { margin-top: 20px; display: block; margin-left: auto; margin-right: auto; }
</style>
