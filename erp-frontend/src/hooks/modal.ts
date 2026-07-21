export const useModal = (defaultTitle = '') => {
  const title = ref(defaultTitle)

  const open = ref(false)

  // @Decrypted antdv 4.x 后改为使用 open
  const visible = open

  return {
    title,
    visible,
    open,
    openModal() {
      open.value = true
    },
    closeModal() {
      open.value = false
    }
  }
}
