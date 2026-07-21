<template>
  <a-drawer v-model:open="open" title="同步日志详情" :width="640" @close="handleClose">
    <a-spin :spinning="loading">
      <template v-if="detail">
        <!-- 基本信息 -->
        <a-descriptions title="基本信息" :column="2" bordered size="small" class="mb-4">
          <a-descriptions-item label="日志编号">{{ detail.logNo }}</a-descriptions-item>
          <a-descriptions-item label="同步类型">
            {{ detail.syncType === 'SCHEDULED' ? '定时同步' : '手动同步' }}
          </a-descriptions-item>
          <a-descriptions-item label="店铺">{{ detail.shopName }}</a-descriptions-item>
          <a-descriptions-item label="同步时间">{{ detail.syncTime }}</a-descriptions-item>
        </a-descriptions>

        <!-- 同步统计 -->
        <a-descriptions title="同步统计" :column="4" bordered size="small" class="mb-4">
          <a-descriptions-item label="SKU 总数">{{ detail.totalCount }}</a-descriptions-item>
          <a-descriptions-item label="成功">
            <span class="success-text">{{ detail.successCount }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="失败">
            <span class="error-text">{{ detail.failCount }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="未映射">
            <span class="warning-text">{{ detail.unmappedCount }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <div class="mb-4">
          <span class="mr-2">同步状态:</span>
          <a-tag :color="getStatusColor(detail.syncStatus)">
            {{ getStatusText(detail.syncStatus) }}
          </a-tag>
        </div>

        <!-- 错误信息 -->
        <a-alert
          v-if="detail.errorMessage"
          :message="detail.errorMessage"
          type="error"
          class="mb-4"
          show-icon
        />

        <!-- 失败明细 -->
        <template v-if="detail.failDetails?.length">
          <div class="font-medium mb-2">失败明细</div>
          <a-table
            :data-source="detail.failDetails"
            :columns="failColumns"
            :pagination="false"
            size="small"
            row-key="platformItemId"
          />
        </template>
      </template>
    </a-spin>

    <template #footer>
      <a-button @click="handleClose">关闭</a-button>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { isSuccess } from '@/api'
import { getFboSyncLogDetail } from '@/api/wms/fbo'
import type { FboSyncLogDetailVO } from '@/api/wms/fbo/types'

defineOptions({ name: 'FboSyncLogDetailDrawer' })

const open = ref(false)
const loading = ref(false)
const detail = ref<FboSyncLogDetailVO>()

const failColumns = [
  { title: '平台商品ID', dataIndex: 'platformItemId', key: 'platformItemId' },
  { title: '失败原因', dataIndex: 'message', key: 'message' }
]

function getStatusColor(status: string) {
  return status === 'SUCCESS' ? 'success' : status === 'PARTIAL' ? 'warning' : 'error'
}

function getStatusText(status: string) {
  return status === 'SUCCESS' ? '成功' : status === 'PARTIAL' ? '部分成功' : '失败'
}

async function loadDetail(id: number) {
  loading.value = true
  try {
    const res = await getFboSyncLogDetail(id)
    if (isSuccess(res)) {
      detail.value = res.data
    }
  } finally {
    loading.value = false
  }
}

function show(id: number) {
  open.value = true
  detail.value = undefined
  loadDetail(id)
}

function handleClose() {
  open.value = false
}

defineExpose({ show })
</script>

<style scoped>
.success-text {
  color: var(--ant-color-success);
}
.error-text {
  color: var(--ant-color-error);
}
.warning-text {
  color: var(--ant-color-warning);
}
</style>
