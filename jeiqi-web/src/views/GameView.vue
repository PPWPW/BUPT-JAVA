<template>
  <div class="game-view">
    <GameInfo
      :redPlayer="gameStore.redPlayer"
      :blackPlayer="gameStore.blackPlayer"
      :currentTurn="gameStore.currentTurn"
    />

    <Timer :remaining="timer.remaining.value" />

    <div class="game-main">
      <ChessBoard
        :pieces="gameStore.pieces"
        :selectedPos="gameStore.selectedPos"
        :legalMoves="gameStore.legalMoves"
        :mySide="gameStore.mySide"
        :myTurn="gameStore.currentTurn === gameStore.mySide"
        @cellClick="onCellClick"
      />

      <MoveHistory :moves="gameStore.moveHistory" />
    </div>

    <div class="actions">
      <button class="btn-secondary" @click="onResign">认输</button>
      <button class="btn-secondary" @click="onDraw">求和</button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import { useGame } from '../composables/useGame'
import { useTimer } from '../composables/useTimer'
import GameInfo from '../components/game/GameInfo.vue'
import Timer from '../components/game/Timer.vue'
import ChessBoard from '../components/board/ChessBoard.vue'
import MoveHistory from '../components/game/MoveHistory.vue'
import GameResult from '../components/game/GameResult.vue'

const router = useRouter()
const gameStore = useGameStore()
const { handleCellClick, board, ws } = useGame()
const timer = useTimer(60)
const showResult = ref(false)

function onCellClick(col: number, row: number) {
  handleCellClick(col, row)
}

function onResign() {
  ws.resign()
}

function onDraw() {
  ws.requestDraw(true)
}

watch(() => gameStore.status, (s) => {
  if (s === 'PLAYING') timer.start()
  if (s === 'FINISHED') { timer.stop(); showResult.value = true }
})
</script>

<style scoped>
.game-view { display: flex; flex-direction: column; align-items: center; padding: 20px; gap: 16px; }
.game-main { display: flex; gap: 20px; align-items: flex-start; }
.actions { display: flex; gap: 12px; }
</style>
