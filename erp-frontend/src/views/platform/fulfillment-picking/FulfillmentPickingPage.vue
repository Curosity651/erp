<template>
  <div class="picking-page">
    <a-card :bordered="false" class="search-card">
      <a-form layout="inline" :model="query" class="picking-search">
        <a-form-item :label="t('platform.picking.taskNo')">
          <a-input v-model:value="query.taskNo" allow-clear :placeholder="t('platform.common.enter')" style="width: 190px" />
        </a-form-item>
        <a-form-item :label="t('platform.picking.warehouse')">
          <a-select
            v-model:value="query.warehouseId"
            allow-clear
            :placeholder="t('platform.common.all')"
            :options="warehouseOptions"
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item :label="t('platform.picking.status')">
          <a-select
            v-model:value="query.taskStatus"
            allow-clear
            :placeholder="t('platform.common.all')"
            :options="statusOptions"
            style="width: 130px"
          />
        </a-form-item>
        <a-form-item :label="t('platform.picking.operator')">
          <user-select
            v-model:value="query.operatorId"
            :placeholder="t('platform.common.all')"
            :options="userOptions"
            :loading="usersLoading"
            style="width: 140px"
          />
        </a-form-item>
        <a-form-item :label="t('platform.picking.createdAt')">
          <a-range-picker
            v-model:value="dateRange"
            value-format="YYYY-MM-DD"
            style="width: 230px"
          />
        </a-form-item>
        <a-form-item class="search-actions-item">
          <search-actions :loading="loading" @search="load" @reset="reset" />
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :title="t('platform.picking.title')" :bordered="false" class="list-card">
      <template #extra><a-button @click="load">{{ t('platform.common.refresh') }}</a-button></template>
      <a-table
        row-key="id"
        :loading="loading"
        :data-source="tasks"
        :columns="columns"
        :pagination="{ pageSize: 20, showSizeChanger: true }"
        :scroll="{ x: 1420 }"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'warehouse'">
            {{ warehouseName(record.warehouseId) }}
          </template>
          <template v-else-if="column.key === 'progress'">
            {{ progressText(record) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor[record.taskStatus]">
              {{ statusText[record.taskStatus] || record.taskStatus }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'operator'">
            {{ record.operatorId ? getUserName(record.operatorId) : '-' }}
          </template>
          <template v-else-if="column.key === 'operate'">
            <operation-group>
              <a @click="handlePrimary(record)">{{ primaryText(record) }}</a>
              <a v-if="canRelease(record)" @click="handleRelease(record)">{{ t('platform.picking.release') }}</a>
            </operation-group>
          </template>
        </template>
      </a-table>
    </a-card>

    <simplified-task-modal
      v-model:open="simplifiedOpen"
      :task="simplifiedTask"
      :warehouse-name="simplifiedTask ? warehouseName(simplifiedTask.warehouseId) : '-'"
      @success="load"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  claimFulfillmentPickTask,
  listFulfillmentPickTasks,
  releaseFulfillmentPickTask,
  startSimplifiedFulfillmentTask
} from '@/api/wms/fulfillment'
import type {
  FulfillmentPickTask,
  FulfillmentPickTaskQuery
} from '@/api/wms/fulfillment/types'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import { useUserData } from '@/hooks/use-user-data'
import { useUserStore } from '@/stores/user-store'
import UserSelect from '@/components/Lov/UserSelect.vue'
import { SearchActions } from '@/components/Search'
import { OperationGroup } from '@/components/Operation'
import {
  canReleaseTask,
  primaryTaskAction,
  type PickingTaskStatus
} from './picking-task-flow'
import SimplifiedTaskModal from './SimplifiedTaskModal.vue'
import { createReactivationRefresh } from './reactivation-refresh'

defineOptions({ name: 'FulfillmentPickingPage' })
const { t } = useI18n()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.userId)
const { allUsers: userOptions, loading: usersLoading, loadAllUsers, getUserName } = useUserData()
const loading = ref(false)
const tasks = ref<FulfillmentPickTask[]>([])
const warehouses = ref<{ id: number; warehouseName: string }[]>([])
const dateRange = ref<[string, string]>()
const query = reactive<FulfillmentPickTaskQuery>({})
const simplifiedOpen = ref(false)
const simplifiedTask = ref<FulfillmentPickTask>()

const statusKeys: Record<string, string> = {
  PENDING: 'platform.picking.status.pending',
  PICKING: 'platform.picking.status.picking',
  PARTIAL_EXCEPTION: 'platform.picking.status.partialException',
  COMPLETED: 'platform.picking.status.completed',
  CANCELLED: 'platform.picking.status.cancelled'
}
const statusText = computed<Record<string, string>>(() =>
  Object.fromEntries(Object.entries(statusKeys).map(([status, key]) => [status, t(key)]))
)
const statusColor: Record<string, string> = {
  PENDING: 'orange',
  PICKING: 'blue',
  PARTIAL_EXCEPTION: 'red',
  COMPLETED: 'green',
  CANCELLED: 'default'
}
const statusOptions = computed(() =>
  Object.entries(statusText.value).map(([value, label]) => ({ value, label }))
)
const warehouseOptions = computed(() =>
  warehouses.value.map(item => ({ value: item.id, label: item.warehouseName }))
)
const columns = computed(() => [
  { title: t('platform.picking.taskNumber'), dataIndex: 'taskNo', width: 230, fixed: 'left' as const },
  { title: t('platform.picking.warehouse'), key: 'warehouse', width: 150 },
  { title: t('platform.picking.orderCount'), dataIndex: 'orderCount', width: 90 },
  { title: t('platform.picking.totalPieces'), dataIndex: 'totalQuantity', width: 90 },
  { title: t('platform.picking.progress'), key: 'progress', width: 110 },
  { title: t('platform.common.status'), key: 'status', width: 120 },
  { title: t('platform.picking.operator'), key: 'operator', width: 120 },
  { title: t('platform.picking.claimedAt'), dataIndex: 'claimedTime', width: 180 },
  { title: t('platform.common.createdAt'), dataIndex: 'createTime', width: 180 },
  { title: t('platform.common.operation'), key: 'operate', width: 180, fixed: 'right' as const }
])

const load = async () => {
  loading.value = true
  try {
    const result = await listFulfillmentPickTasks({
      ...query,
      startTime: dateRange.value?.[0] ? `${dateRange.value[0]} 00:00:00` : undefined,
      endTime: dateRange.value?.[1] ? `${dateRange.value[1]} 23:59:59` : undefined
    })
    if (isSuccess(result)) tasks.value = result.data || []
  } finally {
    loading.value = false
  }
}
const refreshOnReactivation = createReactivationRefresh(load)
onActivated(refreshOnReactivation)
const reset = () => {
  Object.assign(query, {
    taskNo: undefined,
    warehouseId: undefined,
    taskStatus: undefined,
    operatorId: undefined
  })
  dateRange.value = undefined
  load()
}
const warehouseName = (id: number) =>
  warehouses.value.find(item => item.id === id)?.warehouseName || t('platform.picking.warehouseFallback', { id })
const progressText = (record: FulfillmentPickTask) => {
	const progress = `${record.completedOrderCount || 0} / ${record.orderCount}`
	return record.exceptionOrderCount
    ? t('platform.picking.exceptionProgress', { progress, count: record.exceptionOrderCount })
    : progress
}
const action = (record: FulfillmentPickTask) =>
  primaryTaskAction(record.taskStatus, record.operatorId, currentUserId.value)
const primaryText = (record: FulfillmentPickTask) =>
  ({
    claim: t('platform.picking.action.claim'),
    work: t('platform.picking.action.work'),
    view: t('platform.picking.action.view')
  })[action(record)]
const canRelease = (record: FulfillmentPickTask) =>
  canReleaseTask(record.taskStatus, record.operatorId, currentUserId.value)
const openSimplified = async (record: FulfillmentPickTask, start = true) => {
  if (start) {
    const result = await startSimplifiedFulfillmentTask(record.id)
    if (!isSuccess(result)) return
    record.operationMode = 'SIMPLE'
  }
  simplifiedTask.value = record
  simplifiedOpen.value = true
}

const handlePrimary = async (record: FulfillmentPickTask) => {
  if (action(record) === 'claim') {
    const result = await claimFulfillmentPickTask(record.id)
    if (!isSuccess(result)) return
    message.success(t('platform.picking.claimed'))
    record.operatorId = currentUserId.value
    record.taskStatus = 'PICKING'
    record.operationMode = 'SIMPLE'
    await openSimplified(record)
    return
  }
  if (action(record) === 'work') {
    await openSimplified(record)
    return
  }
  await openSimplified(record, false)
}
const handleRelease = async (record: FulfillmentPickTask) => {
  const result = await releaseFulfillmentPickTask(record.id)
  if (isSuccess(result)) {
    message.success(t('platform.picking.released'))
    await load()
  }
}
onMounted(async () => {
  const warehouseResult = await getWarehouseOptions()
  if (isSuccess(warehouseResult)) warehouses.value = warehouseResult.data || []
  await loadAllUsers()
  await load()
})
</script>

<style scoped>
.picking-page { display: grid; gap: 16px; min-width: 0; }
.picking-search { display: flex; flex-wrap: wrap; align-items: center; gap: 16px 20px; }
.picking-search :deep(.ant-form-item) { margin: 0; flex: 0 0 auto; }
.picking-search :deep(.ant-form-item-row) { flex-wrap: nowrap; }
.search-actions-item { margin-left: auto !important; }
.list-card, .list-card :deep(.ant-card-body) { min-width: 0; }
.list-card :deep(.ant-card-body) { overflow: hidden; }
</style>
