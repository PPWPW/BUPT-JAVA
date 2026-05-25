<template>
  <div class="board-wrapper">
    <BoardCoordinates />
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
          :class="{ legal: isLegal(col, row), 'my-turn': myTurn }"
          @click="$emit('cellClick', col, row)"
        >
          <ChessPiece
            v-if="getPiece(col, row)"
            :piece="getPiece(col, row)!"
            :isSelected="isSelected(col, row)"
            @click="$emit('cellClick', col, row)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import BoardCoordinates from './BoardCoordinates.vue'
import ChessPiece from './ChessPiece.vue'
import type { ChessPiece as Piece, Side } from '../../types/game'

const props = defineProps<{
  pieces: Piece[]
  selectedPos: { col: number; row: number } | null
  legalMoves: { col: number; row: number }[]
  mySide: Side | null
  myTurn: boolean
}>()

defineEmits<{ cellClick: [col: number, row: number] }>()

const cols = [0,1,2,3,4,5,6,7,8]
const rows = [9,8,7,6,5,4,3,2,1,0]

const flipped = computed(() => props.mySide === 'BLACK')

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
</script>

<style scoped>
.board-wrapper { position: relative; display: inline-block; }
.board { background: #d4a574; border: 3px solid #8b5e3c; border-radius: 4px; padding: 2px; }
.board.flipped { transform: rotate(180deg); }
.board.flipped .piece { transform: rotate(180deg); }
.board-row { display: flex; }
.cell {
  width: 64px; height: 64px; display: flex; align-items: center;
  justify-content: center; position: relative;
}
.cell:nth-child(odd) { background: rgba(139,94,60,0.15); }
.cell.legal::after {
  content: ''; position: absolute; width: 16px; height: 16px;
  background: rgba(0,200,0,0.4); border-radius: 50%;
}
.cell.my-turn { cursor: pointer; }
.board.flipped .cell.legal::after { transform: rotate(180deg); }
</style>
