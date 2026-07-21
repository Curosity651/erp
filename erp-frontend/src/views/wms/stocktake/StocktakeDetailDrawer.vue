<template>
  <a-drawer
    v-model:open="visible"
    title="盘点单详情"
    :width="900"
    :body-style="{ paddingBottom: '80px' }"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <!-- 基本信息 -->
        <a-descriptions title="基本信息" :column="2" bordered size="small">
          <a-descriptions-item label="盘点单号">{{ detail.stocktakeNo }}</a-descriptions-item>
          <a-descriptions-item label="盘点仓库">{{ detail.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="盘点日期">{{ detail.stocktakeDate }}</a-descriptions-item>
          <a-descriptions-item label="盘点范围">
            <a-tag :color="detail.stocktakeScope === 'ALL' ? 'blue' : 'orange'">
              {{ detail.stocktakeScope === 'ALL' ? '全部SKU' : '指定SKU' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="单据状态">
            <a-tag :color="getStatusColor(detail.orderStatus)">
              {{ StocktakeStatusMap[detail.orderStatus] }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="确认时间">{{
            detail.confirmTime || '-'
          }}</a-descriptions-item>
          <a-descriptions-item label="创建人">{{ detail.createByName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{
            detail.remark || '-'
          }}</a-descriptions-item>
        </a-descriptions>

        <!-- 盘点统计 -->
        <div class="section-title">盘点统计</div>
        <a-row :gutter="16" class="statistics-row">
          <a-col :span="6">
            <a-statistic title="总SKU数" :value="detail.skuCount" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="差异项数" :value="detail.diffCount" class="diff-stat" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="盘盈数量" :value="profitQuantity" class="profit-stat">
              <template #prefix>
                <plus-outlined />
              </template>
            </a-statistic>
          </a-col>
          <a-col :span="6">
            <a-statistic title="盘亏数量" :value="Math.abs(lossQuantity)" class="loss-stat">
              <template #prefix>
                <minus-outlined />
              </template>
            </a-statistic>
          </a-col>
        </a-row>

        <!-- 盘点明细 -->
        <div class="section-title">
          盘点明细
          <span class="item-count">共 {{ filteredItems.length }} 条</span>
        </div>

        <!-- 筛选条件 -->
        <div class="filter-row">
          <a-space>
            <a-input
              v-model:value="filterKeyword"
              placeholder="SKU编码/名称"
              style="width: 180px"
              allow-clear
            />
            <a-checkbox v-model:checked="showDiffOnly">仅显示差异</a-checkbox>
          </a-space>
        </div>

        <a-table
          :columns="itemColumns"
          :data-source="filteredItems"
          :pagination="false"
          size="small"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'skuInfo'">
              <sku-brief-cell :brief="record.skuBrief" />
            </template>
            <template v-else-if="column.key === 'sourceType'">
              <a-tag :color="record.sourceType === 'ADDED' ? 'orange' : 'blue'">
                {{ StocktakeItemSourceMap[record.sourceType] || '库内' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'stocktakeStatus'">
              <a-tag :color="record.stocktakeStatus === 'COUNTED' ? 'success' : 'default'">
                {{ record.stocktakeStatus === 'COUNTED' ? '已盘' : '未盘' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'diffQuantity'">
              <span :class="getDiffClass(record.diffQuantity)">
                {{ formatDiff(record.diffQuantity) }}
              </span>
            </template>
          </template>
        </a-table>
      </template>
    </a-spin>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { PlusOutlined, MinusOutlined } from '@ant-design/icons-vue'
import { SkuBriefCell } from '@/components/Sku'
import { getStocktakeDetail } from '@/api/wms/stocktake'
import { isSuccess } from '@/api'
import type { StocktakeDetailVO, StocktakeStatus, StocktakeItemVO } from '@/api/wms/stocktake/types'
import { StocktakeItemSourceMap } from '@/api/wms/stocktake/types'

defineOptions({ name: 'StocktakeDetailDrawer' })

const visible = ref(false)
const loading = ref(false)
const detail = ref<StocktakeDetailVO | null>(null)

// 筛选条件
const filterKeyword = ref('')
const showDiffOnly = ref(false)

// 盘点单状态映射
const StocktakeStatusMap: Record<string, string> = {
  COUNTING: '盘点中',
  CONFIRMED: '已确认',
  CANCELLED: '已取消'
}

/**
 * 获取状态颜色
 */
const getStatusColor = (status: StocktakeStatus): string => {
  const colorMap: Record<string, string> = {
    COUNTING: 'processing',
    CONFIRMED: 'success',
    CANCELLED: 'error'
  }
  return colorMap[status] || 'default'
}

/**
 * 获取差异数量样式
 */
const getDiffClass = (diff: number | undefined): string => {
  if (diff === undefined || diff === null) return ''
  if (diff > 0) return 'diff-profit'
  if (diff < 0) return 'diff-loss'
  return ''
}

/**
 * 格式化差异显示
 */
const formatDiff = (diff?: number): string => {
  if (diff === undefined || diff === null) return '-'
  if (diff > 0) return `+${diff}`
  return String(diff)
}

// 筛选后的明细列表
const filteredItems = computed(() => {
  if (!detail.value?.items) return []

  let result = [...detail.value.items]

  // 关键字筛选
  if (filterKeyword.value) {
    const keyword = filterKeyword.value.toLowerCase()
    result = result.filter(
      (item: StocktakeItemVO) =>
        item.skuCode.toLowerCase().includes(keyword) ||
        (item.skuBrief?.skuName || '').toLowerCase().includes(keyword)
    )
  }

  // 仅显示差异
  if (showDiffOnly.value) {
    result = result.filter(
      (item: StocktakeItemVO) =>
        item.diffQuantity !== undefined && item.diffQuantity !== null && item.diffQuantity !== 0
    )
  }

  return result
})

// 盘盈数量
const profitQuantity = computed(() => {
  if (!detail.value?.items) return 0
  return detail.value.items
    .filter((item: StocktakeItemVO) => item.diffQuantity && item.diffQuantity > 0)
    .reduce((sum: number, item: StocktakeItemVO) => sum + (item.diffQuantity || 0), 0)
})

// 盘亏数量
const lossQuantity = computed(() => {
  if (!detail.value?.items) return 0
  return detail.value.items
    .filter((item: StocktakeItemVO) => item.diffQuantity && item.diffQuantity < 0)
    .reduce((sum: number, item: StocktakeItemVO) => sum + (item.diffQuantity || 0), 0)
})

// 盘点明细列定义
const itemColumns = [
  { title: 'SKU信息', key: 'skuInfo', width: 200 },
  { title: '来源', key: 'sourceType', width: 70, align: 'center' as const },
  { title: '系统数量', dataIndex: 'systemQuantity', width: 90, align: 'right' as const },
  { title: '实盘数量', dataIndex: 'actualQuantity', width: 90, align: 'right' as const },
  { title: '差异数量', key: 'diffQuantity', width: 90, align: 'right' as const },
  { title: '盘点状态', key: 'stocktakeStatus', width: 90, align: 'center' as const }
]

/**
 * 打开抽屉
 */
const open = async (id: number) => {
  visible.value = true
  loading.value = true
  detail.value = null
  // 重置筛选条件
  filterKeyword.value = ''
  showDiffOnly.value = false

  try {
    const result = await getStocktakeDetail(id)
    if (isSuccess(result) && result.data) {
      detail.value = result.data
    }
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 24px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.item-count {
  font-size: 12px;
  font-weight: normal;
  color: #999;
  margin-left: 8px;
}

.statistics-row {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

.filter-row {
  margin-bottom: 12px;
}

.sku-info-cell {
  line-height: 1.4;
}

.sku-code {
  font-weight: 500;
  color: #1890ff;
}

.sku-name {
  font-size: 12px;
  color: #666;
}

.diff-profit {
  color: #52c41a;
  font-weight: 500;
}

.diff-loss {
  color: #ff4d4f;
  font-weight: 500;
}

.profit-stat :deep(.ant-statistic-content) {
  color: #52c41a;
}

.loss-stat :deep(.ant-statistic-content) {
  color: #ff4d4f;
}

.diff-stat :deep(.ant-statistic-content) {
  color: #faad14;
}
</style>
