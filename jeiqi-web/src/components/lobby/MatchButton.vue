<template>
  <div class="match-area">
    <button
      class="btn-primary match-btn"
      @click="toggleMatch"
    >
      {{ matching ? '匹配中(点击取消)' : '开始匹配' }}
    </button>
    <p v-if="matching" class="hint">正在寻找对手...</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{ match: []; cancel: [] }>()
const matching = ref(false)

function toggleMatch() {
  if (matching.value) {
    matching.value = false
    emit('cancel')
  } else {
    matching.value = true
    emit('match')
  }
}

defineExpose({ stopMatching: () => { matching.value = false } })
</script>

<style scoped>
.match-area { text-align: center; margin: 24px 0; }
.match-btn { font-size: 18px; padding: 14px 40px; }
.match-btn:disabled { background: #555; cursor: not-allowed; }
.hint { margin-top: 12px; color: #aaa; }
</style>
