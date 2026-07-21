<template>
  <div class="form-section">
    <div class="section-title">
      <safety-certificate-outlined class="section-icon" />
      质检数据
    </div>

    <!-- SKU质检数据列表 -->
    <div class="qc-table-wrapper">
      <a-table
        :data-source="qcItems"
        :columns="columns"
        :pagination="false"
        size="small"
        row-key="skuCode"
      >
        <template #bodyCell="{ column, record }">
          <!-- SKU信息 -->
          <template v-if="column.key === 'skuInfo'">
            <sku-brief-cell :brief="getSkuBrief(record.skuCode)" />
          </template>

          <!-- 尺寸显示 -->
          <template v-else-if="column.key === 'dimension'">
            <span v-if="hasDimension(record)">
              {{ record.lengthCm }} × {{ record.widthCm }} × {{ record.heightCm }}
            </span>
            <span v-else class="empty-text">-</span>
          </template>

          <!-- 毛重显示 -->
          <template v-else-if="column.key === 'grossWeightKg'">
            <span v-if="record.grossWeightKg">{{ record.grossWeightKg }}</span>
            <span v-else class="empty-text">-</span>
          </template>

          <!-- 净重显示 -->
          <template v-else-if="column.key === 'netWeightKg'">
            <span v-if="record.netWeightKg">{{ record.netWeightKg }}</span>
            <span v-else class="empty-text">-</span>
          </template>

          <!-- 质检报告状态 -->
          <template v-else-if="column.key === 'qcFile'">
            <span v-if="record.qcFileId" class="status-uploaded">
              <check-circle-outlined /> 已上传
            </span>
            <span v-else class="status-empty">- 未上传</span>
          </template>

          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" :disabled="disabled" @click="handleEdit(record)">
              编辑
            </a-button>
          </template>
        </template>
      </a-table>
    </div>

    <div class="qc-hint"><info-circle-outlined /> 质检数据将作为入库验货的标准</div>

    <!-- 质检数据编辑弹窗 -->
    <qc-item-form-modal
      ref="qcItemModalRef"
      :sku-brief-map="skuBriefMap"
      @save="handleSaveQcItem"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  SafetyCertificateOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'
import QcItemFormModal from './QcItemFormModal.vue'
import { SkuBriefCell } from '@/components/Sku'
import type {
  QcItemDTO,
  QcDataDTO,
  PurchaseOrderItemDTO,
  SkuBriefVO
} from '@/api/wms/purchase-order/types'

interface Props {
  // 质检数据DTO
  qcData: QcDataDTO
  // 采购明细列表（用于生成SKU行）
  orderItems: PurchaseOrderItemDTO[]
  // SKU展示信息映射表
  skuBriefMap?: Record<string, SkuBriefVO>
  // 是否禁用
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  skuBriefMap: () => ({})
})

const emits = defineEmits<{
  (e: 'update:qcData', value: QcDataDTO): void
}>()

// 弹窗引用
const qcItemModalRef = ref<InstanceType<typeof QcItemFormModal>>()

// 质检数据项列表
const qcItems = computed<QcItemDTO[]>(() => {
  return props.qcData.items || []
})

// 获取 SKU 展示信息
const getSkuBrief = (skuCode: string): SkuBriefVO | undefined => {
  return props.skuBriefMap[skuCode]
}

// 表格列定义
const columns = [
  { title: 'SKU信息', key: 'skuInfo', width: 240 },
  { title: '尺寸(cm)', key: 'dimension', width: 140, align: 'center' as const },
  { title: '毛重(kg)', key: 'grossWeightKg', width: 100, align: 'center' as const },
  { title: '净重(kg)', key: 'netWeightKg', width: 100, align: 'center' as const },
  { title: '质检报告', key: 'qcFile', width: 100, align: 'center' as const },
  { title: '操作', key: 'action', width: 80, align: 'center' as const }
]

// 判断是否有尺寸数据
const hasDimension = (record: QcItemDTO): boolean => {
  return !!(record.lengthCm && record.widthCm && record.heightCm)
}

// 根据采购明细初始化质检数据
watch(
  () => props.orderItems,
  items => {
    if (!items || items.length === 0) return

    const existingItems = props.qcData.items || []
    const existingSkuCodes = new Set(existingItems.map(item => item.skuCode))

    // 为新的SKU添加空的质检数据行
    const newItems: QcItemDTO[] = items
      .filter(item => item.skuCode && !existingSkuCodes.has(item.skuCode))
      .map(item => ({
        skuCode: item.skuCode
      }))

    if (newItems.length > 0) {
      emits('update:qcData', {
        ...props.qcData,
        items: [...existingItems, ...newItems]
      })
    }
  },
  { immediate: true }
)

// 编辑质检数据
const handleEdit = (record: QcItemDTO) => {
  qcItemModalRef.value?.open(record)
}

// 保存质检数据
const handleSaveQcItem = (data: QcItemDTO) => {
  const items = [...(props.qcData.items || [])]
  const index = items.findIndex(item => item.skuCode === data.skuCode)

  if (index > -1) {
    items[index] = { ...items[index], ...data }
  } else {
    items.push(data)
  }

  emits('update:qcData', {
    ...props.qcData,
    items
  })
}

// 校验质检数据
const validate = (): string | null => {
  for (const item of qcItems.value) {
    if (item.grossWeightKg && item.netWeightKg) {
      if (item.netWeightKg > item.grossWeightKg) {
        return `SKU ${item.skuCode} 的净重不能大于毛重`
      }
    }
  }
  return null
}

defineExpose({ validate })
</script>

<style scoped>
.form-section {
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-icon {
  margin-right: 8px;
  color: #1890ff;
  font-size: 16px;
}

.qc-table-wrapper {
  margin-bottom: 12px;
}

.empty-text {
  color: #bfbfbf;
}

.status-uploaded {
  color: #52c41a;
}

.status-empty {
  color: #bfbfbf;
}

.qc-hint {
  color: #8c8c8c;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-size: 13px;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 12px 8px;
}
</style>
