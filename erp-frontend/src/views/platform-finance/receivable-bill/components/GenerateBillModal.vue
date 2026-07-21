<template>
  <a-modal
    :open="open"
    title="生成 / 重算账单"
    :confirm-loading="submitting"
    ok-text="生成"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-alert
      type="info"
      show-icon
      message="按账期汇总生成月度账单：草稿态账单将被重算覆盖，已确认/已付款账单跳过。"
      style="margin-bottom: 16px"
    />
    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="账期" required>
        <a-date-picker
          v-model:value="billMonth"
          picker="month"
          placeholder="选择账期"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="WMS服务商">
        <a-select
          v-model:value="wmsTenantId"
          :options="operatorOptions"
          :loading="operatorLoading"
          placeholder="全部服务商"
          allow-clear
          show-search
          option-filter-prop="label"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import { isSuccess } from '@/api'
import { listWmsOperators } from '@/api/tenant'
import { generateMonthlyBill } from '@/api/platform-finance/receivable'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'generated'): void
}>()

const billMonth = ref<Dayjs>()
const wmsTenantId = ref<number>()
const submitting = ref(false)

const operatorLoading = ref(false)
const operatorOptions = ref<{ label: string; value: number }[]>([])

async function loadOperators() {
  if (operatorOptions.value.length) return
  operatorLoading.value = true
  try {
    const res = await listWmsOperators()
    if (isSuccess(res) && res.data) {
      operatorOptions.value = res.data.map(o => ({ label: o.tenantName, value: o.id }))
    }
  } catch (e) {
    console.error('加载 WMS 服务商失败', e)
  } finally {
    operatorLoading.value = false
  }
}

watch(
  () => props.open,
  v => {
    if (v) {
      loadOperators()
      billMonth.value = undefined
      wmsTenantId.value = undefined
    }
  }
)

async function handleOk() {
  if (!billMonth.value) {
    message.warning('请选择账期')
    return
  }
  submitting.value = true
  try {
    const res = await generateMonthlyBill({
      billMonth: billMonth.value.format('YYYY-MM'),
      wmsTenantId: wmsTenantId.value
    })
    if (isSuccess(res) && res.data) {
      const { created, recalculated, skipped } = res.data
      message.success(`生成完成：新建 ${created}，重算 ${recalculated}，跳过 ${skipped}`)
      emit('generated')
      emit('update:open', false)
    } else {
      message.error(res.message || '生成失败')
    }
  } catch (e: any) {
    message.error(e?.message || '生成失败')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  emit('update:open', false)
}
</script>
