import { ref, onUnmounted } from 'vue'

export function useTimer(seconds: number = 60) {
  const remaining = ref(seconds)
  const running = ref(false)
  let interval: ReturnType<typeof setInterval> | null = null

  function start() {
    if (running.value) return
    running.value = true
    interval = setInterval(() => {
      if (remaining.value > 0) {
        remaining.value--
      } else {
        stop()
      }
    }, 1000)
  }

  function stop() {
    running.value = false
    if (interval) { clearInterval(interval); interval = null }
  }

  function reset(s: number = seconds) {
    stop()
    remaining.value = s
  }

  onUnmounted(() => stop())

  return { remaining, running, start, stop, reset }
}
