<template>
  <div
    class="piece"
    :class="[piece.side.toLowerCase(), { hidden: !piece.revealed, selected: isSelected }]"
    @click="$emit('click')"
  >
    {{ displayText }}
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { PIECE_NAMES, PIECE_NAMES_BLACK } from '../../types/game'
import type { ChessPiece as Piece } from '../../types/game'

const props = defineProps<{ piece: Piece; isSelected: boolean }>()
defineEmits<{ click: [] }>()

const displayText = computed(() => {
  if (!props.piece.revealed) return '?'
  if (props.piece.side === 'RED') return PIECE_NAMES[props.piece.type!] || '?'
  return PIECE_NAMES_BLACK[props.piece.type!] || '?'
})
</script>

<style scoped>
.piece {
  width: 56px; height: 56px; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; font-size: 22px;
  font-weight: bold; cursor: pointer; user-select: none; transition: transform 0.15s;
}
.piece.red { background: #fff5e6; color: #c0392b; border: 2px solid #c0392b; }
.piece.black { background: #fff5e6; color: #2c3e50; border: 2px solid #2c3e50; }
.piece.hidden { background: #2c3e50; color: #95a5a6; border: 2px solid #555; font-size: 18px; }
.piece:hover { transform: scale(1.1); }
.piece.selected { border-color: #f1c40f; box-shadow: 0 0 12px #f1c40f; }
</style>
