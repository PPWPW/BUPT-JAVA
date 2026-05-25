<template>
  <div id="app-container">
    <router-view />
  </div>
</template>

<script setup lang="ts">
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Microsoft YaHei', sans-serif; background: #1a1a2e; color: #e0e0e0; }
#app-container { min-height: 100vh; }
button { cursor: pointer; border: none; border-radius: 6px; padding: 10px 20px; font-size: 16px; }
.btn-primary { background: #e94560; color: white; }
.btn-primary:hover { background: #c73a52; }
.btn-secondary { background: #0f3460; color: white; }
.btn-secondary:hover { background: #1a4580; }
input { padding: 10px; border: 1px solid #333; border-radius: 6px; background: #16213e; color: #e0e0e0; font-size: 16px; }
</style>
