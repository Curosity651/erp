<template>
  <a-card :bordered="false" class="sku-config-card">
    <template #title>
      <a-space>
        <bars-outlined />
        <span>SKU 独立配置</span>
      </a-space>
    </template>

    <div class="table-toolbar">
      <div class="toolbar-left">
        <a-space>
          <RegionSelect
            v-model:value="searchParams.regionId"
            placeholder="选择区域"
            allow-clear
            style="width: 200px"
          />
          <a-input-search
            v-model:value="searchParams.skuKeyword"
            placeholder="请输入 SKU 编码或名称"
            style="width: 240px"
            enter-button
            @search="handleSearch"
          />
        </a-space>
      </div>
      <div class="toolbar-right">
        <a-button type="primary" @click="handleCreate">
          <plus-outlined />
          新增配置
        </a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="showPagination ? pagination : false"
      row-key="id"
      size="middle"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'skuBrief'">
          <SkuBriefCell :brief="record.skuBrief" />
        </template>

        <template v-else-if="column.key === 'safetyStock'">
          <ConfigValueCell
            :value="record.safetyStock"
            :default-value="props.defaultSafetyStock"
            suffix=" 件"
          />
        </template>

        <template v-else-if="column.key === 'alertRule'">
          <ConfigValueCell
            :value="record.notifyThresholdDays"
            :default-value="props.thresholdDays"
            prefix="≤ "
            suffix=" 天"
          />
        </template>

        <template v-else-if="column.key === 'operate'">
          <operation-group>
            <a @click="handleEdit(record)">编辑</a>
            <DeleteTextButton @confirm="handleDelete(record)" />
          </operation-group>
        </template>
      </template>
    </a-table>

    <!-- 数据少时显示总数 -->
    <div v-if="!showPagination && tableData.length > 0" class="table-footer">
      共 {{ pagination.total }} 条配置
    </div>

    <SkuConfigFormModal
      v-model:open="formModalOpen"
      :edit-data="editData"
      :threshold-days="props.thresholdDays"
      :default-safety-stock="props.defaultSafetyStock"
      @success="loadData"
    />
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { PlusOutlined, BarsOutlined } from '@ant-design/icons-vue'
import { doRequest } from '@/utils/axios/request'
import { isSuccess } from '@/api'
import { pageSkuConfig, deleteSkuConfig } from '@/api/wms/inventory-forecast'
import RegionSelect from '@/components/Lov/RegionSelect.vue'
import SkuBriefCell from '@/components/Sku/SkuBriefCell.vue'
import DeleteTextButton from '@/components/Button/DeleteTextButton.vue'
import OperationGroup from '@/components/Operation/OperationGroup.vue'
import ConfigValueCell from './ConfigValueCell.vue'
import SkuConfigFormModal from './SkuConfigFormModal.vue'
import type { InventoryConfigVO } from '@/api/wms/inventory-forecast/types'

defineOptions({ name: 'SkuConfigTable' })

const props = defineProps<{
  thresholdDays: number
  defaultSafetyStock: number
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

const columns = [
  { title: '区域', dataIndex: 'regionName', key: 'regionName', width: 120 },
  { title: 'SKU 信息', key: 'skuBrief', width: 280 },
  { title: '安全库存', key: 'safetyStock', width: 160 },
  { title: '预警规则', key: 'alertRule', width: 180 },
  { title: '操作', key: 'operate', width: 120, fixed: 'right' as const }
]

const loading = ref(false)
const tableData = ref<InventoryConfigVO[]>([])
const searchParams = reactive({
  regionId: undefined as number | undefined,
  skuKeyword: ''
})
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const showPagination = computed(() => pagination.total > 20)

const formModalOpen = ref(false)
const editData = ref<InventoryConfigVO>()

async function loadData() {
  loading.value = true
  try {
    const result = await pageSkuConfig({
      current: pagination.current,
      size: pagination.pageSize,
      ...searchParams
    })
    if (isSuccess(result)) {
      tableData.value = result.data.records
      pagination.total = result.data.total
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function handleTableChange(pag: any) {
  if (pag) {
    pagination.current = pag.current ?? 1
    pagination.pageSize = pag.pageSize ?? 20
    loadData()
  }
}

function handleCreate() {
  editData.value = undefined
  formModalOpen.value = true
}

function handleEdit(record: Record<string, any>) {
  editData.value = record as InventoryConfigVO
  formModalOpen.value = true
}

function handleDelete(record: Record<string, any>) {
  doRequest(deleteSkuConfig(record.id), {
    successMessage: '删除成功',
    onSuccess: () => loadData()
  })
}

onMounted(loadData)
</script>

<style scoped>
.sku-config-card {
  :deep(.ant-card-body) {
    padding: 24px;
    padding-top: 12px;
  }
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-footer {
  margin-top: 16px;
  text-align: right;
  font-size: 13px;
  color: var(--ant-color-text-secondary);
}
</style>
