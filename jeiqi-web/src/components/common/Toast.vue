<template>
  <div v-if="visible" class="toast" :class="type">{{ message }}</div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
const type = ref<'success' | 'error'>('success')
let timer: ReturnType<typeof setTimeout> | null = null

function show(msg: string, t: 'success' | 'error' = 'success') {
  message.value = msg
  type.value = t
  visible.value = true
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { visible.value = false }, 3000)
}

defineExpose({ show })
</script>

<style scoped>
.toast {
  position: fixed; top: 20px; right: 20px; padding: 12px 24px;
  border-radius: 8px; color: white; z-index: 1000; font-size: 14px;
}
.success { background: #2ecc71; }
.error { background: #e74c3c; }
</style>
