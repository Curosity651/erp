<template>
  <a-drawer v-model:open="open" title="FBO 同步日志" :width="960" @close="handleClose">
    <pro-table
      ref="tableRef"
      row-key="id"
      :columns="columns"
      :request="tableRequest"
      :pagination="{ pageSize: 10 }"
      :card-props="{ bodyStyle: { padding: 0 } }"
    >
      <!-- 筛选区域 -->
      <template #headerTitle>
        <div class="flex flex-wrap gap-3 mb-2">
          <ShopSelectInput
            v-model="searchParams.shopId"
            default-platform="ozon"
            :hide-platform-filter="true"
            placeholder="选择店铺"
            style="width: 200px"
          />
          <a-select
            v-model:value="searchParams.syncType"
            placeholder="同步类型"
            allow-clear
            style="width: 120px"
            :options="syncTypeOptions"
          />
          <a-select
            v-model:value="searchParams.syncStatus"
            placeholder="同步状态"
            allow-clear
            style="width: 120px"
            :options="syncStatusOptions"
          />
          <a-button type="primary" @click="handleSearch">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
        </div>
      </template>

      <!-- 自定义列渲染 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'syncStatus'">
          <a-tag :color="getStatusColor(record.syncStatus)">
            {{ getStatusText(record.syncStatus) }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'syncType'">
          {{ record.syncType === 'SCHEDULED' ? '定时同步' : '手动同步' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="handleViewDetail(record.id)">查看</a-button>
        </template>
      </template>
    </pro-table>

    <!-- 详情抽屉 -->
    <FboSyncLogDetailDrawer ref="detailDrawerRef" />
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick } from 'vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { getFboSyncLogPage } from '@/api/wms/fbo'
import type { FboSyncLogQO } from '@/api/wms/fbo/types'
import { mergePageParam } from '@/utils/page-utils'
import ShopSelectInput from '@/components/ShopSelectInput.vue'
import FboSyncLogDetailDrawer from './FboSyncLogDetailDrawer.vue'

defineOptions({ name: 'FboSyncLogDrawer' })

const open = ref(false)
const tableRef = ref<ProTableInstanceExpose>()
const detailDrawerRef = ref<InstanceType<typeof FboSyncLogDetailDrawer>>()
const searchParams = reactive<FboSyncLogQO>({})

const syncTypeOptions = [
  { label: '定时同步', value: 'SCHEDULED' },
  { label: '手动同步', value: 'MANUAL' }
]
const syncStatusOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '部分成功', value: 'PARTIAL' },
  { label: '失败', value: 'FAILED' }
]

const columns: ProColumns[] = [
  { title: '日志编号', dataIndex: 'logNo', width: 180 },
  { title: '店铺', dataIndex: 'shopName', width: 120 },
  { title: '同步类型', key: 'syncType', width: 100 },
  { title: 'SKU总数', dataIndex: 'totalCount', width: 80, align: 'right' },
  { title: '成功', dataIndex: 'successCount', width: 60, align: 'right' },
  { title: '失败', dataIndex: 'failCount', width: 60, align: 'right' },
  { title: '未映射', dataIndex: 'unmappedCount', width: 70, align: 'right' },
  { title: '状态', key: 'syncStatus', width: 100 },
  { title: '同步时间', dataIndex: 'syncTime', width: 160 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

function getStatusColor(status: string) {
  return status === 'SUCCESS' ? 'success' : status === 'PARTIAL' ? 'warning' : 'error'
}

function getStatusText(status: string) {
  return status === 'SUCCESS' ? '成功' : status === 'PARTIAL' ? '部分成功' : '失败'
}

const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return getFboSyncLogPage({ ...pageParam, ...searchParams })
}

function handleSearch() {
  tableRef.value?.actionRef?.reload(true)
}

function handleReset() {
  Object.keys(searchParams).forEach(k => delete (searchParams as Record<string, unknown>)[k])
  handleSearch()
}

function handleViewDetail(id: number) {
  detailDrawerRef.value?.show(id)
}

function show() {
  open.value = true
  nextTick(() => tableRef.value?.actionRef?.reload(true))
}

function handleClose() {
  open.value = false
}

defineExpose({ show })
</script>
