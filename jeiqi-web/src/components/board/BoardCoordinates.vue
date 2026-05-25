<template>
  <div class="coords-row">
    <span v-for="c in cols" :key="c" class="coord">{{ c }}</span>
  </div>
  <div class="coords-col">
    <span v-for="r in rows" :key="r" class="coord">{{ r }}</span>
  </div>
</template>

<script setup lang="ts">
const cols = ['a','b','c','d','e','f','g','h','i']
const rows = [9,8,7,6,5,4,3,2,1,0]
</script>

<style scoped>
.coords-row { display: flex; padding-left: 64px; gap: 0; margin-bottom: 2px; }
.coords-row .coord { width: 64px; text-align: center; font-size: 12px; color: #888; }
.coords-col { position: absolute; left: 4px; top: 20px; display: flex; flex-direction: column; }
.coords-col .coord { height: 64px; line-height: 64px; width: 20px; text-align: center; font-size: 12px; color: #888; }
</style>
