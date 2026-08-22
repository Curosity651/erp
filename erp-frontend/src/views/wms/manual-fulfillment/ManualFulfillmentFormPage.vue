<template>
  <a-modal
    v-model:open="visible"
    :title="editingId ? '编辑人工出库' : '新建人工出库'"
    width="760px"
    :confirm-loading="saving"
    @ok="handleSave"
  >
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="form-grid">
        <a-form-item label="出库仓库" name="warehouseId">
          <a-select v-model:value="form.warehouseId" placeholder="请选择自有仓库">
            <a-select-option v-for="item in warehouseOptions" :key="item.id" :value="item.id">
              {{ item.warehouseName }}（{{ item.warehouseCode }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="物流产品" name="logisticsProductId">
          <a-select v-model:value="form.logisticsProductId" placeholder="请选择物流产品">
            <a-select-option v-for="item in productOptions" :key="item.id" :value="item.id">
              {{ item.productName }}（{{ item.unitPrice }} {{ item.currency }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="收件人" name="recipientName">
          <a-input v-model:value="form.recipientName" maxlength="100" />
        </a-form-item>
        <a-form-item label="联系电话" name="recipientPhone">
          <a-input v-model:value="form.recipientPhone" maxlength="50" />
        </a-form-item>
        <a-form-item class="address-field" label="收货地址" name="recipientAddress">
          <a-input v-model:value="form.recipientAddress" maxlength="500" />
        </a-form-item>
      </div>

      <div class="items-header">
        <span>商品明细</span>
        <a-button type="link" @click="addItem"><plus-outlined />添加商品</a-button>
      </div>
      <a-table
        :data-source="form.items"
        :columns="itemColumns"
        :pagination="false"
        row-key="_key"
        size="small"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'skuCode'">
            <a-select
              v-model:value="record.skuCode"
              show-search
              :filter-option="false"
              :options="skuOptions"
              placeholder="输入 SKU 搜索"
              style="width: 100%"
              @search="searchSku"
            />
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="1" :precision="0" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'operate'">
            <a-button type="text" danger :disabled="form.items.length === 1" @click="removeItem(index)">
              <delete-outlined />
            </a-button>
          </template>
        </template>
      </a-table>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import type { WarehouseOptionVO } from '@/api/wms/warehouse/types'
import { searchSkuByKeyword } from '@/api/product/sku'
import {
  createManualFulfillment,
  getManualFulfillment,
  listManualFulfillmentItems,
  updateManualFulfillment
} from '@/api/wms/fulfillment'
import type { ManualFulfillmentForm } from '@/api/wms/fulfillment/types'
import { listOwnerLogisticsProducts } from '@/api/wms/logistics-product'
import type { LogisticsProductVO } from '@/api/wms/logistics-product/types'

const emit = defineEmits<{ saved: [] }>()
const visible = ref(false)
const saving = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const warehouseOptions = ref<WarehouseOptionVO[]>([])
const skuOptions = ref<{ label: string; value: string }[]>([])
const productOptions = ref<LogisticsProductVO[]>([])
let lineKey = 1
type FormLine = ManualFulfillmentForm['items'][number] & { _key: number }
const form = reactive<Omit<ManualFulfillmentForm, 'items'> & { items: FormLine[] }>({ items: [] })

const rules: Record<string, Rule[]> = {
  warehouseId: [{ required: true, message: '请选择出库仓库' }],
  logisticsProductId: [{ required: true, message: '请选择物流产品' }]
}
const itemColumns = [
  { title: 'ERP SKU', key: 'skuCode', width: 420 },
  { title: '数量', key: 'quantity', width: 160 },
  { title: '操作', key: 'operate', width: 70, align: 'center' }
]

const newLine = (skuCode = '', quantity = 1): FormLine => ({ _key: lineKey++, skuCode, quantity })
const reset = () => {
  editingId.value = undefined
  form.warehouseId = undefined
  form.logisticsProductId = undefined
  form.recipientName = ''
  form.recipientPhone = ''
  form.recipientAddress = ''
  form.items = [newLine()]
}

const loadOptions = async () => {
  const [warehouseResult, productResult] = await Promise.all([
    getWarehouseOptions(), listOwnerLogisticsProducts()
  ])
  if (isSuccess(warehouseResult)) warehouseOptions.value = (warehouseResult.data || []).filter((item) => item.warehouseType === 'OWN')
  if (isSuccess(productResult)) productOptions.value = productResult.data || []
}

const open = async (id?: number) => {
  reset()
  await loadOptions()
  if (id) {
    editingId.value = id
    const [orderResult, itemResult] = await Promise.all([
      getManualFulfillment(id),
      listManualFulfillmentItems(id)
    ])
    if (!isSuccess(orderResult) || !isSuccess(itemResult)) {
      message.error('人工出库草稿加载失败')
      return
    }
    Object.assign(form, {
      warehouseId: orderResult.data.warehouseId,
      logisticsProductId: orderResult.data.logisticsProductId,
      recipientName: orderResult.data.recipientName || '',
      recipientPhone: orderResult.data.recipientPhone || '',
      recipientAddress: orderResult.data.recipientAddress || '',
      items: (itemResult.data || []).map((item) => newLine(item.skuCode, item.quantity))
    })
    skuOptions.value = (itemResult.data || []).map((item) => ({
      label: `${item.skuCode}${item.skuName ? ` - ${item.skuName}` : ''}`,
      value: item.skuCode
    }))
  }
  visible.value = true
}

const searchSku = async (keyword: string) => {
  const text = keyword.trim()
  if (!text) return
  const result = await searchSkuByKeyword({ current: 1, size: 30 }, text)
  if (isSuccess(result)) {
    skuOptions.value = (result.data?.records || []).map((item) => ({
      label: `${item.skuCode} - ${item.chineseName || ''}`,
      value: item.skuCode
    }))
  }
}

const addItem = () => form.items.push(newLine())
const removeItem = (index: number) => form.items.splice(index, 1)

const handleSave = async () => {
  await formRef.value?.validate()
  if (form.items.some((item) => !item.skuCode || !item.quantity)) {
    message.warning('请完整填写商品 SKU 和数量')
    return
  }
  if (new Set(form.items.map((item) => item.skuCode.trim().toUpperCase())).size !== form.items.length) {
    message.warning('同一人工出库单内 SKU 不能重复')
    return
  }
  saving.value = true
  try {
    const payload: ManualFulfillmentForm = {
      warehouseId: form.warehouseId,
      logisticsProductId: form.logisticsProductId,
      recipientName: form.recipientName,
      recipientPhone: form.recipientPhone,
      recipientAddress: form.recipientAddress,
      items: form.items.map(({ skuCode, quantity }) => ({ skuCode, quantity }))
    }
    const result = editingId.value
      ? await updateManualFulfillment(editingId.value, payload)
      : await createManualFulfillment(payload)
    if (!isSuccess(result)) {
      message.error(result.message || '保存失败')
      return
    }
    message.success('草稿已保存')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}
.address-field {
  grid-column: 1 / -1;
}
.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 10px;
  font-weight: 600;
}
@media (max-width: 720px) {
  .form-grid { grid-template-columns: 1fr; }
  .address-field { grid-column: auto; }
}
</style>
