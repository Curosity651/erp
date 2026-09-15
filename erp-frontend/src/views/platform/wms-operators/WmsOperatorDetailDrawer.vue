<template>
  <a-drawer v-model:open="open" :title="t('platform.operator.detailTitle', { name: current?.tenantName || '' })" width="640">
    <a-descriptions :column="1" bordered size="small" style="margin-bottom: 16px">
      <a-descriptions-item :label="t('platform.operator.codeShort')">{{ current?.tenantCode }}</a-descriptions-item>
      <a-descriptions-item :label="t('platform.operator.nameShort')">{{ current?.tenantName }}</a-descriptions-item>
      <a-descriptions-item :label="t('platform.operator.contactName')">{{ current?.contactName || '-' }}</a-descriptions-item>
      <a-descriptions-item :label="t('platform.operator.contactPhone')">{{ current?.contactPhone || '-' }}</a-descriptions-item>
      <a-descriptions-item :label="t('platform.common.status')">
        <a-tag :color="current?.status === 1 ? 'green' : 'red'">
          {{ current?.status === 1 ? t('platform.operator.enabled') : t('platform.operator.disabled') }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item :label="t('platform.common.createdAt')">{{ current?.createTime || '-' }}</a-descriptions-item>
    </a-descriptions>

    <a-divider orientation="left" style="margin: 8px 0">
      {{ t('platform.operator.clients', { count: clients.length }) }}
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
            {{ record.status === 1 ? t('platform.operator.enabled') : t('platform.operator.disabled') }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { isSuccess } from '@/api'
import { listErpTenantsByParent } from '@/api/tenant'
import type { TenantBrief } from '@/api/tenant/types'

const { t } = useI18n()
const clientColumns = computed(() => [
  { title: t('platform.operator.codeShort'), dataIndex: 'tenantCode', key: 'tenantCode' },
  { title: t('platform.operator.ownerName'), dataIndex: 'tenantName', key: 'tenantName' },
  { title: t('platform.operator.contactName'), dataIndex: 'contactName', key: 'contactName' },
  { title: t('platform.common.status'), dataIndex: 'status', key: 'status' }
])

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
