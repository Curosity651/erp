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

    <a-card :bordered="false" title="出库作业" class="list-card">
      <template #extra>
        <a-space>
          <a-tag v-if="demoMode">演示数据</a-tag>
          <a-tag :color="reviewTag.color">{{ reviewTag.text }}</a-tag>
          <a-button
            type="primary"
            :disabled="demoMode || !summary?.allProcessed || summary?.reviewCurrent"
            @click="confirmOpen = true"
          >
            {{ summary?.reviewCurrent ? '已完成复核' : '完成复核' }}
          </a-button>
          <a-button @click="load">刷新</a-button>
        </a-space>
      </template>

      <div class="workbench-summary">
        <div class="summary-item">
          <span>任务总数</span>
          <strong>{{ summary?.totalTaskCount || 0 }}</strong>
        </div>
        <div class="summary-item">
          <span>已完成</span>
          <strong>{{ summary?.completedTaskCount || 0 }}</strong>
        </div>
        <div class="summary-item">
          <span>已取消</span>
          <strong>{{ summary?.cancelledTaskCount || 0 }}</strong>
        </div>
        <div class="summary-item" :class="{ danger: summary?.unprocessedTaskCount }">
          <span>未处理完成</span>
          <strong>{{ summary?.unprocessedTaskCount || 0 }}</strong>
        </div>
        <div class="summary-item" :class="{ danger: summary?.exceptionTaskCount }">
          <span>异常任务</span>
          <strong>{{ summary?.exceptionTaskCount || 0 }}</strong>
        </div>
      </div>

      <div v-if="summary?.reviewNo" class="review-record">
        <span><em>复核单号</em>{{ summary.reviewNo }}</span>
        <span>
          <em>复核人员</em>{{ summary.reviewedBy ? getUserName(summary.reviewedBy) : '-' }}
        </span>
        <span><em>复核时间</em>{{ summary.reviewedTime || '-' }}</span>
      </div>

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
            {{ record.operatorId ? operatorName(record.operatorId) : '-' }}
          </template>
          <template v-else-if="column.key === 'exception'">
            <span :class="{ danger: record.exceptionOrderCount }">
              {{ record.exceptionOrderCount || 0 }}
            </span>
          </template>
          <template v-else-if="column.key === 'operate'">
            <operation-group>
              <a @click="openDetail(record)">查看</a>
            </operation-group>
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
            {{ taskDetail.task.operatorId ? operatorName(taskDetail.task.operatorId) : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="订单数">{{ taskDetail.task.orderCount }}</a-descriptions-item>
          <a-descriptions-item label="货物件数">{{
            taskDetail.task.totalQuantity
          }}</a-descriptions-item>
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
import { OperationGroup } from '@/components/Operation'
import { createReactivationRefresh } from '../fulfillment-picking/reactivation-refresh'
import { demoOperatorName, demoWarehouseName, resolveOutboundWorkbenchData } from './workbench-flow'

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
const demoMode = ref(false)
const demoDetails = ref<Record<number, FulfillmentPickTaskDetail>>({})
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
  warehouses.value.find(item => item.id === id)?.warehouseName ||
  demoWarehouseName(id) ||
  `仓库 ${id}`

const operatorName = (id: number) => demoOperatorName(id) || getUserName(id)

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
    demoMode.value = false
    demoDetails.value = {}
    if (isSuccess(taskResult) && isSuccess(summaryResult) && summaryResult.data) {
      const resolved = resolveOutboundWorkbenchData({
        devMode: import.meta.env.DEV,
        workDate: workDate.value,
        warehouseId: query.warehouseId,
        taskFilter: query,
        tasks: taskResult.data || [],
        summary: summaryResult.data
      })
      tasks.value = resolved.tasks
      summary.value = resolved.summary
      demoMode.value = resolved.demoMode
      demoDetails.value = resolved.details
      reviewRemark.value = resolved.summary.remark || ''
      return
    }
    if (isSuccess(taskResult)) tasks.value = taskResult.data || []
    if (isSuccess(summaryResult)) summary.value = summaryResult.data
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
  if (demoMode.value) return
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
  const demoDetail = demoDetails.value[task.id]
  if (demoMode.value && demoDetail) {
    taskDetail.value = demoDetail
    detailLoading.value = false
    return
  }
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
.review-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}
.review-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
}
.review-search :deep(.ant-form-item) {
  margin: 0;
  flex: 0 0 auto;
}
.review-search :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
}
.search-actions-item {
  margin-left: auto !important;
}
.list-card,
.list-card :deep(.ant-card-body) {
  min-width: 0;
}
.list-card :deep(.ant-card-body) {
  overflow: hidden;
}
.workbench-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 28px;
  padding: 4px 0 16px;
  border-bottom: 1px solid #f0f0f0;
}
.summary-item {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  white-space: nowrap;
}
.summary-item span {
  color: rgba(0, 0, 0, 0.45);
}
.summary-item strong {
  color: rgba(0, 0, 0, 0.88);
  font-size: 18px;
  line-height: 1;
}
.summary-item.danger strong {
  color: #cf1322;
}
.review-record {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 28px;
  padding: 12px 0;
  color: rgba(0, 0, 0, 0.88);
  border-bottom: 1px solid #f0f0f0;
}
.review-record em {
  margin-right: 8px;
  color: rgba(0, 0, 0, 0.45);
  font-style: normal;
}
.workbench-summary + :deep(.ant-table-wrapper),
.review-record + :deep(.ant-table-wrapper) {
  margin-top: 16px;
}
.confirm-remark {
  margin-top: 20px;
  margin-bottom: 0;
}
.detail-table {
  margin-top: 16px;
}
.danger {
  color: #cf1322;
  font-weight: 600;
}
@media (max-width: 767px) {
  .workbench-summary {
    gap: 12px 20px;
  }
  .review-record {
    flex-direction: column;
    gap: 6px;
  }
}
</style>
