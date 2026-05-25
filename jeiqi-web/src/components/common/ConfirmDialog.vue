<template>
  <div v-if="visible" class="overlay" @click.self="onCancel">
    <div class="dialog">
      <p>{{ message }}</p>
      <div class="btns">
        <button class="btn-secondary" @click="onCancel">取消</button>
        <button class="btn-primary" @click="onConfirm">确认</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
let resolvePromise: ((v: boolean) => void) | null = null

function show(msg: string): Promise<boolean> {
  message.value = msg
  visible.value = true
  return new Promise((resolve) => { resolvePromise = resolve })
}

function onConfirm() { visible.value = false; resolvePromise?.(true) }
function onCancel() { visible.value = false; resolvePromise?.(false) }

defineExpose({ show })
</script>

<style scoped>
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; z-index: 999; }
.dialog { background: #16213e; padding: 24px 32px; border-radius: 12px; text-align: center; }
.dialog p { margin-bottom: 16px; font-size: 16px; }
.btns { display: flex; gap: 12px; justify-content: center; }
</style>
