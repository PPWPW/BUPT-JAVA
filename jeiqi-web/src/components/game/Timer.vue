<template>
  <div class="timer" :class="{ danger: remaining <= 10 }">
    {{ formatTime(remaining) }}
  </div>
</template>

<script setup lang="ts">
defineProps<{ remaining: number }>()

function formatTime(s: number): string {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${sec.toString().padStart(2, '0')}`
}
</script>

<style scoped>
.timer { font-size: 24px; font-weight: bold; color: #e0e0e0; text-align: center; width: 100%; }
.timer.danger { color: #e74c3c; animation: blink 0.5s infinite alternate; }
@keyframes blink { to { opacity: 0.5; } }
</style>
