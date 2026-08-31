<template>
  <a-modal
    v-model:open="openModel"
    :title="title"
    :confirm-loading="loading"
    :width="width"
    :ok-button-props="{ disabled: eligible.length === 0 }"
    @ok="$emit('confirm')"
  >
    <div class="confirm-summary">
      <span>可确认：{{ eligible.length }} 单</span>
      <span>不可确认：{{ ineligible.length }} 单</span>
    </div>

    <div class="fulfillment-fields">
      <a-form-item label="物流产品" class="fulfillment-field">
        <a-select
          v-model:value="productModel"
          :options="productOptions"
          allow-clear
          placeholder="使用店铺默认物流产品"
        />
      </a-form-item>
      <a-form-item label="发货仓库" class="fulfillment-field">
        <a-select
          v-model:value="warehouseModel"
          :options="warehouseOptions"
          allow-clear
          placeholder="使用店铺默认仓库"
        />
      </a-form-item>
    </div>

    <div v-if="eligible.length" class="confirm-section">
      <div class="confirm-section-title">可确认订单</div>
      <ul class="confirm-list">
        <li v-for="o in eligible.slice(0, 10)" :key="o.id">
          <span>{{ o.platformOrderId }} (ERP:{{ o.id }})</span>
          <span v-if="o.items && o.items.length" class="confirm-sku">
            {{ o.items[0]?.skuName || '-' }}
            <span v-if="o.items.length > 1"> +{{ o.items.length - 1 }}</span>
          </span>
        </li>
      </ul>
      <div v-if="eligible.length > 10" class="confirm-minor">
        仅显示前 10 条…
      </div>
    </div>

    <div v-if="ineligible.length" class="confirm-section confirm-warn">
      <div class="confirm-section-title">不可确认订单</div>
      <ul class="confirm-list">
        <li v-for="o in ineligible.slice(0, 10)" :key="o.id">
          <span>{{ o.platformOrderId || '-' }} (ERP:{{ o.id }})</span>
          <span class="confirm-reason">{{ o.reason }}</span>
        </li>
      </ul>
      <div v-if="ineligible.length > 10" class="confirm-minor">
        仅显示前 10 条…
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { BaseOrderVO } from '@/api/order/types'
import { listOwnerLogisticsProducts } from '@/api/wms/logistics-product'
import { getWarehouseOptions } from '@/api/wms/warehouse'

interface Props {
  loading?: boolean
  eligible: BaseOrderVO[]
  ineligible: (BaseOrderVO & { reason: string })[]
  title?: string
  width?: string
}

withDefaults(defineProps<Props>(), {
  loading: false,
  title: '确认发货',
  width: '640px'
})

const openModel = defineModel<boolean>('open', { required: true })
const productModel = defineModel<number | undefined>('logisticsProductId')
const warehouseModel = defineModel<number | undefined>('wmsWarehouseId')
const productOptions = ref<{ label: string; value: number }[]>([])
const warehouseOptions = ref<{ label: string; value: number }[]>([])

watch(openModel, async value => {
  if (!value) return
  if (!productOptions.value.length) {
    const response = await listOwnerLogisticsProducts()
    if (response.code === 200) {
      productOptions.value = (response.data || []).map(item => ({
        label: `${item.productName} (${item.currency || 'RUB'} ${Number(item.unitPrice).toFixed(2)})`,
        value: item.id
      }))
    }
  }
  if (!warehouseOptions.value.length) {
    const response = await getWarehouseOptions()
    if (response.code === 200) {
      warehouseOptions.value = (response.data || [])
        .filter(item => item.warehouseType === 'OWN')
        .map(item => ({ label: `${item.warehouseName} (${item.warehouseCode})`, value: item.id }))
    }
  }
})

defineEmits<{
  confirm: []
}>()
</script>

<style scoped>
.confirm-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.fulfillment-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.fulfillment-field {
  margin-bottom: 0;
}

.confirm-section {
  margin-top: 12px;
}

.confirm-section-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.confirm-section.confirm-warn {
  background: #fff7f7;
  border: 1px solid #ffcdd2;
  border-radius: 6px;
  padding: 8px;
}

.confirm-list {
  padding-left: 16px;
}

.confirm-list li {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 4px 0;
}

.confirm-sku {
  color: #8c8c8c;
  font-size: 12px;
}

.confirm-reason {
  color: #a00;
}

.confirm-minor {
  color: #888;
  font-size: 12px;
}
</style>
