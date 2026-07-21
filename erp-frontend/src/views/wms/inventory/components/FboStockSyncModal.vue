<template>
  <a-modal
    v-model:open="open"
    :title="step === 1 ? '同步 FBO 库存' : '同步完成'"
    :width="520"
    :confirm-loading="loading"
    :footer="step === 2 ? null : undefined"
    @ok="handleSync"
    @cancel="handleClose"
  >
    <!-- Step 1: 选择同步范围 -->
    <template v-if="step === 1">
      <a-form layout="vertical">
        <a-form-item label="同步范围">
          <a-radio-group v-model:value="syncScope">
            <a-radio value="all">全部 Ozon 店铺</a-radio>
            <a-radio value="single">指定店铺</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="syncScope === 'single'" label="选择店铺" required>
          <ShopSelectInput
            v-model="selectedShopId"
            default-platform="ozon"
            :hide-platform-filter="true"
            placeholder="请选择 Ozon 店铺"
          />
        </a-form-item>
      </a-form>
      <a-alert message="同步将从 Ozon 平台获取最新 FBO 库存数据" type="info" show-icon />
    </template>

    <!-- Step 2: 同步结果 -->
    <template v-else>
      <a-result :status="resultStatus" :title="resultTitle">
        <template #extra>
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="SKU 总数">{{ totalStats.total }}</a-descriptions-item>
            <a-descriptions-item label="成功">{{ totalStats.success }}</a-descriptions-item>
            <a-descriptions-item label="失败">{{ totalStats.fail }}</a-descriptions-item>
            <a-descriptions-item label="未映射">{{ totalStats.unmapped }}</a-descriptions-item>
          </a-descriptions>
          <div class="mt-4 flex justify-center gap-2">
            <a-button @click="handleViewLog">查看同步日志</a-button>
            <a-button type="primary" @click="handleClose">关闭</a-button>
          </div>
        </template>
      </a-result>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { syncFboStock } from '@/api/wms/fbo'
import type { FboSyncResultVO } from '@/api/wms/fbo/types'
import ShopSelectInput from '@/components/ShopSelectInput.vue'

defineOptions({ name: 'FboStockSyncModal' })

const emit = defineEmits<{
  (e: 'success'): void
  (e: 'viewLog'): void
}>()

const open = ref(false)
const loading = ref(false)
const step = ref(1)
const syncScope = ref<'all' | 'single'>('all')
const selectedShopId = ref<number>()
const results = ref<FboSyncResultVO[]>([])

const totalStats = computed(() => {
  return results.value.reduce(
    (acc, r) => ({
      total: acc.total + r.totalCount,
      success: acc.success + r.successCount,
      fail: acc.fail + r.failCount,
      unmapped: acc.unmapped + r.unmappedCount
    }),
    { total: 0, success: 0, fail: 0, unmapped: 0 }
  )
})

// 根据同步结果计算显示状态
const resultStatus = computed(() => {
  const stats = totalStats.value
  // 检查是否有 FAILED 状态的结果
  const hasFailed = results.value.some((r) => r.syncStatus === 'FAILED')
  if (hasFailed) return 'error'
  // 全部失败或有失败/未映射
  if (stats.total > 0 && stats.success === 0) return 'error'
  if (stats.fail > 0 || stats.unmapped > 0) return 'warning'
  return 'success'
})

const resultTitle = computed(() => {
  const status = resultStatus.value
  if (status === 'error') return '同步失败'
  if (status === 'warning') return '同步完成（部分异常）'
  return '同步完成'
})

function show() {
  open.value = true
  step.value = 1
  syncScope.value = 'all'
  selectedShopId.value = undefined
  results.value = []
}

async function handleSync() {
  if (syncScope.value === 'single' && !selectedShopId.value) {
    message.warning('请选择店铺')
    return
  }

  loading.value = true
  try {
    const shopId = syncScope.value === 'single' ? selectedShopId.value : undefined
    const res = await syncFboStock(shopId)
    if (isSuccess(res)) {
      results.value = res.data || []
      step.value = 2
      emit('success')
    }
  } finally {
    loading.value = false
  }
}

function handleViewLog() {
  open.value = false
  emit('viewLog')
}

function handleClose() {
  open.value = false
}

defineExpose({ show })
</script>
