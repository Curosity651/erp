<template>
  <a-modal :open="open" title="Ozon 交接单规则" :width="820" :footer="null" @cancel="close">
    <a-alert
      type="info"
      show-icon
      message="规则按店铺和配送方式生效。开启后，该配送方式的订单在海外仓签出前必须生成可下载的 Ozon 交接单。"
      style="margin-bottom: 12px"
    />
    <a-table
      :data-source="rules"
      :loading="loading"
      :pagination="false"
      :row-key="record => `${record.shopId}-${record.deliveryMethodId}`"
      size="small"
    >
      <a-table-column title="店铺" :width="180">
        <template #default="{ record }">{{ record.shopName || record.shopId }}</template>
      </a-table-column>
      <a-table-column title="配送方式" :width="260">
        <template #default="{ record }">
          <div>{{ record.deliveryMethodName || '未返回名称' }}</div>
          <div class="sub">ID: {{ record.deliveryMethodId }}</div>
        </template>
      </a-table-column>
      <a-table-column title="需要交接单" :width="120" align="center">
        <template #default="{ record }">
          <a-switch v-model:checked="record.requiredChecked" />
        </template>
      </a-table-column>
      <a-table-column title="容器数" :width="100">
        <template #default="{ record }">
          <a-input-number v-model:value="record.containersCount" :min="1" :max="99" style="width: 72px" />
        </template>
      </a-table-column>
      <a-table-column title="操作" :width="80" align="center">
        <template #default="{ record }">
          <a-button type="link" size="small" :loading="savingId === record.deliveryMethodId" @click="save(record)">
            保存
          </a-button>
        </template>
      </a-table-column>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { listOzonDeliveryMethodRules, saveOzonDeliveryMethodRule } from '@/api/order/ozon-order'
import type { OzonDeliveryMethodRule } from '@/api/order/ozon-order/types'

type EditableRule = OzonDeliveryMethodRule & { requiredChecked: boolean }
const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (event: 'update:open', value: boolean): void }>()
const loading = ref(false)
const savingId = ref<number>()
const rules = ref<EditableRule[]>([])

watch(() => props.open, value => { if (value) load() }, { immediate: true })

async function load() {
  loading.value = true
  try {
    const res = await listOzonDeliveryMethodRules()
    if (isSuccess(res)) rules.value = (res.data || []).map(rule => ({ ...rule, requiredChecked: rule.actRequired === 1 }))
  } finally {
    loading.value = false
  }
}

async function save(rule: EditableRule) {
  savingId.value = rule.deliveryMethodId
  try {
    const res = await saveOzonDeliveryMethodRule({
      id: rule.id,
      shopId: rule.shopId,
      shopName: rule.shopName,
      deliveryMethodId: rule.deliveryMethodId,
      deliveryMethodName: rule.deliveryMethodName,
      actRequired: rule.requiredChecked ? 1 : 0,
      containersCount: rule.containersCount,
      enabled: rule.enabled
    })
    if (isSuccess(res)) {
      message.success('规则已保存')
      await load()
    }
  } finally {
    savingId.value = undefined
  }
}

function close() { emit('update:open', false) }
</script>

<style scoped>.sub { color: #8c8c8c; font-size: 12px; }</style>
