<template>
  <div class="game-info">
    <div class="player" :class="{ active: currentTurn === 'RED' }">
      <span class="dot red"></span>
      {{ redPlayer || '红方' }}
    </div>
    <div class="vs">VS</div>
    <div class="player" :class="{ active: currentTurn === 'BLACK' }">
      <span class="dot black"></span>
      {{ blackPlayer || '黑方' }}
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Side } from '../../types/game'

defineProps<{
  redPlayer: string
  blackPlayer: string
  currentTurn: Side
}>()
</script>

<style scoped>
.game-info { display: flex; align-items: center; gap: 20px; padding: 12px 24px; background: #16213e; border-radius: 10px; }
.player { font-size: 16px; display: flex; align-items: center; gap: 6px; opacity: 0.5; }
.player.active { opacity: 1; font-weight: bold; }
.dot { width: 12px; height: 12px; border-radius: 50%; }
.dot.red { background: #c0392b; }
.dot.black { background: #2c3e50; border: 1px solid #555; }
.vs { color: #e94560; font-weight: bold; }
</style>
