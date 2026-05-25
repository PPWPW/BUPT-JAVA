<template>
  <div class="move-history">
    <h3>走子记录</h3>
    <div class="moves-list" ref="listRef">
      <div v-for="m in moves" :key="m.moveNumber" class="move-item">
        <span class="num">{{ m.moveNumber }}.</span>
        <span :class="m.side === 'RED' ? 'red' : 'black'">
          {{ m.notation }}
        </span>
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
.move-history { background: #16213e; border-radius: 10px; padding: 16px; width: 200px; max-height: 400px; display: flex; flex-direction: column; }
h3 { font-size: 14px; color: #aaa; margin-bottom: 8px; }
.moves-list { overflow-y: auto; flex: 1; }
.move-item { font-size: 14px; padding: 2px 0; }
.num { color: #666; margin-right: 6px; }
.red { color: #e74c3c; }
.black { color: #95a5a6; }
.empty { color: #555; font-size: 13px; }
</style>
