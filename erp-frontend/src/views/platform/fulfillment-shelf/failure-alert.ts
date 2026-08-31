import { ref } from 'vue'

export function createFailureAlertState(durationMs = 10_000) {
  const failures = ref<string[]>([])
  let dismissTimer: ReturnType<typeof setTimeout> | undefined

  const cancelTimer = () => {
    if (dismissTimer !== undefined) {
      clearTimeout(dismissTimer)
      dismissTimer = undefined
    }
  }

  const clear = () => {
    cancelTimer()
    failures.value = []
  }

  const show = (items: string[]) => {
    cancelTimer()
    failures.value = items
    if (items.length) {
      dismissTimer = setTimeout(clear, durationMs)
    }
  }

  return { failures, show, clear, dispose: cancelTimer }
}
