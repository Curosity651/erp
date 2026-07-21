<template>
  <a-drawer v-model:open="open" :title="`服务商详情 - ${current?.tenantName || ''}`" width="640">
    <a-descriptions :column="1" bordered size="small" style="margin-bottom: 16px">
      <a-descriptions-item label="编码">{{ current?.tenantCode }}</a-descriptions-item>
      <a-descriptions-item label="名称">{{ current?.tenantName }}</a-descriptions-item>
      <a-descriptions-item label="联系人">{{ current?.contactName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="联系电话">{{ current?.contactPhone || '-' }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="current?.status === 1 ? 'green' : 'red'">
          {{ current?.status === 1 ? '启用' : '停用' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ current?.createTime || '-' }}</a-descriptions-item>
    </a-descriptions>

    <a-divider orientation="left" style="margin: 8px 0">
      名下货主（{{ clients.length }}）
    </a-divider>
    <a-table
      :columns="clientColumns"
      :data-source="clients"
      :loading="loading"
      row-key="id"
      size="small"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { isSuccess } from '@/api'
import { listErpTenantsByParent } from '@/api/tenant'
import type { TenantBrief } from '@/api/tenant/types'

const clientColumns = [
  { title: '编码', dataIndex: 'tenantCode', key: 'tenantCode' },
  { title: '货主名称', dataIndex: 'tenantName', key: 'tenantName' },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName' },
  { title: '状态', dataIndex: 'status', key: 'status' }
]

const open = ref(false)
const loading = ref(false)
const current = ref<TenantBrief>()
const clients = ref<TenantBrief[]>([])

async function openDetail(record: TenantBrief) {
  current.value = record
  open.value = true
  loading.value = true
  clients.value = []
  try {
    const res = await listErpTenantsByParent(record.id)
    if (isSuccess(res)) clients.value = res.data || []
  } finally {
    loading.value = false
  }
}

defineExpose({ openDetail })
</script>

<script lang="ts">
export default {
  name: 'WmsOperatorDetailDrawer'
}
</script>
