import { Modal } from 'ant-design-vue'
import type { Ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'

export interface UnsavedChangesGuardOptions {
  title?: string
  content?: string
  okText?: string
  cancelText?: string
}

/**
 * 未保存变更确认守卫
 * @param isDirty 是否有未保存的变更
 * @param options 配置选项
 */
export function useUnsavedChangesGuard(
  isDirty: Ref<boolean>,
  options?: UnsavedChangesGuardOptions
) {
  const defaultOptions = {
    title: '确认离开',
    content: '您有未保存的修改，确定要离开吗？',
    okText: '确定离开',
    cancelText: '继续编辑',
    ...options
  }

  /**
   * 确认后执行操作
   * @param onConfirm 确认后的回调
   */
  const confirmIfDirty = (onConfirm: () => void) => {
    if (isDirty.value) {
      Modal.confirm({
        title: defaultOptions.title,
        content: defaultOptions.content,
        okText: defaultOptions.okText,
        cancelText: defaultOptions.cancelText,
        onOk: () => {
          isDirty.value = false
          onConfirm()
        }
      })
    } else {
      onConfirm()
    }
  }

  /**
   * 重置脏状态
   */
  const resetDirty = () => {
    isDirty.value = false
  }

  /**
   * 注册路由离开守卫
   * 当有未保存变更时，弹出确认框阻止路由跳转
   */
  const useRouteLeaveGuard = () => {
    onBeforeRouteLeave((_to, _from, next) => {
      if (isDirty.value) {
        Modal.confirm({
          title: defaultOptions.title,
          content: defaultOptions.content,
          okText: defaultOptions.okText,
          cancelText: defaultOptions.cancelText,
          onOk: () => {
            isDirty.value = false
            next()
          },
          onCancel: () => next(false)
        })
      } else {
        next()
      }
    })
  }

  return {
    confirmIfDirty,
    resetDirty,
    useRouteLeaveGuard
  }
}
