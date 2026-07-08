<template>
  <div v-if="visible" class="overlay">
    <div class="result-dialog">
      <h2>{{ title }}</h2>
      <p class="reason">{{ reasonText }}</p>
      
      <!-- Rematch Actions (Only for active players, not spectators) -->
      <div v-if="mySide" class="rematch-actions">
        <div v-if="rematchStatus === 'NONE'">
          <button class="btn-success" @click="$emit('requestRematch')">再来一局</button>
        </div>
        <div v-else-if="rematchStatus === 'SENT'">
          <button class="btn-success" disabled>已申请，等待对方回应...</button>
        </div>
        <div v-else-if="rematchStatus === 'RECEIVED'">
          <div class="rematch-invite">
            <div class="invite-text">对方邀请您再来一局</div>
            <div class="invite-btns">
              <button class="btn-success" @click="$emit('acceptRematch')">接受</button>
              <button class="btn-danger" @click="$emit('declineRematch')">拒绝</button>
            </div>
          </div>
        </div>
      </div>

      <div class="navigation-actions">
        <button class="btn-primary" @click="$emit('close')">返回大厅</button>
        <button class="btn-secondary" @click="$emit('replay')" v-if="showReplay">查看棋谱</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Side } from '../../types/game'

const props = withDefaults(
  defineProps<{
    visible: boolean
    winner: Side | null
    reason: string | null
    mySide: Side | null
    showReplay: boolean
    rematchStatus?: 'NONE' | 'SENT' | 'RECEIVED' | 'DECLINED'
  }>(),
  {
    rematchStatus: 'NONE'
  }
)

defineEmits<{
  close: []
  replay: []
  requestRematch: []
  acceptRematch: []
  declineRematch: []
}>()

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
.result-dialog { background: #16213e; padding: 32px 48px; border-radius: 16px; text-align: center; border: 1px solid #1f2937; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
h2 { font-size: 28px; margin-bottom: 12px; color: #f1c40f; }
.reason { color: #aaa; margin-bottom: 20px; font-size: 14px; }
.rematch-actions {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.rematch-invite {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.invite-text {
  font-size: 14px;
  color: #2ecc71;
  font-weight: 600;
}
.invite-btns {
  display: flex;
  gap: 12px;
}
.navigation-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
button {
  padding: 8px 20px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}
.btn-primary {
  background: #3498db;
  color: white;
}
.btn-primary:hover {
  background: #2980b9;
}
.btn-secondary {
  background: #34495e;
  color: white;
}
.btn-secondary:hover {
  background: #2c3e50;
}
.btn-success {
  background: #2ecc71;
  color: white;
}
.btn-success:hover:not(:disabled) {
  background: #27ae60;
}
.btn-success:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-danger {
  background: #e74c3c;
  color: white;
}
.btn-danger:hover {
  background: #c0392b;
}
</style>
