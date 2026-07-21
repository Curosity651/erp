<template>
  <sku-form-panel
    :mode="formMode"
    :sku-id="skuId"
    :initial-data="initialFormData"
    @cancel="handleCancel"
    @submit-success="handleSubmitSuccess"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { emitter } from '@/hooks/mitt'
import SkuFormPanel from './SkuFormPanel.vue'

defineOptions({ name: 'SkuFormPage' })

const route = useRoute()
const router = useRouter()

// 表单模式
const formMode = ref(route.params.mode as 'create' | 'edit' | 'view' | 'copy')

// SKU ID
const id = route.params.id
const skuId = ref(id ? Number(id) : undefined)

// 初始数据（用于复制模式）
const initialFormData = ref<any>()

// 如果是复制模式，从 sessionStorage 中获取复制数据
if (formMode.value === 'copy' && route.query.copyKey) {
  try {
    const copyKey = route.query.copyKey as string

    // 先检查 sessionStorage 是否可用
    if (typeof Storage === 'undefined') {
      throw new Error('浏览器不支持 sessionStorage')
    }

    const copyDataStr = sessionStorage.getItem(copyKey)

    if (copyDataStr) {
      initialFormData.value = JSON.parse(copyDataStr)

      // 使用后立即清除，避免内存泄漏
      sessionStorage.removeItem(copyKey)
    } else {
      message.warning('未找到复制数据，将以新建模式打开')
    }
  } catch (error) {
    console.error('解析复制数据失败:', error)
    const errorMessage = error instanceof Error ? error.message : '未知错误'
    message.error(`复制数据解析失败: ${errorMessage}`)
  }
}

/* 关闭当前 tab 页并跳转到列表页 */
const closeCurrentTabAndGoToList = () => {
  // 跳转到 SKU 列表页
  router.push('/product/sku')

  // 发送事件关闭当前 tab
  emitter.emit('close-current-tab')
}

/* 表单取消处理 */
const handleCancel = () => {
  closeCurrentTabAndGoToList()
}

/* 表单提交成功处理 */
const handleSubmitSuccess = () => {
  // 发送刷新SKU列表的事件
  emitter.emit('refresh-sku-list')

  closeCurrentTabAndGoToList()
}
</script>
