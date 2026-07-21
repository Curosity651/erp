<template>
  <a-drawer v-model:open="open" :title="`货主详情 - ${current?.tenantName || ''}`" :width="560">
    <a-descriptions :column="1" bordered size="small">
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
  </a-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { TenantBrief } from '@/api/tenant/types'

const open = ref(false)
const current = ref<TenantBrief>()

function openDetail(record: TenantBrief) {
  current.value = record
  open.value = true
}

defineExpose({ openDetail })
</script>

<script lang="ts">
export default {
  name: 'ClientDetailDrawer'
}
</script>
