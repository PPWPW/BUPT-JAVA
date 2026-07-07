<template>
  <div class="captured-area">
    <h3>吃子记录</h3>
    
    <div class="section my-captures">
      <div class="section-title">{{ section1Title }} ({{ section1Pieces.length }})</div>
      <div class="pieces-grid">
        <div 
          v-for="(p, index) in section1Pieces" 
          :key="index" 
          class="mini-piece" 
          :class="[p.side.toLowerCase(), { hidden: !p.revealed }]"
        >
          {{ getPieceText(p) }}
        </div>
        <div v-if="section1Pieces.length === 0" class="empty-text">尚无吃子</div>
      </div>
    </div>
    
    <div class="section opponent-captures">
      <div class="section-title">{{ section2Title }} ({{ section2Pieces.length }})</div>
      <div class="pieces-grid">
        <div 
          v-for="(p, index) in section2Pieces" 
          :key="index" 
          class="mini-piece" 
          :class="[p.side.toLowerCase(), { hidden: !p.revealed }]"
        >
          {{ getPieceText(p) }}
        </div>
        <div v-if="section2Pieces.length === 0" class="empty-text">尚无失子</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Side } from '../../types/game'

interface CapturedPiece {
  type: string | null
  side: Side
  revealed: boolean
}

const props = defineProps<{
  capturedPieces: CapturedPiece[]
  mySide: Side | null
}>()

const PIECE_NAMES: Record<string, string> = {
  king: '帅',
  rook: '车',
  knight: '马',
  cannon: '炮',
  pawn: '兵',
  guard: '仕',
  bishop: '相'
}

const PIECE_NAMES_BLACK: Record<string, string> = {
  king: '将',
  rook: '车',
  knight: '马',
  cannon: '炮',
  pawn: '卒',
  guard: '士',
  bishop: '象'
}

const section1Title = computed(() => {
  if (props.mySide) {
    return '我方吃掉的棋子'
  }
  return '红方吃掉的棋子'
})

const section2Title = computed(() => {
  if (props.mySide) {
    return '对方吃掉的棋子'
  }
  return '黑方吃掉的棋子'
})

const section1Pieces = computed(() => {
  if (props.mySide) {
    return props.capturedPieces.filter(p => p.side !== props.mySide)
  }
  // For spectator, show black pieces captured by Red
  return props.capturedPieces.filter(p => p.side === 'black')
})

const section2Pieces = computed(() => {
  if (props.mySide) {
    return props.capturedPieces.filter(p => p.side === props.mySide)
  }
  // For spectator, show red pieces captured by Black
  return props.capturedPieces.filter(p => p.side === 'red')
})

function getPieceText(p: CapturedPiece): string {
  if (!p.revealed || !p.type) return '?'
  return p.side === 'red' ? PIECE_NAMES[p.type] || '?' : PIECE_NAMES_BLACK[p.type] || '?'
}
</script>

<style scoped>
.captured-area {
  width: 220px;
  background: #16213e;
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  box-shadow: 0 4px 15px rgba(0,0,0,0.3);
  border: 1px solid #1a1a2e;
}
h3 {
  margin: 0;
  font-size: 16px;
  color: #e94560;
  text-align: center;
  border-bottom: 2px solid #e94560;
  padding-bottom: 8px;
}
.section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.section-title {
  font-size: 12px;
  color: #8892b0;
  font-weight: bold;
}
.pieces-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 40px;
  padding: 8px;
  background: #0f172a;
  border-radius: 6px;
}
.empty-text {
  font-size: 11px;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}
.mini-piece {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  user-select: none;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}
.mini-piece.red {
  background: #fff5e6;
  color: #c0392b;
  border: 1.5px solid #c0392b;
}
.mini-piece.black {
  background: #fff5e6;
  color: #2c3e50;
  border: 1.5px solid #2c3e50;
}
.mini-piece.hidden {
  background: #1e293b;
  color: #64748b;
  border: 1.5px solid #475569;
}
</style>
