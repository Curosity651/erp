<template>
  <a-modal
    v-model:open="visible"
    title="选择调拨商品"
    width="700px"
    :footer="null"
    :destroy-on-close="true"
  >
    <!-- 搜索区域 -->
    <div class="search-area">
      <a-input-search
        v-model:value="keyword"
        placeholder="请输入SKU编码或名称搜索"
        style="width: 300px"
        allow-clear
        @search="handleSearch"
        @press-enter="handleSearch"
      />
    </div>

    <!-- 可选商品列表 -->
    <pro-table
      ref="tableRef"
      row-key="skuCode"
      :request="tableRequest"
      :columns="columns"
      :tool-bar-render="false"
      :row-selection="rowSelection"
      :scroll="{ y: 400 }"
      size="small"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- SKU信息 -->
        <template v-if="column.key === 'skuInfo'">
          <SkuBriefCell :brief="record.skuBrief" />
        </template>

        <!-- 可用库存 -->
        <template v-else-if="column.key === 'availableQuantity'">
          <span class="available-quantity">{{ record.availableQuantity }}</span>
        </template>
      </template>
    </pro-table>

    <!-- 底部操作 -->
    <div class="modal-footer">
      <div class="selected-info">
        已选择 <span class="count">{{ selectedRowKeys.length }}</span> 个商品
      </div>
      <a-space>
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" :disabled="selectedRowKeys.length === 0" @click="handleConfirm">
          确认添加
        </a-button>
      </a-space>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ProTable from '#/table'
import type { ProTableInstanceExpose } from '#/table/Table'
import type { TableRequest } from '#/table/typing'
import { mergePageParam } from '@/utils/page-utils'
import { pageAvailableStock } from '@/api/wms/transfer-order'
import type { AvailableStockVO } from '@/api/wms/transfer-order/types'
import type { TransferItemFormData } from './TransferItemTable.vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'

defineOptions({ name: 'SelectInventoryModal' })

const emits = defineEmits<{
  (e: 'confirm', items: TransferItemFormData[]): void
}>()

const visible = ref(false)
const keyword = ref('')
const warehouseId = ref<number | null>(null)
const existingSkuCodes = ref<string[]>([])

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()

// 当前页数据缓存（用于确认时获取完整数据）
const currentPageData = ref<AvailableStockVO[]>([])

// 选中的行
const selectedRowKeys = ref<string[]>([])

// 表格列定义
const columns = [
  { title: 'SKU信息', key: 'skuInfo', width: 400 },
  { title: '可用库存', key: 'availableQuantity', width: 120, align: 'center' as const }
]

// 行选择配置
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: string[]) => {
    selectedRowKeys.value = keys
  },
  getCheckboxProps: (record: AvailableStockVO) => ({
    disabled: existingSkuCodes.value.includes(record.skuCode)
  })
}))

/**
 * 表格请求
 */
const tableRequest: TableRequest = async (params, _sorter, _filter) => {
  if (!warehouseId.value) {
    return { records: [], total: 0 }
  }

  const pageParam = mergePageParam(params, _sorter, _filter)
  const result = await pageAvailableStock({
    ...pageParam,
    warehouseId: warehouseId.value,
    keyword: keyword.value || undefined
  })

  // 缓存当前页数据
  if (result.data) {
    currentPageData.value = result.data.records || []
  }

  return result
}

/**
 * 搜索
 */
const handleSearch = () => {
  selectedRowKeys.value = []
  tableRef.value?.actionRef?.reload(true)
}

/**
 * 表格变化（翻页时清空选中）
 */
const handleTableChange = () => {
  selectedRowKeys.value = []
}

/**
 * 打开弹窗
 */
const open = (wId: number, existingCodes: string[] = []) => {
  warehouseId.value = wId
  existingSkuCodes.value = existingCodes
  keyword.value = ''
  selectedRowKeys.value = []
  currentPageData.value = []
  visible.value = true

  // 等待弹窗渲染后刷新表格
  setTimeout(() => {
    tableRef.value?.actionRef?.reload(true)
  }, 100)
}

/**
 * 确认选择
 */
const handleConfirm = () => {
  // 从当前页数据中获取选中的商品
  const items: TransferItemFormData[] = selectedRowKeys.value.map(skuCode => {
    const stock = currentPageData.value.find(s => s.skuCode === skuCode)!
    return {
      key: skuCode,
      skuCode: stock.skuCode,
      skuBrief: stock.skuBrief,
      availableQuantity: stock.availableQuantity,
      quantity: stock.availableQuantity
    }
  })

  emits('confirm', items)
  visible.value = false
}

/**
 * 取消
 */
const handleCancel = () => {
  visible.value = false
}

defineExpose({ open })
</script>

<style scoped>
.search-area {
  margin-bottom: 16px;
}

.available-quantity {
  color: var(--ant-success-color, #52c41a);
  font-weight: 500;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--ant-border-color-split, #f0f0f0);
}

.selected-info {
  color: var(--ant-text-color-secondary, #8c8c8c);
}

.selected-info .count {
  color: var(--ant-primary-color, #1890ff);
  font-weight: 600;
}
</style>
