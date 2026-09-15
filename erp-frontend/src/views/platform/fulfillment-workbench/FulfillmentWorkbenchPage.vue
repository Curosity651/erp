<template>
  <div class="review-page">
    <a-card :bordered="false" class="search-card">
      <a-form layout="inline" :model="query" class="review-search">
        <a-form-item label="作业日期">
          <a-date-picker
            v-model:value="workDate"
            value-format="YYYY-MM-DD"
            :allow-clear="false"
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item label="仓库">
          <a-select
            v-model:value="query.warehouseId"
            :options="warehouseOptions"
            allow-clear
            placeholder="全部"
            style="width: 170px"
          />
        </a-form-item>
        <a-form-item label="拣货任务号">
          <a-input
            v-model:value="query.taskNo"
            allow-clear
            placeholder="请输入"
            style="width: 210px"
          />
        </a-form-item>
        <a-form-item label="任务状态">
          <a-select
            v-model:value="query.taskStatus"
            :options="statusOptions"
            allow-clear
            placeholder="全部"
            style="width: 140px"
          />
        </a-form-item>
        <a-form-item label="拣货人员">
          <user-select
            v-model:value="query.operatorId"
            :options="userOptions"
            :loading="usersLoading"
            allow-clear
            placeholder="全部"
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item class="search-actions-item">
          <search-actions :loading="loading" @search="load" @reset="reset" />
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :bordered="false" title="当日出库复核">
      <template #extra>
        <a-space>
          <a-tag :color="reviewTag.color">{{ reviewTag.text }}</a-tag>
          <a-button
            type="primary"
            :disabled="!summary?.allProcessed || summary?.reviewCurrent"
            @click="confirmOpen = true"
          >
            {{ summary?.reviewCurrent ? '已完成复核' : '完成复核' }}
          </a-button>
        </a-space>
      </template>

      <a-row :gutter="16" class="summary-row">
        <a-col :xs="12" :sm="8" :lg="4">
          <a-statistic title="任务总数" :value="summary?.totalTaskCount || 0" />
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-statistic title="已完成" :value="summary?.completedTaskCount || 0" />
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-statistic title="已取消" :value="summary?.cancelledTaskCount || 0" />
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-statistic
            title="未处理完成"
            :value="summary?.unprocessedTaskCount || 0"
            :value-style="summary?.unprocessedTaskCount ? { color: '#cf1322' } : undefined"
          />
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-statistic
            title="异常任务"
            :value="summary?.exceptionTaskCount || 0"
            :value-style="summary?.exceptionTaskCount ? { color: '#cf1322' } : undefined"
          />
        </a-col>
        <a-col :xs="12" :sm="8" :lg="4">
          <a-statistic title="已复核" :value="summary?.reviewCurrent ? 1 : 0" />
        </a-col>
      </a-row>

      <a-descriptions v-if="summary?.reviewNo" size="small" :column="3" class="review-record">
        <a-descriptions-item label="复核单号">{{ summary.reviewNo }}</a-descriptions-item>
        <a-descriptions-item label="复核人员">
          {{ summary.reviewedBy ? getUserName(summary.reviewedBy) : '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="复核时间">{{ summary.reviewedTime || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card :bordered="false" title="当日拣货任务">
      <template #extra><a-button @click="load">刷新</a-button></template>
      <a-table
        row-key="id"
        :loading="loading"
        :data-source="tasks"
        :columns="columns"
        :pagination="{ pageSize: 20, showSizeChanger: true }"
        :scroll="{ x: 1280 }"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'warehouse'">
            {{ warehouseName(record.warehouseId) }}
          </template>
          <template v-else-if="column.key === 'progress'">
            {{ record.completedOrderCount || 0 }} / {{ record.orderCount || 0 }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="taskStatusMeta(record.taskStatus).color">
              {{ taskStatusMeta(record.taskStatus).text }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'operator'">
            {{ record.operatorId ? getUserName(record.operatorId) : '-' }}
          </template>
          <template v-else-if="column.key === 'exception'">
            <span :class="{ danger: record.exceptionOrderCount }">
              {{ record.exceptionOrderCount || 0 }}
            </span>
          </template>
          <template v-else-if="column.key === 'operate'">
            <a @click="openDetail(record)">查看</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="confirmOpen"
      title="完成出库复核"
      :confirm-loading="confirming"
      ok-text="确认复核完成"
      @ok="confirmReview"
    >
      <a-descriptions :column="1" size="small" bordered>
        <a-descriptions-item label="作业日期">{{ workDate }}</a-descriptions-item>
        <a-descriptions-item label="仓库范围">
          {{ query.warehouseId ? warehouseName(query.warehouseId) : '全部仓库' }}
        </a-descriptions-item>
        <a-descriptions-item label="任务结果">
          共 {{ summary?.totalTaskCount || 0 }} 张，已完成
          {{ summary?.completedTaskCount || 0 }} 张，已取消
          {{ summary?.cancelledTaskCount || 0 }} 张
        </a-descriptions-item>
      </a-descriptions>
      <a-form-item label="复核备注" class="confirm-remark">
        <a-textarea v-model:value="reviewRemark" :rows="3" :maxlength="500" show-count />
      </a-form-item>
    </a-modal>

    <a-drawer v-model:open="detailOpen" title="拣货任务明细" width="860">
      <a-spin :spinning="detailLoading">
        <a-descriptions v-if="taskDetail" :column="2" bordered size="small">
          <a-descriptions-item label="任务号">{{ taskDetail.task.taskNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            {{ taskStatusMeta(taskDetail.task.taskStatus).text }}
          </a-descriptions-item>
          <a-descriptions-item label="仓库">
            {{ warehouseName(taskDetail.task.warehouseId) }}
          </a-descriptions-item>
          <a-descriptions-item label="拣货人员">
            {{ taskDetail.task.operatorId ? getUserName(taskDetail.task.operatorId) : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="订单数">{{ taskDetail.task.orderCount }}</a-descriptions-item>
          <a-descriptions-item label="货物件数">{{ taskDetail.task.totalQuantity }}</a-descriptions-item>
        </a-descriptions>

        <a-table
          v-if="taskDetail"
          row-key="taskOrder.id"
          :data-source="taskDetail.orderQueue"
          :columns="detailColumns"
          :pagination="false"
          :scroll="{ x: 760 }"
          class="detail-table"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'orderNo'">
              {{ record.fulfillmentOrder.fulfillmentNo }}
            </template>
            <template v-else-if="column.key === 'sourceOrder'">
              {{ record.fulfillmentOrder.sourceOrderNo }}
            </template>
            <template v-else-if="column.key === 'goods'">
              {{ record.skuCount }} 种 / {{ record.totalQuantity }} 件
            </template>
            <template v-else-if="column.key === 'picked'">
              {{ record.pickedQuantity }} / {{ record.totalQuantity }}
            </template>
            <template v-else-if="column.key === 'orderStatus'">
              {{ orderStatusText(record.taskOrder.orderStatus) }}
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import {
  confirmOutboundPickReview,
  getFulfillmentPickTask,
  getOutboundPickReviewSummary,
  listFulfillmentPickTasks
} from '@/api/wms/fulfillment'
import type {
  FulfillmentPickTask,
  FulfillmentPickTaskDetail,
  FulfillmentPickTaskQuery,
  OutboundPickReviewSummary
} from '@/api/wms/fulfillment/types'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import { SearchActions } from '@/components/Search'
import UserSelect from '@/components/Lov/UserSelect.vue'
import { useUserData } from '@/hooks/use-user-data'
import { createReactivationRefresh } from '../fulfillment-picking/reactivation-refresh'

defineOptions({ name: 'FulfillmentWorkbenchPage' })

const loading = ref(false)
const confirming = ref(false)
const confirmOpen = ref(false)
const detailOpen = ref(false)
const detailLoading = ref(false)
const workDate = ref(dayjs().format('YYYY-MM-DD'))
const reviewRemark = ref('')
const tasks = ref<FulfillmentPickTask[]>([])
const summary = ref<OutboundPickReviewSummary>()
const taskDetail = ref<FulfillmentPickTaskDetail>()
const warehouses = ref<{ id: number; warehouseName: string }[]>([])
const query = reactive<FulfillmentPickTaskQuery>({})
const { allUsers: userOptions, loading: usersLoading, loadAllUsers, getUserName } = useUserData()

const statusMeta: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待领取', color: 'orange' },
  PICKING: { text: '拣货中', color: 'blue' },
  PARTIAL_EXCEPTION: { text: '存在异常', color: 'red' },
  COMPLETED: { text: '已完成', color: 'green' },
  CANCELLED: { text: '已取消', color: 'default' }
}

const statusOptions = Object.entries(statusMeta).map(([value, item]) => ({
  value,
  label: item.text
}))
const warehouseOptions = computed(() =>
  warehouses.value.map(item => ({ value: item.id, label: item.warehouseName }))
)
const reviewTag = computed(() => {
  if (summary.value?.reviewCurrent) return { text: '已完成复核', color: 'green' }
  if (!summary.value?.totalTaskCount) return { text: '当日暂无任务', color: 'default' }
  if (summary.value.allProcessed) return { text: '全部处理完成，可以复核', color: 'green' }
  return { text: `还有 ${summary.value.unprocessedTaskCount} 张未完成`, color: 'red' }
})

const columns = [
  { title: '拣货任务号', dataIndex: 'taskNo', width: 230, fixed: 'left' as const },
  { title: '仓库', key: 'warehouse', width: 160 },
  { title: '订单数', dataIndex: 'orderCount', width: 90 },
  { title: '货物件数', dataIndex: 'totalQuantity', width: 100 },
  { title: '订单完成进度', key: 'progress', width: 130 },
  { title: '异常订单', key: 'exception', width: 100 },
  { title: '任务状态', key: 'status', width: 120 },
  { title: '拣货人员', key: 'operator', width: 130 },
  { title: '领取时间', dataIndex: 'claimedTime', width: 180 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', key: 'operate', width: 80, fixed: 'right' as const }
]

const detailColumns = [
  { title: '履约单号', key: 'orderNo', width: 190 },
  { title: '平台订单', key: 'sourceOrder', width: 180 },
  { title: '货物', key: 'goods', width: 110 },
  { title: '拣货进度', key: 'picked', width: 110 },
  { title: '处理状态', key: 'orderStatus', width: 110 }
]

const taskStatusMeta = (status: string) =>
  statusMeta[status] || { text: status || '-', color: 'default' }

const orderStatusText = (status: string) =>
  ({
    PENDING: '待处理',
    PICKING: '处理中',
    WAITING_LABEL: '等待打包',
    COMPLETED: '已完成',
    EXCEPTION: '异常',
    CANCELLED: '已取消'
  })[status] || status

const warehouseName = (id: number) =>
  warehouses.value.find(item => item.id === id)?.warehouseName || `仓库 ${id}`

const load = async () => {
  loading.value = true
  try {
    const startTime = `${workDate.value} 00:00:00`
    const endTime = `${workDate.value} 23:59:59`
    const [taskResult, summaryResult] = await Promise.all([
      listFulfillmentPickTasks({ ...query, startTime, endTime }),
      getOutboundPickReviewSummary({
        workDate: workDate.value,
        warehouseId: query.warehouseId
      })
    ])
    if (isSuccess(taskResult)) tasks.value = taskResult.data || []
    if (isSuccess(summaryResult)) {
      summary.value = summaryResult.data
      reviewRemark.value = summaryResult.data?.remark || ''
    }
  } finally {
    loading.value = false
  }
}

const reset = () => {
  workDate.value = dayjs().format('YYYY-MM-DD')
  Object.assign(query, {
    warehouseId: undefined,
    taskNo: undefined,
    taskStatus: undefined,
    operatorId: undefined
  })
  load()
}

const confirmReview = async () => {
  confirming.value = true
  try {
    const result = await confirmOutboundPickReview({
      workDate: workDate.value,
      warehouseId: query.warehouseId,
      remark: reviewRemark.value || undefined
    })
    if (isSuccess(result)) {
      message.success('当日拣货任务复核完成')
      confirmOpen.value = false
      await load()
    }
  } finally {
    confirming.value = false
  }
}

const openDetail = async (task: FulfillmentPickTask) => {
  detailOpen.value = true
  detailLoading.value = true
  taskDetail.value = undefined
  try {
    const result = await getFulfillmentPickTask(task.id)
    if (isSuccess(result)) taskDetail.value = result.data
  } finally {
    detailLoading.value = false
  }
}

const refreshOnReactivation = createReactivationRefresh(load)
onActivated(refreshOnReactivation)
onMounted(async () => {
  const warehouseResult = await getWarehouseOptions()
  if (isSuccess(warehouseResult)) warehouses.value = warehouseResult.data || []
  await loadAllUsers()
  await load()
})
</script>

<style scoped>
.review-page { display: grid; gap: 16px; min-width: 0; }
.review-search { display: flex; flex-wrap: wrap; align-items: center; gap: 16px 20px; }
.review-search :deep(.ant-form-item) { margin: 0; flex: 0 0 auto; }
.review-search :deep(.ant-form-item-row) { flex-wrap: nowrap; }
.search-actions-item { margin-left: auto !important; }
.summary-row { padding: 8px 0 16px; }
.review-record { padding-top: 12px; border-top: 1px solid #f0f0f0; }
.confirm-remark { margin-top: 20px; margin-bottom: 0; }
.detail-table { margin-top: 16px; }
.danger { color: #cf1322; font-weight: 600; }
</style>
