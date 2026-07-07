<template>
  <div class="board-wrapper">
    <BoardCoordinates :flipped="flipped" />
    <div class="board-container">
      <!-- Authentic Chinese Chess Board SVG Grid Background -->
      <svg class="board-grid" :class="{ flipped: flipped }" :style="{ width: 'calc(var(--cell-size) * 9)', height: 'calc(var(--cell-size) * 10)' }" viewBox="0 0 576 640" xmlns="http://www.w3.org/2000/svg">
        <!-- Board wood background -->
        <rect x="0" y="0" width="576" height="640" fill="#f3d7b5" rx="6" />
        
        <!-- Board outer double border -->
        <rect x="32" y="32" width="512" height="576" fill="none" stroke="#704a24" stroke-width="3.5" />
        <rect x="26" y="26" width="524" height="588" fill="none" stroke="#704a24" stroke-width="1.5" stroke-opacity="0.8" />
        
        <!-- Horizontal lines -->
        <g stroke="#704a24" stroke-width="1.5">
          <line x1="32" y1="96" x2="544" y2="96" />
          <line x1="32" y1="160" x2="544" y2="160" />
          <line x1="32" y1="224" x2="544" y2="224" />
          <line x1="32" y1="288" x2="544" y2="288" />
          <line x1="32" y1="352" x2="544" y2="352" />
          <line x1="32" y1="416" x2="544" y2="416" />
          <line x1="32" y1="480" x2="544" y2="480" />
          <line x1="32" y1="544" x2="544" y2="544" />
        </g>

        <!-- Vertical lines -->
        <g stroke="#704a24" stroke-width="1.5">
          <!-- Col 1 -->
          <line x1="96" y1="32" x2="96" y2="288" />
          <line x1="96" y1="352" x2="96" y2="608" />
          <!-- Col 2 -->
          <line x1="160" y1="32" x2="160" y2="288" />
          <line x1="160" y1="352" x2="160" y2="608" />
          <!-- Col 3 -->
          <line x1="224" y1="32" x2="224" y2="288" />
          <line x1="224" y1="352" x2="224" y2="608" />
          <!-- Col 4 -->
          <line x1="288" y1="32" x2="288" y2="288" />
          <line x1="288" y1="352" x2="288" y2="608" />
          <!-- Col 5 -->
          <line x1="352" y1="32" x2="352" y2="288" />
          <line x1="352" y1="352" x2="352" y2="608" />
          <!-- Col 6 -->
          <line x1="416" y1="32" x2="416" y2="288" />
          <line x1="416" y1="352" x2="416" y2="608" />
          <!-- Col 7 -->
          <line x1="480" y1="32" x2="480" y2="288" />
          <line x1="480" y1="352" x2="480" y2="608" />
        </g>

        <!-- Palaces (九宫格斜线) -->
        <g stroke="#704a24" stroke-width="1.5">
          <line x1="224" y1="32" x2="352" y2="160" />
          <line x1="352" y1="32" x2="224" y2="160" />
          <line x1="224" y1="480" x2="352" y2="608" />
          <line x1="352" y1="480" x2="224" y2="608" />
        </g>

        <!-- Cannon & Pawn starting markers -->
        <g stroke="#704a24" stroke-width="1.2" stroke-opacity="0.8" fill="none">
          <!-- Red Cannons -->
          <path d="M 86 160 H 106 M 96 150 V 170" />
          <path d="M 470 160 H 490 M 480 150 V 170" />
          <!-- Black Cannons -->
          <path d="M 86 480 H 106 M 96 470 V 490" />
          <path d="M 470 480 H 490 M 480 470 V 490" />
          <!-- Red Pawns -->
          <path d="M 22 224 H 42 M 32 214 V 234" />
          <path d="M 150 224 H 170 M 160 214 V 234" />
          <path d="M 278 224 H 298 M 288 214 V 234" />
          <path d="M 406 224 H 426 M 416 214 V 234" />
          <path d="M 534 224 H 554 M 544 214 V 234" />
          <!-- Black Pawns -->
          <path d="M 22 416 H 42 M 32 406 V 426" />
          <path d="M 150 416 H 170 M 160 406 V 426" />
          <path d="M 278 416 H 298 M 288 406 V 426" />
          <path d="M 406 416 H 426 M 416 406 V 426" />
          <path d="M 534 416 H 554 M 544 406 V 426" />
        </g>

        <!-- Chu River & Han Border Texts -->
        <text x="160" y="330" font-family="'KaiTi', 'SimSun', 'STKaiti', serif" font-size="28" fill="#704a24" font-weight="bold" text-anchor="middle">楚 河</text>
        <text x="416" y="330" font-family="'KaiTi', 'SimSun', 'STKaiti', serif" font-size="28" fill="#704a24" font-weight="bold" text-anchor="middle">汉 界</text>
      </svg>

      <!-- Active Board Cells (Layered on top) -->
      <div class="board" :class="{ flipped: flipped }">
        <div
          v-for="row in rowsDisplay"
          :key="row"
          class="board-row"
        >
          <div
            v-for="col in colsDisplay"
            :key="col"
            class="cell"
            :class="{ 
              legal: isLegal(col, row), 
              'my-turn': myTurn,
              'last-move-src': isLastMoveSrc(col, row),
              'last-move-dst': isLastMoveDst(col, row)
            }"
            @click="$emit('cellClick', col, row)"
          >
            <ChessPiece
              v-if="getPiece(col, row)"
              :piece="getPiece(col, row)!"
              :isSelected="isSelected(col, row)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import BoardCoordinates from './BoardCoordinates.vue'
import ChessPiece from './ChessPiece.vue'
import type { ChessPiece as Piece, Side, MoveRecord } from '../../types/game'

const props = defineProps<{
  pieces: Piece[]
  selectedPos: { col: number; row: number } | null
  legalMoves: { col: number; row: number }[]
  mySide: Side | null
  myTurn: boolean
  lastMove?: MoveRecord | null
}>()

defineEmits<{ cellClick: [col: number, row: number] }>()

const cols = [0,1,2,3,4,5,6,7,8]
const rows = [9,8,7,6,5,4,3,2,1,0]

const flipped = computed(() => props.mySide === 'black')

const colsDisplay = computed(() => flipped.value ? [...cols].reverse() : cols)
const rowsDisplay = computed(() => flipped.value ? [...rows].reverse() : rows)

function getPiece(col: number, row: number): Piece | undefined {
  return props.pieces.find(p => p.alive && p.position.col === col && p.position.row === row)
}

function isSelected(col: number, row: number): boolean {
  return props.selectedPos?.col === col && props.selectedPos?.row === row
}

function isLegal(col: number, row: number): boolean {
  return props.legalMoves.some(m => m.col === col && m.row === row)
}

const COLS = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i']
function posToAlgebraic(col: number, row: number): string {
  return COLS[col] + row
}

function isLastMoveSrc(col: number, row: number): boolean {
  if (!props.lastMove) return false
  return props.lastMove.source === posToAlgebraic(col, row)
}

function isLastMoveDst(col: number, row: number): boolean {
  if (!props.lastMove) return false
  return props.lastMove.destination === posToAlgebraic(col, row)
}
</script>

<style scoped>
.board-wrapper {
  position: relative;
  display: inline-block;
}

.board-container {
  position: relative;
  width: calc(var(--cell-size) * 9);
  height: calc(var(--cell-size) * 10);
  margin-left: calc(var(--cell-size) * 0.5);
  border-radius: 6px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.5);
  overflow: hidden;
}
.board-grid {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 0;
  pointer-events: none;
  transition: transform 0.3s ease;
}
.board-grid.flipped {
  transform: rotate(180deg);
}
.board {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 1;
  display: block;
  background: transparent;
  transition: transform 0.3s ease;
}
.cell.last-move-src::before {
  content: '';
  position: absolute;
  top: 4px; left: 4px; right: 4px; bottom: 4px;
  border: 2px dashed rgba(243, 156, 18, 0.7);
  background: rgba(243, 156, 18, 0.1);
  border-radius: 50%;
  pointer-events: none;
}
.cell.last-move-dst::before {
  content: '';
  position: absolute;
  top: 2px; left: 2px; right: 2px; bottom: 2px;
  border: 2.5px solid rgba(243, 156, 18, 0.85);
  background: rgba(243, 156, 18, 0.18);
  border-radius: 50%;
  pointer-events: none;
}
.board-row { display: flex; }
.cell {
  width: var(--cell-size);
  height: var(--cell-size);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.cell.legal::after {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  background: rgba(46, 204, 113, 0.6);
  border: 2px solid #2ecc71;
  border-radius: 50%;
  box-shadow: 0 0 8px #2ecc71;
}
.cell.my-turn { cursor: pointer; }
</style>
