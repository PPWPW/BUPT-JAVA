<template>
  <div v-if="visible" class="overlay" @click.self="$emit('close')">
    <div class="result-dialog">
      <h2>{{ title }}</h2>
      <p class="reason">{{ reasonText }}</p>
      <button class="btn-primary" @click="$emit('close')">返回大厅</button>
      <button class="btn-secondary" @click="$emit('replay')" v-if="showReplay">查看棋谱</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Side } from '../../types/game'

const props = defineProps<{
  visible: boolean
  winner: Side | null
  reason: string | null
  mySide: Side | null
  showReplay: boolean
}>()

defineEmits<{ close: []; replay: [] }>()

const title = computed(() => {
  if (!props.winner) return '和棋'
  return props.winner === props.mySide ? '你赢了!' : '你输了'
})

const reasonText = computed(() => {
  const map: Record<string, string> = {
    checkmate: '将死',
    stalemate: '困毙',
    timeout: '超时',
    resign: '认输',
    king_captured: '帅被吃',
    draw_agreed: '双方同意和棋',
    draw_no_capture: '80单步无吃子和棋',
    repetition_loss: '长将/长捉判负',
    disconnect: '对方断线',
  }
  return map[props.reason || ''] || props.reason || ''
})
</script>

<style scoped>
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.result-dialog { background: #16213e; padding: 32px 48px; border-radius: 16px; text-align: center; }
h2 { font-size: 28px; margin-bottom: 12px; color: #f1c40f; }
.reason { color: #aaa; margin-bottom: 24px; font-size: 14px; }
button { margin: 0 8px; }
</style>
