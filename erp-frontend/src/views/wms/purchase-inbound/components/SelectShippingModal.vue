<template>
  <a-modal
    v-model:open="visible"
    title="选择物流单"
    :width="800"
    :mask-closable="false"
    @cancel="handleCancel"
  >
    <!-- 筛选条件 -->
    <div class="filter-section">
      <a-space :size="12">
        <a-input
          v-model:value="filterParams.keyword"
          placeholder="物流单号/采购单号"
          style="width: 200px"
          allow-clear
          @press-enter="handleSearch"
        />
        <LogisticsProviderSelect
          v-model:value="filterParams.providerId"
          placeholder="物流商"
          style="width: 150px"
        />
        <a-button type="primary" @click="handleSearch">查询</a-button>
      </a-space>
    </div>

    <!-- 物流单列表 -->
    <a-spin :spinning="loading">
      <a-table
        :data-source="shippingList"
        :columns="columns"
        :pagination="false"
        :row-selection="rowSelection"
        row-key="id"
        size="small"
        :scroll="{ y: 300 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'shippingNo'">
            <div class="shipping-no-cell">
              <div class="shipping-no">{{ record.shippingNo }}</div>
              <div class="provider-name">{{ record.providerName }}</div>
            </div>
          </template>
          <template v-else-if="column.key === 'quantity'">
            <div class="quantity-cell">
              <div>发货: {{ record.totalQuantity }}</div>
              <div class="pending">
                待入库: <span class="pending-value">{{ record.pendingQuantity }}</span>
              </div>
            </div>
          </template>
        </template>
      </a-table>
      <div v-if="shippingList.length === 0 && !loading" class="empty-tip">暂无可入库的物流单</div>
    </a-spin>

    <div class="tip-text">说明：仅显示已发货状态且有待入库货物的物流单</div>

    <template #footer>
      <a-button @click="handleCancel">取消</a-button>
      <a-button type="primary" :disabled="!selectedShipping" @click="handleConfirm">
        确认选择
      </a-button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { TableRowSelection } from 'ant-design-vue/es/table/interface'
import { getAvailableShipping } from '@/api/wms/purchase-inbound'
import type { AvailableShippingVO, AvailableShippingQO } from '@/api/wms/purchase-inbound/types'
import { isSuccess } from '@/api'
import LogisticsProviderSelect from '@/components/Lov/LogisticsProviderSelect.vue'

const emits = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', shipping: AvailableShippingVO): void
}>()

const visible = ref(false)
const loading = ref(false)
const shippingList = ref<AvailableShippingVO[]>([])
const selectedRowKeys = ref<number[]>([])
const selectedShipping = ref<AvailableShippingVO | null>(null)

// 筛选参数
const filterParams = reactive({
  keyword: undefined as string | undefined, // 统一的搜索关键词
  providerId: undefined as number | undefined
})

// 表格列定义
const columns = [
  { title: '物流单号', key: 'shippingNo', width: 200 },
  { title: '发货日期', dataIndex: 'shippingDate', width: 120 },
  { title: '数量信息', key: 'quantity', width: 150 }
]

// 行选择配置
const rowSelection = computed<TableRowSelection>(() => ({
  type: 'radio',
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[], rows: AvailableShippingVO[]) => {
    selectedRowKeys.value = keys as number[]
    selectedShipping.value = rows[0] || null
  }
}))

/**
 * 加载可入库物流单列表
 */
const loadShippingList = async () => {
  loading.value = true
  try {
    const params: AvailableShippingQO = {
      shippingNo: filterParams.keyword,
      purchaseOrderNo: filterParams.keyword, // 后端会做 OR 查询
      providerId: filterParams.providerId
    }
    const result = await getAvailableShipping(params)
    if (isSuccess(result)) {
      shippingList.value = result.data || []
    } else {
      message.error(result.message || '获取物流单列表失败')
    }
  } catch (e) {
    message.error('获取物流单列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 查询
 */
const handleSearch = () => {
  selectedRowKeys.value = []
  selectedShipping.value = null
  loadShippingList()
}

/**
 * 打开弹窗
 */
const open = () => {
  visible.value = true
  selectedRowKeys.value = []
  selectedShipping.value = null
  filterParams.keyword = undefined
  filterParams.providerId = undefined
  loadShippingList()
}

/**
 * 取消
 */
const handleCancel = () => {
  visible.value = false
}

/**
 * 确认选择
 */
const handleConfirm = () => {
  if (selectedShipping.value) {
    emits('confirm', selectedShipping.value)
    visible.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.filter-section {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.shipping-no-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.shipping-no {
  font-weight: 600;
  color: #262626;
}

.provider-name {
  font-size: 12px;
  color: #8c8c8c;
}

.quantity-cell {
  font-size: 12px;
}

.pending {
  color: #8c8c8c;
}

.pending-value {
  color: #1890ff;
  font-weight: 600;
}

.empty-tip {
  text-align: center;
  padding: 40px 0;
  color: #8c8c8c;
}

.tip-text {
  margin-top: 12px;
  font-size: 12px;
  color: #8c8c8c;
}
</style>
