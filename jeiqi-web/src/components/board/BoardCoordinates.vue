<template>
  <div class="coords-row">
    <span v-for="c in colsDisplay" :key="c" class="coord">{{ c }}</span>
  </div>
  <div class="coords-col">
    <span v-for="r in rowsDisplay" :key="r" class="coord">{{ r }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  flipped: boolean
}>()

const cols = ['a','b','c','d','e','f','g','h','i']
const rows = [9,8,7,6,5,4,3,2,1,0]

const colsDisplay = computed(() => props.flipped ? [...cols].reverse() : cols)
const rowsDisplay = computed(() => props.flipped ? [...rows].reverse() : rows)
</script>

<style scoped>
.coords-row {
  display: flex;
  margin-left: 32px;
  margin-bottom: 2px;
  width: 576px;
}
.coords-row .coord {
  width: 64px;
  text-align: center;
  font-size: 14px;
  font-weight: bold;
  color: #a8a8b3;
}
.coords-col {
  position: absolute;
  left: 0;
  top: 24px;
  display: flex;
  flex-direction: column;
  width: 32px;
}
.coords-col .coord {
  height: 64px;
  line-height: 64px;
  text-align: center;
  font-size: 14px;
  font-weight: bold;
  color: #a8a8b3;
}
</style>
