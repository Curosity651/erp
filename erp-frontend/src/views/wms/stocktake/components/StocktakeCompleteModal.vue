<template>
  <a-modal
    v-model:open="open"
    title="确认盘点"
    :width="760"
    :confirm-loading="confirmLoading"
    :ok-button-props="{ disabled: !confirmed }"
    ok-text="确认盘点"
    cancel-text="返回修改"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <!-- 提示信息 -->
    <a-alert type="warning" show-icon style="margin-bottom: 16px">
      <template #message> 请确认以下盘点差异，确认后将直接更新库存 </template>
    </a-alert>

    <!-- 加载状态 -->
    <a-spin :spinning="loading">
      <!-- 差异汇总 -->
      <a-row :gutter="16" class="diff-summary">
        <a-col :span="6">
          <a-statistic title="盘点总数" :value="preview?.totalCount ?? 0" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="无差异" :value="preview?.noDiffCount ?? 0" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="盘盈" class="profit-stat">
            <template #formatter>
              <span class="stat-profit">
                {{ preview?.profitCount ?? 0 }} 项 (+{{ preview?.profitQuantity ?? 0 }})
              </span>
            </template>
          </a-statistic>
        </a-col>
        <a-col :span="6">
          <a-statistic title="盘亏" class="loss-stat">
            <template #formatter>
              <span class="stat-loss">
                {{ preview?.lossCount ?? 0 }} 项 (-{{ Math.abs(preview?.lossQuantity ?? 0) }})
              </span>
            </template>
          </a-statistic>
        </a-col>
      </a-row>

      <!-- 未盘点提示 -->
      <a-alert
        v-if="preview?.pendingCount && preview.pendingCount > 0"
        type="info"
        show-icon
        style="margin: 16px 0"
      >
        <template #message>
          还有 {{ preview.pendingCount }} 项未盘点，完成后将按无差异处理
        </template>
      </a-alert>

      <!-- 差异明细表格 -->
      <div v-if="preview?.diffItems && preview.diffItems.length > 0" class="diff-table">
        <a-typography-title :level="5">差异明细</a-typography-title>
        <a-table
          :data-source="preview.diffItems"
          :columns="columns"
          size="small"
          :pagination="{ pageSize: 5 }"
          row-key="skuCode"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'skuInfo'">
              <sku-brief-cell :brief="record.skuBrief" />
            </template>
            <template v-else-if="column.dataIndex === 'diffQuantity'">
              <span :class="record.diffType === 'PROFIT' ? 'stat-profit' : 'stat-loss'">
                {{ record.diffType === 'PROFIT' ? '+' : '' }}{{ record.diffQuantity }}
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'diffType'">
              <a-tag :color="record.diffType === 'PROFIT' ? 'success' : 'error'">
                {{ record.diffType === 'PROFIT' ? '盘盈' : '盘亏' }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>

      <!-- 无差异提示 -->
      <a-empty
        v-else-if="!loading && preview"
        description="本次盘点无差异"
        style="margin: 24px 0"
      />
    </a-spin>

    <!-- 确认勾选 -->
    <div style="margin-top: 16px; border-top: 1px solid #f0f0f0; padding-top: 16px">
      <a-checkbox v-model:checked="confirmed"> 我已核对以上差异无误 </a-checkbox>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { getDiffPreview, confirmStocktake } from '@/api/wms/stocktake'
import { isSuccess } from '@/api'
import type { StocktakeDiffPreviewVO } from '@/api/wms/stocktake/types'
import { SkuBriefCell } from '@/components/Sku'

const props = defineProps<{
  stocktakeId: number | null
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
  (e: 'cancel'): void
}>()

// Vue 3.3 兼容的 v-model 实现
const open = computed({
  get: () => props.open,
  set: (val: boolean) => emit('update:open', val)
})

const loading = ref(false)
const confirmLoading = ref(false)
const confirmed = ref(false)
const preview = ref<StocktakeDiffPreviewVO | null>(null)

// 表格列定义
const columns = [
  { title: 'SKU信息', key: 'skuInfo', width: 250 },
  { title: '系统数量', dataIndex: 'systemQuantity', width: 90, align: 'right' as const },
  { title: '实盘数量', dataIndex: 'actualQuantity', width: 90, align: 'right' as const },
  { title: '差异', dataIndex: 'diffQuantity', width: 80, align: 'right' as const },
  { title: '类型', dataIndex: 'diffType', width: 70, align: 'center' as const }
]

// 监听弹窗打开，加载差异预览
watch(open, async newVal => {
  if (newVal && props.stocktakeId) {
    await loadDiffPreview()
  } else {
    // 关闭时重置状态
    confirmed.value = false
    preview.value = null
  }
})

// 加载差异预览
async function loadDiffPreview() {
  if (!props.stocktakeId) return

  loading.value = true
  try {
    const result = await getDiffPreview(props.stocktakeId)
    if (isSuccess(result) && result.data) {
      preview.value = result.data
    } else {
      message.error('获取差异预览失败')
    }
  } catch (error) {
    message.error('获取差异预览失败')
  } finally {
    loading.value = false
  }
}

// 确认盘点
async function handleConfirm() {
  if (!confirmed.value) {
    message.warning('请先勾选确认差异无误')
    return
  }

  if (!props.stocktakeId) return

  confirmLoading.value = true
  try {
    const result = await confirmStocktake(props.stocktakeId)
    if (isSuccess(result)) {
      message.success('盘点确认完成，库存已更新')
      open.value = false
      emit('success')
    } else {
      message.error(result.message || '确认盘点失败')
    }
  } catch (error) {
    if (!(error as { resolved?: boolean })?.resolved) {
      message.error('确认盘点失败')
    }
  } finally {
    confirmLoading.value = false
  }
}

// 取消
function handleCancel() {
  open.value = false
  emit('cancel')
}
</script>

<style scoped>
.diff-summary {
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.diff-table {
  margin-top: 16px;
}

.stat-profit {
  color: #52c41a;
  font-weight: 500;
}

.stat-loss {
  color: #ff4d4f;
  font-weight: 500;
}
</style>
