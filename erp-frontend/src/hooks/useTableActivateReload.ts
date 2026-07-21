import { onActivated } from 'vue'

/**
 * Refresh a cached real-time list when the user switches back to its tab.
 * ProTable owns the first request, so the first activation is intentionally skipped.
 */
export function useTableActivateReload(reload: () => void | Promise<void>) {
  let activatedOnce = false

  onActivated(() => {
    if (!activatedOnce) {
      activatedOnce = true
      return
    }
    void reload()
  })
}
