<template>
  <div class="game-view">
    <div class="game-main">
      <div class="left-panel">
        <GameInfo
          :redPlayer="gameStore.redPlayer"
          :blackPlayer="gameStore.blackPlayer"
          :currentTurn="gameStore.currentTurn"
        />
        <Timer :remaining="timer.remaining.value" />
        
        <div class="turn-banner" :class="{ 'my-turn': isMyTurn }">
          <span class="pulse-dot"></span>
          {{ turnMessage }}
        </div>

        <CapturedPiecesArea
          :capturedPieces="gameStore.capturedPieces"
          :mySide="gameStore.mySide"
        />
      </div>

      <div class="board-area">
        <ChessBoard
          :pieces="gameStore.pieces"
          :selectedPos="gameStore.selectedPos"
          :legalMoves="gameStore.legalMoves"
          :mySide="gameStore.mySide"
          :myTurn="isMyTurn"
          :lastMove="lastMove"
          @cellClick="onCellClick"
        />

        <div class="actions">
          <button class="btn-secondary" @click="onResign">认输</button>
          <button class="btn-secondary" @click="onDraw">求和</button>
        </div>
      </div>

      <MoveHistory :moves="gameStore.moveHistory" />
    </div>

    <GameResult
      :visible="showResult"
      :winner="gameStore.winner"
      :reason="gameStore.reason"
      :mySide="gameStore.mySide"
      :showReplay="true"
      @close="router.push('/')"
      @replay="router.push(`/replay/${gameStore.gameId}`)"
    />

    <!-- Draw Request Modal -->
    <div v-if="showDrawRequestModal" class="overlay-draw">
      <div class="draw-dialog">
        <h3>求和申请</h3>
        <p>对方发起了求和申请，您是否同意和棋？</p>
        <div class="draw-btns">
          <button class="btn-primary" @click="onAcceptDraw">同意</button>
          <button class="btn-secondary" @click="onRejectDraw">拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import { useGame } from '../composables/useGame'
import { useTimer } from '../composables/useTimer'
import GameInfo from '../components/game/GameInfo.vue'
import Timer from '../components/game/Timer.vue'
import ChessBoard from '../components/board/ChessBoard.vue'
import MoveHistory from '../components/game/MoveHistory.vue'
import GameResult from '../components/game/GameResult.vue'
import CapturedPiecesArea from '../components/game/CapturedPiecesArea.vue'

const router = useRouter()
const gameStore = useGameStore()
const { handleCellClick, board, ws } = useGame()
const timer = useTimer(60)
const showResult = ref(false)
const showDrawRequestModal = ref(false)

const isMyTurn = computed(() => gameStore.currentTurn === gameStore.mySide)

const lastMove = computed(() => {
  if (gameStore.moveHistory.length === 0) return null
  return gameStore.moveHistory[gameStore.moveHistory.length - 1]
})

const turnMessage = computed(() => {
  if (gameStore.status === 'FINISHED') return '对局已结束'
  return isMyTurn.value ? '您的回合，请选择棋子移动' : '对方正在思考中...'
})

function onCellClick(col: number, row: number) {
  handleCellClick(col, row)
}

function onResign() {
  ws.resign()
}

function onDraw() {
  ws.requestDraw(true)
}

function onAcceptDraw() {
  ws.requestDraw(true)
  showDrawRequestModal.value = false
  gameStore.drawRequestReceived = false
}

function onRejectDraw() {
  ws.requestDraw(false)
  showDrawRequestModal.value = false
  gameStore.drawRequestReceived = false
}

watch(() => gameStore.status, (s) => {
  if (s === 'PLAYING') {
    timer.reset(60)
    timer.start()
  }
  if (s === 'FINISHED') {
    timer.stop()
    showResult.value = true
  }
})

watch(() => gameStore.currentTurn, () => {
  if (gameStore.status === 'PLAYING') {
    timer.reset(60)
    timer.start()
  }
})

watch(() => gameStore.drawRequestReceived, (val) => {
  if (val) {
    showDrawRequestModal.value = true
  }
})

watch(() => gameStore.drawRejected, (val) => {
  if (val) {
    alert('对方拒绝了您的求和申请。')
    gameStore.drawRejected = false
  }
})

onUnmounted(() => {
  gameStore.reset()
})
</script>

<style scoped>
.game-view {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 10px;
  background: #1a1a2e;
}
.game-main { display: flex; gap: 20px; align-items: flex-start; }
.left-panel { display: flex; flex-direction: column; gap: 12px; width: 220px; }
.board-area { display: flex; flex-direction: column; gap: 10px; align-items: center; }
.turn-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 16px;
  background: #1f2937;
  border-radius: 6px;
  font-size: 13px;
  color: #9ca3af;
  border: 1px solid #374151;
  transition: all 0.3s;
  width: 100%;
  box-sizing: border-box;
}
.turn-banner.my-turn {
  color: #2ecc71;
  border-color: #2ecc71;
  background: rgba(46, 204, 113, 0.1);
  box-shadow: 0 0 10px rgba(46, 204, 113, 0.2);
}
.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9ca3af;
}
.my-turn .pulse-dot {
  background: #2ecc71;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0% { transform: scale(0.9); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.5; }
  100% { transform: scale(0.9); opacity: 1; }
}
.actions { display: flex; gap: 12px; margin-top: 4px; }

.overlay-draw { position: fixed; inset: 0; background: rgba(0,0,0,0.85); display: flex; align-items: center; justify-content: center; z-index: 1001; }
.draw-dialog { background: #16213e; padding: 24px 36px; border-radius: 12px; text-align: center; border: 1px solid #e94560; max-width: 320px; }
.draw-dialog h3 { color: #e94560; margin-bottom: 12px; font-size: 20px; }
.draw-dialog p { color: #ccc; margin-bottom: 20px; font-size: 14px; }
.draw-btns { display: flex; gap: 16px; justify-content: center; }
</style>
