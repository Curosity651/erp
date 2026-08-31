export const createReactivationRefresh = (refresh: () => void | Promise<void>) => {
  let activated = false

  return async () => {
    if (!activated) {
      activated = true
      return
    }
    await refresh()
  }
}
