<template>
  <div>
    <a-alert type="warning" show-icon message="该页面是独立运输台账，所有金额均不进入原有计费。" class="expense-alert" />
    <div class="expense-toolbar">
      <strong>{{ type === 'FUEL' ? '油费记录' : '司机月结' }}</strong>
      <a-button type="primary" @click="edit()">新增记录</a-button>
    </div>
    <a-table row-key="id" :loading="loading" :data-source="records" :columns="columns" :pagination="{ pageSize: 10 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'amount'">{{ record.amount }} {{ record.currency || 'CNY' }}</template>
        <template v-else-if="column.key === 'operate'"><a @click="edit(record)">编辑</a><a-divider type="vertical" /><a @click="remove(record)">删除</a></template>
      </template>
    </a-table>
    <a-modal v-model:open="open" :title="type === 'FUEL' ? '油费记录' : '司机月结'" :confirm-loading="saving" @ok="submit">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-form-item v-if="type === 'FUEL'" label="日期" name="expenseDate" :rules="[{ required: true, message: '请选择日期' }]">
          <a-date-picker v-model:value="form.expenseDate" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
        <a-form-item v-if="type === 'DRIVER_MONTHLY'" label="月份" name="settlementMonth" :rules="[{ required: true, message: '请选择月份' }]">
          <a-month-picker v-model:value="form.settlementMonth" value-format="YYYY-MM" style="width: 100%" />
        </a-form-item>
        <a-form-item v-if="type === 'DRIVER_MONTHLY'" label="司机" name="driverName" :rules="[{ required: true, message: '请输入司机' }]">
          <a-input v-model:value="form.driverName" maxlength="128" />
        </a-form-item>
        <a-form-item label="金额（仅记录）" name="amount" :rules="[{ required: true, message: '请输入金额' }]">
          <a-input-number v-model:value="form.amount" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="币种"><a-select v-model:value="form.currency" :options="currencyOptions" /></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="form.note" :rows="3" maxlength="500" show-count /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { createTransportExpense, deleteTransportExpense, listTransportExpenses, updateTransportExpense } from '@/api/wms/fulfillment'
import type { TransportExpenseForm, TransportExpenseRecord } from '@/api/wms/fulfillment/types'

const props = defineProps<{ type: 'FUEL' | 'DRIVER_MONTHLY'; active: boolean }>()
const loading = ref(false)
const saving = ref(false)
const open = ref(false)
const editingId = ref<number>()
const records = ref<TransportExpenseRecord[]>([])
const formRef = ref<FormInstance>()
const form = reactive<TransportExpenseForm>({ expenseType: props.type, amount: 0, currency: 'CNY' })
const currencyOptions = ['CNY', 'USD', 'EUR', 'RUB'].map(value => ({ label: value, value }))
const columns = computed(() => [
  { title: props.type === 'FUEL' ? '日期' : '月份', dataIndex: props.type === 'FUEL' ? 'expenseDate' : 'settlementMonth', key: 'period' },
  ...(props.type === 'DRIVER_MONTHLY' ? [{ title: '司机', dataIndex: 'driverName', key: 'driverName' }] : []),
  { title: '金额', key: 'amount' }, { title: '备注', dataIndex: 'note', key: 'note' },
  { title: '记录时间', dataIndex: 'createTime', key: 'createTime' }, { title: '操作', key: 'operate', width: 120 }
])

const load = async () => {
  loading.value = true
  try { const res = await listTransportExpenses(props.type); if (isSuccess(res)) records.value = res.data || [] } finally { loading.value = false }
}
watch(() => props.active, value => { if (value) load() }, { immediate: true })
const edit = (record?: TransportExpenseRecord) => {
  editingId.value = record?.id
  Object.assign(form, { expenseType: props.type, expenseDate: record?.expenseDate, settlementMonth: record?.settlementMonth, driverName: record?.driverName || '', amount: record?.amount ?? 0, currency: record?.currency || 'CNY', note: record?.note || '' })
  open.value = true
}
const submit = async () => {
  await formRef.value?.validate(); saving.value = true
  try {
    const res = editingId.value ? await updateTransportExpense(editingId.value, form) : await createTransportExpense(form)
    if (isSuccess(res)) { message.success('运输费用记录已保存'); open.value = false; await load() }
  } finally { saving.value = false }
}
const remove = (record: TransportExpenseRecord) => Modal.confirm({
  title: '删除这条记录？', content: '删除只影响运输台账，不影响任何计费数据。',
  onOk: async () => { const res = await deleteTransportExpense(record.id); if (isSuccess(res)) { message.success('已删除'); await load() } }
})
</script>

<style scoped>
.expense-alert { margin-bottom: 16px; }
.expense-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
