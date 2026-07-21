<template>
  <a-popconfirm :title="title" :ok-text="okText" :cancel-text="cancelText" @confirm="handleConfirm">
    <export-button v-bind="$attrs" :loading="loading" :disabled="disabled" />
  </a-popconfirm>
</template>

<script setup lang="ts">
import { ExportButton } from './IconButton'

defineOptions({ name: 'ExportConfirmButton' })

interface ExportConfirmButtonProps {
  // 确认弹窗相关
  title?: string
  okText?: string
  cancelText?: string

  // 导出处理（使用方提供完整的导出逻辑）
  onExport: () => Promise<void> | void

  // 按钮状态
  loading?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<ExportConfirmButtonProps>(), {
  title: '确认导出?',
  okText: '是',
  cancelText: '否',
  loading: false,
  disabled: false
})

const handleConfirm = async () => {
  await props.onExport()
}
</script>
