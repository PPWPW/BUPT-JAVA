<template>
  <div class="move-history">
    <h3>走子记录</h3>
    <div class="moves-list" ref="listRef">
      <div v-for="m in moves" :key="m.moveNumber" class="move-item" :class="m.side.toLowerCase()">
        <span class="num">{{ m.moveNumber }}.</span>
        <span class="move-val">{{ m.source }} -> {{ m.destination }}</span>
      </div>
      <div v-if="moves.length === 0" class="empty">暂无走子</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { MoveRecord } from '../../types/game'

const props = defineProps<{ moves: MoveRecord[] }>()
const listRef = ref<HTMLElement | null>(null)

watch(() => props.moves.length, () => {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
})
</script>

<style scoped>
.move-history { background: #16213e; border-radius: 10px; padding: 16px; width: 220px; max-height: 520px; display: flex; flex-direction: column; }
h3 { font-size: 14px; color: #aaa; margin-bottom: 8px; }
.moves-list { overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 4px; }
.move-item { display: flex; font-size: 14px; padding: 2px 0; align-items: center; }
.num { color: #666; width: 35px; }
.move-val { font-weight: bold; }
.red .move-val { color: #e74c3c; }
.black .move-val { color: #95a5a6; }
.empty { color: #555; font-size: 13px; }
</style>
