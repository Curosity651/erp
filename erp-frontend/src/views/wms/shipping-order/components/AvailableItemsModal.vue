<template>
  <a-modal
    v-model:open="visible"
    title="从采购单添加货物"
    :width="900"
    :mask-closable="false"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <!-- 搜索表单 -->
    <a-form layout="inline" style="margin-bottom: 16px">
      <a-form-item label="采购单号">
        <a-input
          v-model:value="searchForm.purchaseOrderNo"
          placeholder="请输入采购单号"
          allow-clear
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item label="SKU编码">
        <a-input
          v-model:value="searchForm.skuCode"
          placeholder="请输入SKU编码"
          allow-clear
          style="width: 150px"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 数据表格 -->
    <a-table
      :data-source="dataSource"
      :columns="columns"
      :loading="loading"
      :pagination="false"
      :row-selection="rowSelection"
      size="small"
      row-key="purchaseOrderItemId"
      :scroll="{ y: 400 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'skuInfo'">
          <sku-brief-cell :brief="record.skuBrief" />
        </template>
      </template>
    </a-table>

    <!-- 已选统计 -->
    <div class="selection-info">
      已选择 <span class="count">{{ selectedRowKeys.length }}</span> 项
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { getAvailableItems } from '@/api/wms/shipping-order'
import { isSuccess } from '@/api'
import type { AvailableItemVO, AvailableItemQO } from '@/api/wms/shipping-order/types'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'

const props = defineProps<{
  excludeItemIds: number[]
  shippingOrderId?: number
}>()

const emits = defineEmits<{
  (e: 'confirm', items: AvailableItemVO[]): void
}>()

const visible = ref(false)
const loading = ref(false)
const dataSource = ref<AvailableItemVO[]>([])
const selectedRowKeys = ref<number[]>([])
const selectedRows = ref<AvailableItemVO[]>([])

const searchForm = reactive<AvailableItemQO>({
  purchaseOrderNo: undefined,
  skuCode: undefined,
  supplierId: undefined,
  shippingOrderId: undefined
})

const columns = [
  { title: '采购单号', dataIndex: 'purchaseOrderNo', width: 140 },
  { title: 'SKU信息', key: 'skuInfo', width: 220 },
  { title: '供应商', dataIndex: 'supplierName', width: 120, ellipsis: true },
  { title: '采购数量', dataIndex: 'purchaseQuantity', width: 80, align: 'center' },
  { title: '已发货', dataIndex: 'shippedQuantity', width: 70, align: 'center' },
  { title: '可发货', dataIndex: 'availableQuantity', width: 70, align: 'center' }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[], rows: AvailableItemVO[]) => {
    selectedRowKeys.value = keys
    selectedRows.value = rows
  },
  getCheckboxProps: (record: AvailableItemVO) => ({
    disabled: props.excludeItemIds.includes(record.purchaseOrderItemId)
  })
}))

// 打开弹窗
const open = () => {
  searchForm.shippingOrderId = props.shippingOrderId
  visible.value = true
  selectedRowKeys.value = []
  selectedRows.value = []
  loadData()
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const result = await getAvailableItems(searchForm)
    if (isSuccess(result)) {
      // 过滤掉已添加的明细
      dataSource.value = (result.data || []).filter(
        item => !props.excludeItemIds.includes(item.purchaseOrderItemId)
      )
    } else {
      message.error(result.message || '加载数据失败')
    }
  } catch (e) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleSearch = () => {
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.purchaseOrderNo = undefined
  searchForm.skuCode = undefined
  searchForm.supplierId = undefined
  loadData()
}

// 确认
const handleOk = () => {
  if (selectedRows.value.length === 0) {
    message.warning('请选择要添加的货物')
    return
  }
  emits('confirm', selectedRows.value)
  visible.value = false
}

// 取消
const handleCancel = () => {
  visible.value = false
}

defineExpose({ open })
</script>

<style scoped>
.selection-info {
  margin-top: 12px;
  font-size: 13px;
  color: #595959;
}

.selection-info .count {
  font-weight: 600;
  color: #1890ff;
}
</style>
