<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <div class="title">合同资金</div>
        <div class="subtitle">管理服务商租赁、押金、货架认购款及到期退款</div>
      </div>
      <a-space>
        <a-button :loading="loading" @click="load">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button type="primary" @click="createOpen = true">
          <template #icon><PlusOutlined /></template>
          新建合同
        </a-button>
      </a-space>
    </div>

    <a-table
      row-key="id"
      :loading="loading"
      :data-source="rows"
      :columns="columns"
      :pagination="{ pageSize: 20 }"
      :scroll="{ x: 1450 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'operator'">
          {{ operatorName(record.wmsTenantId) }}
        </template>
        <template v-else-if="column.key === 'warehouse'">
          {{ warehouseName(record.warehouseId) }}
        </template>
        <template v-else-if="column.key === 'refund'">
          {{ money(record.refundableAmount) }}（{{ Number(record.refundableRate) * 100 }}%）
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.contractStatus === 'ACTIVE' ? 'green' : 'default'">
            {{ record.contractStatus === 'ACTIVE' ? '履约中' : '已结算' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'operate'">
          <a-space size="small">
            <a @click="showFunds(record)">资金明细</a>
            <a v-if="record.contractStatus === 'ACTIVE'" @click="openRecognize(record)">按月确认</a>
            <a v-if="record.contractStatus === 'ACTIVE'" @click="openRefund(record)">办理退款</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="createOpen"
      title="新建服务合同"
      :width="720"
      :confirm-loading="saving"
      @ok="createContract"
    >
      <a-alert
        type="info"
        show-icon
        message="默认3列货架：月租20,000元/列，押金60,000元，认购款180,000元；履约满1年后退认购款70%。"
        class="notice"
      />
      <a-form :model="form" :label-col="{ span: 6 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="合同编号" required>
              <a-input v-model:value="form.contractNo" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="WMS服务商" required>
              <a-select v-model:value="form.wmsTenantId" :options="operatorOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="仓库" required>
              <a-select v-model:value="form.warehouseId" :options="warehouseOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="有效期" required>
              <a-range-picker v-model:value="dateRange" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="货架列数">
              <a-input-number v-model:value="form.rackUnitCount" :min="1" :precision="0" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="货架编号">
              <a-select
                v-model:value="rackNos"
                mode="tags"
                placeholder="每列一个编号，可暂不填写"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单列月租">
              <a-input-number v-model:value="form.monthlyRentPerUnit" :min="0" addon-after="元" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="仓库押金">
              <a-input-number v-model:value="form.warehouseDeposit" :min="0" addon-after="元" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="认购款">
              <a-input-number v-model:value="form.subscriptionTotal" :min="0" addon-after="元" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="可退比例">
              <a-input-number
                v-model:value="refundablePercent"
                :min="0"
                :max="100"
                addon-after="%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" :label-col="{ span: 3 }">
              <a-textarea v-model:value="form.remark" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="fundOpen" title="合同资金明细" :width="760">
      <a-descriptions v-if="current" bordered size="small" :column="2">
        <a-descriptions-item label="合同">{{ current.contractNo }}</a-descriptions-item>
        <a-descriptions-item label="服务商">{{
          operatorName(current.wmsTenantId)
        }}</a-descriptions-item>
        <a-descriptions-item label="可退认购款">{{
          money(current.refundableAmount)
        }}</a-descriptions-item>
        <a-descriptions-item label="不退服务费">{{
          money(current.serviceAmount)
        }}</a-descriptions-item>
      </a-descriptions>
      <a-table
        row-key="id"
        :data-source="funds"
        :columns="fundColumns"
        :pagination="false"
        size="small"
        class="fund-table"
      />
    </a-drawer>

    <a-modal v-model:open="recognizeOpen" title="按月确认服务收入" @ok="recognize">
      <a-form layout="vertical">
        <a-form-item label="确认月份" required>
          <a-date-picker v-model:value="recognizeMonth" picker="month" style="width: 100%" />
        </a-form-item>
        <a-alert
          type="warning"
          show-icon
          :message="`每月确认不退认购服务费 ${money(current?.monthlyServiceRecognition || 0)}`"
        />
      </a-form>
    </a-modal>

    <a-modal v-model:open="refundOpen" title="合同到期退款核验" @ok="refund">
      <a-alert
        type="warning"
        show-icon
        message="系统只在三项核验全部确认且合同履约满1年后登记退款。"
        class="notice"
      />
      <a-space direction="vertical">
        <a-checkbox v-model:checked="refundChecks.noDefault">确认服务商无违约</a-checkbox>
        <a-checkbox v-model:checked="refundChecks.noDebt">确认服务商无欠费</a-checkbox>
        <a-checkbox v-model:checked="refundChecks.noRemainingGoods">确认仓库无遗留货物</a-checkbox>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import { listWmsOperators } from '@/api/tenant'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import {
  createServiceContract,
  listContractFunds,
  listServiceContracts,
  recognizeContractService,
  refundServiceContract
} from '@/api/platform-finance/service-contract'
import type {
  ContractFundLedger,
  ServiceContract,
  ServiceContractCreate
} from '@/api/platform-finance/service-contract'

const loading = ref(false)
const saving = ref(false)
const rows = ref<ServiceContract[]>([])
const funds = ref<ContractFundLedger[]>([])
const operators = ref<{ id: number; tenantName: string }[]>([])
const warehouses = ref<{ id: number; warehouseName: string }[]>([])
const current = ref<ServiceContract>()
const createOpen = ref(false)
const fundOpen = ref(false)
const recognizeOpen = ref(false)
const refundOpen = ref(false)
const recognizeMonth = ref<Dayjs>()
const dateRange = ref<[Dayjs, Dayjs]>()
const rackNos = ref<string[]>([])
const refundablePercent = ref(70)
const refundChecks = reactive({ noDefault: false, noDebt: false, noRemainingGoods: false })
const form = reactive({
  contractNo: '',
  wmsTenantId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  rackUnitCount: 3,
  monthlyRentPerUnit: 20000,
  warehouseDeposit: 60000,
  subscriptionTotal: 180000,
  remark: ''
})

const operatorOptions = computed(() =>
  operators.value.map(item => ({ value: item.id, label: item.tenantName }))
)
const warehouseOptions = computed(() =>
  warehouses.value.map(item => ({ value: item.id, label: item.warehouseName }))
)
const columns = [
  { title: '合同编号', dataIndex: 'contractNo', width: 170, fixed: 'left' },
  { title: 'WMS服务商', key: 'operator', width: 150 },
  { title: '仓库', key: 'warehouse', width: 140 },
  { title: '合同期限', customRender: ({ record }: any) => `${record.startDate} 至 ${record.endDate}`, width: 210 },
  { title: '货架列数', dataIndex: 'rackUnitCount', width: 90 },
  { title: '月租/列', dataIndex: 'monthlyRentPerUnit', customRender: ({ value }: any) => money(value), width: 110 },
  { title: '押金', dataIndex: 'warehouseDeposit', customRender: ({ value }: any) => money(value), width: 110 },
  { title: '认购款', dataIndex: 'subscriptionTotal', customRender: ({ value }: any) => money(value), width: 110 },
  { title: '到期可退', key: 'refund', width: 150 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'operate', width: 210, fixed: 'right' }
]
const fundColumns = [
  { title: '时间', dataIndex: 'createTime', width: 160 },
  { title: '资金项目', dataIndex: 'fundComponent', width: 190 },
  { title: '类型', dataIndex: 'transactionType', width: 110 },
  { title: '账期', dataIndex: 'accountingMonth', width: 90 },
  {
    title: '金额',
    customRender: ({ record }: any) =>
      `${record.direction === 'OUT' ? '-' : '+'}${money(record.amount)}`,
    width: 120
  }
]

function money(value: number | string) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
}
function operatorName(id: number) {
  return operators.value.find(item => item.id === id)?.tenantName || `#${id}`
}
function warehouseName(id: number) {
  return warehouses.value.find(item => item.id === id)?.warehouseName || `#${id}`
}
async function load() {
  loading.value = true
  try {
    const response = await listServiceContracts()
    if (isSuccess(response)) rows.value = response.data || []
  } finally {
    loading.value = false
  }
}
async function createContract() {
  if (!form.contractNo.trim() || !form.wmsTenantId || !form.warehouseId || !dateRange.value) {
    message.warning('请补齐合同编号、服务商、仓库和有效期')
    return
  }
  if (rackNos.value.length && rackNos.value.length !== form.rackUnitCount) {
    message.warning('货架编号数量必须等于货架列数')
    return
  }
  saving.value = true
  try {
    const payload: ServiceContractCreate = {
      ...form,
      contractNo: form.contractNo.trim(),
      wmsTenantId: form.wmsTenantId,
      warehouseId: form.warehouseId,
      startDate: dateRange.value[0].format('YYYY-MM-DD'),
      endDate: dateRange.value[1].format('YYYY-MM-DD'),
      refundableRate: refundablePercent.value / 100,
      rackNos: rackNos.value
    }
    const response = await createServiceContract(payload)
    if (isSuccess(response)) {
      message.success('合同资金已登记')
      createOpen.value = false
      await load()
    }
  } finally {
    saving.value = false
  }
}
async function showFunds(record: ServiceContract) {
  current.value = record
  const response = await listContractFunds(record.id)
  if (isSuccess(response)) funds.value = response.data || []
  fundOpen.value = true
}
function openRecognize(record: ServiceContract) {
  current.value = record
  recognizeMonth.value = undefined
  recognizeOpen.value = true
}
async function recognize() {
  if (!current.value || !recognizeMonth.value) {
    message.warning('请选择确认月份')
    return
  }
  const response = await recognizeContractService(
    current.value.id,
    recognizeMonth.value.format('YYYY-MM')
  )
  if (isSuccess(response)) {
    message.success('本月服务收入已确认')
    recognizeOpen.value = false
  }
}
function openRefund(record: ServiceContract) {
  current.value = record
  Object.assign(refundChecks, { noDefault: false, noDebt: false, noRemainingGoods: false })
  refundOpen.value = true
}
async function refund() {
  if (!current.value || !Object.values(refundChecks).every(Boolean)) {
    message.warning('请完成全部退款核验')
    return
  }
  const response = await refundServiceContract({ contractId: current.value.id, ...refundChecks })
  if (isSuccess(response)) {
    message.success('退款流水已登记')
    refundOpen.value = false
    await load()
  }
}

onMounted(async () => {
  const [operatorResponse, warehouseResponse] = await Promise.all([
    listWmsOperators(),
    getWarehouseOptions()
  ])
  if (isSuccess(operatorResponse)) operators.value = operatorResponse.data || []
  if (isSuccess(warehouseResponse)) warehouses.value = warehouseResponse.data || []
  await load()
})
</script>

<style scoped>
.page { min-height: 100%; padding: 16px; background: #fff; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.title { font-size: 18px; font-weight: 600; }
.subtitle { margin-top: 3px; color: rgba(0, 0, 0, 0.45); font-size: 12px; }
.notice { margin-bottom: 16px; }
.fund-table { margin-top: 16px; }
</style>
