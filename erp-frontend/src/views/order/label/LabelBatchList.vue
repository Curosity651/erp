<template>
  <div class="label-batch-list">
    <!-- 搜索和过滤区域 -->
    <div class="search-section">
      <a-space :size="12" wrap>
        <a-input
          v-model:value="queryParams.batchNo"
          placeholder="批次号"
          style="width: 200px"
          allow-clear
          @change="handleSearch"
        />
        <a-range-picker
          v-model:value="dateRange"
          :placeholder="['开始时间', '结束时间']"
          format="YYYY-MM-DD HH:mm:ss"
          show-time
          @change="handleDateChange"
        />
        <a-button @click="handleReset">重置</a-button>
      </a-space>
    </div>

    <!-- 批次列表表格 -->
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="batchId"
      @change="handleTableChange"
    >
      <!-- 自定义列渲染 -->
      <template #bodyCell="{ column, record }">
        <!-- 批次号列 -->
        <template v-if="column.key === 'batchNo'">
          <div class="batch-no">
            <span class="batch-no-text">{{ record.batchNo }}</span>
          </div>
        </template>

        <!-- 平台列 -->
        <template v-else-if="column.key === 'platform'">
          <a-tag color="blue">{{ record.platform }}</a-tag>
        </template>

        <!-- 订单总数列 -->
        <template v-else-if="column.key === 'totalOrders'">
          <div class="count-cell">
            <span class="count-number">{{ record.totalOrders }}</span>
            <span class="count-label">订单</span>
          </div>
        </template>

        <!-- 文件总数列 -->
        <template v-else-if="column.key === 'totalFiles'">
          <div class="count-cell">
            <span class="count-number">{{ record.totalFiles }}</span>
            <span class="count-label">文件</span>
          </div>
        </template>

        <!-- 成功数列 -->
        <template v-else-if="column.key === 'successCount'">
          <div class="count-cell success">
            <span class="count-number">{{ record.successCount }}</span>
            <span class="count-label">成功</span>
          </div>
        </template>

        <!-- 失败数列 -->
        <template v-else-if="column.key === 'failedCount'">
          <div class="count-cell failed">
            <span class="count-number">{{ record.failedCount }}</span>
            <span class="count-label">失败</span>
          </div>
        </template>

        <!-- 成功率列 -->
        <template v-else-if="column.key === 'successRate'">
          <div class="success-rate-cell">
            <a-progress
              :percent="calculateSuccessRate(record)"
              :stroke-color="getProgressColor(calculateSuccessRate(record))"
              :show-info="true"
              :format="percent => `${percent}%`"
              size="small"
              :status="calculateSuccessRate(record) === 0 ? 'exception' : 'normal'"
            />
          </div>
        </template>

        <!-- 操作列 -->
        <template v-else-if="column.key === 'action'">
          <a @click="handleViewDetail(record.batchId)">查看详情</a>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps, TablePaginationConfig } from 'ant-design-vue'
import type { LabelBatchPageVO } from '@/api/order/label-batch'
import { pageBatches } from '@/api/order/label-batch'
import type { Dayjs } from 'dayjs'

// Props 定义
interface Props {
  /** 平台过滤 */
  platform: 'Wildberries' | 'Ozon' | 'yandex'
  /** 是否可见 */
  open: boolean
}

const props = defineProps<Props>()

// Emits 定义
interface Emits {
  /** 选择批次查看详情 */
  (e: 'select', batchId: number): void
}

const emit = defineEmits<Emits>()

// 表格列定义
const columns: TableProps['columns'] = [
  {
    title: '批次号',
    dataIndex: 'batchNo',
    key: 'batchNo',
    width: 200
  },
  {
    title: '平台',
    dataIndex: 'platform',
    key: 'platform',
    width: 100,
    align: 'center'
  },
  {
    title: '订单数',
    dataIndex: 'totalOrders',
    key: 'totalOrders',
    width: 100,
    align: 'center'
  },
  {
    title: '文件数',
    dataIndex: 'totalFiles',
    key: 'totalFiles',
    width: 100,
    align: 'center'
  },
  {
    title: '成功',
    dataIndex: 'successCount',
    key: 'successCount',
    width: 100,
    align: 'center'
  },
  {
    title: '失败',
    dataIndex: 'failedCount',
    key: 'failedCount',
    width: 100,
    align: 'center'
  },
  {
    title: '成功率',
    key: 'successRate',
    width: 150,
    align: 'center'
  },
  {
    title: '创建人',
    dataIndex: 'createdBy',
    key: 'createdBy',
    width: 120
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    fixed: 'right',
    align: 'center'
  }
]

// 状态管理
const loading = ref(false)
const dataSource = ref<LabelBatchPageVO[]>([])
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

// 分页信息
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`
})

// 查询条件
const queryParams = reactive({
  batchNo: '',
  startTime: '',
  endTime: ''
})

// 计算成功率
function calculateSuccessRate(record: LabelBatchPageVO): number {
  const success = record.successCount || 0
  const failed = record.failedCount || 0
  const total = success + failed
  if (total === 0) return 0
  return Math.round((success / total) * 100)
}

// 获取进度条颜色（阶梯式）
function getProgressColor(percent: number): string {
  if (percent === 100) return '#52c41a' // 绿色：100%
  if (percent >= 80) return '#1890ff' // 蓝色：80-99%
  if (percent >= 50) return '#faad14' // 橙色：50-79%
  if (percent > 0) return '#ff4d4f' // 红色：1-49%
  return '#ff4d4f' // 红色：0%
}

// 加载批次列表
async function loadBatches() {
  loading.value = true
  try {
    const resp = await pageBatches(
      {
        page: pagination.current,
        size: pagination.pageSize
      },
      {
        platform: props.platform,
        batchNo: queryParams.batchNo || undefined,
        startTime: queryParams.startTime || undefined,
        endTime: queryParams.endTime || undefined
      }
    )

    const result = resp.data
    dataSource.value = result.records
    pagination.total = result.total
  } catch (error: any) {
    message.error('加载批次列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 处理表格变化（分页、排序等）
function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadBatches()
}

// 处理搜索
function handleSearch() {
  pagination.current = 1
  loadBatches()
}

// 处理日期范围变化
function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    queryParams.startTime = dates[0].format('YYYY-MM-DD HH:mm:ss')
    queryParams.endTime = dates[1].format('YYYY-MM-DD HH:mm:ss')
  } else {
    queryParams.startTime = ''
    queryParams.endTime = ''
  }
  handleSearch()
}

// 处理重置
function handleReset() {
  queryParams.batchNo = ''
  queryParams.startTime = ''
  queryParams.endTime = ''
  dateRange.value = null
  pagination.current = 1
  loadBatches()
}

// 处理查看详情
function handleViewDetail(batchId: number) {
  emit('select', batchId)
}

// 监听 visible 变化，加载数据
watch(
  () => props.open,
  visible => {
    if (visible) {
      loadBatches()
    }
  },
  { immediate: true }
)

// 组件挂载时加载数据
onMounted(() => {
  if (props.open) {
    loadBatches()
  }
})
</script>

<style scoped>
.label-batch-list {
  width: 100%;
}

.search-section {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

/* 批次号样式 */
.batch-no {
  display: flex;
  align-items: center;
}

.batch-no-text {
  font-family: 'Courier New', monospace;
  font-weight: 500;
  color: #1890ff;
}

/* 数量单元格样式 */
.count-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.count-number {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.2;
}

.count-label {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1;
}

/* 成功数样式 */
.count-cell.success .count-number {
  color: #52c41a;
}

/* 失败数样式 */
.count-cell.failed .count-number {
  color: #ff4d4f;
}

/* 成功率单元格样式 */
.success-rate-cell {
  padding: 0 8px;
}

.success-rate-cell :deep(.ant-progress) {
  margin: 0;
}

.success-rate-cell :deep(.ant-progress-text) {
  font-weight: 600;
}
</style>
