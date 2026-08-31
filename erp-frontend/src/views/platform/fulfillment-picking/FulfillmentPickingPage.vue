<template>
  <div class="picking-page">
    <a-card :bordered="false" class="search-card">
      <a-form layout="inline" :model="query" class="picking-search">
        <a-form-item label="拣货任务号">
          <a-input v-model:value="query.taskNo" allow-clear placeholder="请输入" style="width: 190px" />
        </a-form-item>
        <a-form-item label="所属仓库">
          <a-select
            v-model:value="query.warehouseId"
            allow-clear
            placeholder="全部"
            :options="warehouseOptions"
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item label="任务状态">
          <a-select
            v-model:value="query.taskStatus"
            allow-clear
            placeholder="全部"
            :options="statusOptions"
            style="width: 130px"
          />
        </a-form-item>
        <a-form-item label="拣货员">
          <user-select
            v-model:value="query.operatorId"
            placeholder="全部"
            :options="userOptions"
            :loading="usersLoading"
            style="width: 140px"
          />
        </a-form-item>
        <a-form-item label="创建时间">
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

    <a-card title="拣货任务" :bordered="false" class="list-card">
      <template #extra><a-button @click="load">刷新</a-button></template>
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
              <a v-if="canRelease(record)" @click="handleRelease(record)">释放任务</a>
              <a v-if="isActive(record)" @click="openTransfer(record)">转交</a>
              <a
                :class="{ 'disabled-link': !canOpenSimple(record) }"
                @click="canOpenSimple(record) && openSimplified(record)"
              >整单作业</a>
            </operation-group>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="transferOpen" title="转交拣货任务" @ok="handleTransfer">
      <a-form layout="vertical">
        <a-form-item label="目标拣货员" required>
          <user-select
            v-model:value="targetOperatorId"
            :options="userOptions"
            :loading="usersLoading"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

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
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { isSuccess } from '@/api'
import {
  claimFulfillmentPickTask,
  listFulfillmentPickTasks,
  releaseFulfillmentPickTask,
  startSimplifiedFulfillmentTask,
  transferFulfillmentPickTask
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
import { canOpenSimplifiedTask } from './simplified-task-flow'
import { createReactivationRefresh } from './reactivation-refresh'

defineOptions({ name: 'FulfillmentPickingPage' })
const router = useRouter()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.userId)
const { allUsers: userOptions, loading: usersLoading, loadAllUsers, getUserName } = useUserData()
const loading = ref(false)
const tasks = ref<FulfillmentPickTask[]>([])
const warehouses = ref<{ id: number; warehouseName: string }[]>([])
const dateRange = ref<[string, string]>()
const query = reactive<FulfillmentPickTaskQuery>({})
const transferOpen = ref(false)
const transferTaskId = ref<number>()
const targetOperatorId = ref<number>()
const simplifiedOpen = ref(false)
const simplifiedTask = ref<FulfillmentPickTask>()

const statusText: Record<string, string> = {
  PENDING: '待领取',
  PICKING: '拣货中',
  PARTIAL_EXCEPTION: '部分异常',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}
const statusColor: Record<string, string> = {
  PENDING: 'orange',
  PICKING: 'blue',
  PARTIAL_EXCEPTION: 'red',
  COMPLETED: 'green',
  CANCELLED: 'default'
}
const statusOptions = Object.entries(statusText).map(([value, label]) => ({ value, label }))
const warehouseOptions = computed(() =>
  warehouses.value.map(item => ({ value: item.id, label: item.warehouseName }))
)
const columns = [
  { title: '任务号', dataIndex: 'taskNo', width: 230, fixed: 'left' as const },
  { title: '所属仓库', key: 'warehouse', width: 150 },
  { title: '订单数', dataIndex: 'orderCount', width: 90 },
  { title: '总件数', dataIndex: 'totalQuantity', width: 90 },
  { title: '完成进度', key: 'progress', width: 110 },
  { title: '状态', key: 'status', width: 120 },
  { title: '拣货员', key: 'operator', width: 120 },
  { title: '领取时间', dataIndex: 'claimedTime', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'operate', width: 260, fixed: 'right' as const }
]

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
  warehouses.value.find(item => item.id === id)?.warehouseName || `仓库 #${id}`
const progressText = (record: FulfillmentPickTask) => {
	const progress = `${record.completedOrderCount || 0} / ${record.orderCount}`
	return record.exceptionOrderCount ? `${progress}（异常 ${record.exceptionOrderCount}）` : progress
}
const action = (record: FulfillmentPickTask) =>
  primaryTaskAction(record.taskStatus, record.operatorId, currentUserId.value)
const primaryText = (record: FulfillmentPickTask) =>
  record.operationMode === 'SIMPLE' && action(record) === 'work'
    ? '整单作业'
    : ({ claim: '领取任务', work: '继续作业', view: '查看' })[action(record)]
const isActive = (record: FulfillmentPickTask) =>
  ['PENDING', 'PICKING', 'PARTIAL_EXCEPTION'].includes(record.taskStatus)
const canRelease = (record: FulfillmentPickTask) =>
  canReleaseTask(record.taskStatus, record.operatorId, currentUserId.value)
const canOpenSimple = (record: FulfillmentPickTask) =>
  canOpenSimplifiedTask(record.taskStatus, record.operationMode, record.operatorId, currentUserId.value)
const openSimplified = async (record: FulfillmentPickTask) => {
  const result = await startSimplifiedFulfillmentTask(record.id)
  if (!isSuccess(result)) return
  record.operationMode = 'SIMPLE'
  simplifiedTask.value = record
  simplifiedOpen.value = true
}

const handlePrimary = async (record: FulfillmentPickTask) => {
  if (action(record) === 'claim') {
    const result = await claimFulfillmentPickTask(record.id)
    if (!isSuccess(result)) return
    message.success('任务领取成功')
  }
  if (record.operationMode === 'SIMPLE' && action(record) === 'work') {
    openSimplified(record)
    return
  }
  await router.push(`/ops/fulfillment-picking/work/${record.id}`)
}
const handleRelease = async (record: FulfillmentPickTask) => {
  const result = await releaseFulfillmentPickTask(record.id)
  if (isSuccess(result)) {
    message.success('任务已释放')
    await load()
  }
}
const openTransfer = (record: FulfillmentPickTask) => {
  transferTaskId.value = record.id
  targetOperatorId.value = record.operatorId
  transferOpen.value = true
}
const handleTransfer = async () => {
  if (!transferTaskId.value || !targetOperatorId.value) return message.warning('请选择目标拣货员')
  const result = await transferFulfillmentPickTask(transferTaskId.value, targetOperatorId.value)
  if (isSuccess(result)) {
    transferOpen.value = false
    message.success('任务已转交')
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
.disabled-link { color: rgba(0, 0, 0, .25); cursor: not-allowed; }
</style>
