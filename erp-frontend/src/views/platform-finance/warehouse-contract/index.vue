<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <div class="title">合同资金</div>
        <div class="subtitle">管理WMS服务商的货架租赁、押金、认购款和到期退款</div>
      </div>
      <a-space>
        <a-button :loading="loading" @click="load">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button type="primary" @click="openCreate">
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
      :scroll="{ x: 1600 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'operator'">
          {{ operatorName(record.wmsTenantId) }}
        </template>
        <template v-else-if="column.key === 'warehouse'">
          {{ warehouseName(record.warehouseId) }}
        </template>
        <template v-else-if="column.key === 'refund'">
          {{ money(record.refundableAmount) }}（{{ percent(record.refundableRate) }}）
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record)">
            {{ statusText(record) }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'operate'">
          <a-space size="small">
            <a @click="showFunds(record)">资金明细</a>
            <a
              v-if="record.paymentStatus === 'PENDING'"
              class="primary-action"
              @click="openReceive(record)"
            >
              确认收款
            </a>
            <a
              v-if="record.paymentStatus === 'PENDING'"
              class="danger-action"
              @click="cancelContract(record)"
            >
              取消合同
            </a>
            <a
              v-if="record.contractStatus === 'ACTIVE' && record.paymentStatus === 'PAID'"
              @click="openRecognize(record)"
            >
              按月确认
            </a>
            <a
              v-if="record.contractStatus === 'ACTIVE' && record.paymentStatus === 'PAID'"
              @click="openRefund(record)"
            >
              办理退款
            </a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="createOpen"
      title="新建服务合同"
      :width="760"
      :confirm-loading="saving"
      @ok="createContract"
    >
      <a-alert
        type="info"
        show-icon
        message="合同保存后处于待收款状态；确认资金到账时才激活货架并生成资金流水。"
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
              <a-select
                v-model:value="form.wmsTenantId"
                :options="operatorOptions"
                show-search
                option-filter-prop="label"
                @change="loadRackOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="仓库" required>
              <a-select
                v-model:value="form.warehouseId"
                :options="warehouseOptions"
                show-search
                option-filter-prop="label"
                @change="loadRackOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同有效期" required>
              <a-range-picker v-model:value="dateRange" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="货架数量" required>
              <a-input-number
                v-model:value="form.rackUnitCount"
                :min="1"
                :precision="0"
                style="width: 100%"
                @change="handleRackCountChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="实际货架" required>
              <a-select
                v-model:value="rackNos"
                mode="multiple"
                :options="selectableRackOptions"
                :loading="rackLoading"
                :disabled="!form.rackUnitCount"
                :max-tag-count="2"
                :placeholder="form.rackUnitCount ? `请选择 ${form.rackUnitCount} 个实际货架` : '请先填写货架数量'"
                style="width: 100%"
                @change="handleRackSelectionChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单架月租">
              <a-input-number
                v-model:value="form.monthlyRentPerUnit"
                :min="0"
                :precision="2"
                addon-after="元"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="仓储押金">
              <a-input-number
                v-model:value="form.warehouseDeposit"
                :precision="2"
                addon-after="元"
                style="width: 100%"
                disabled
              />
              <div class="field-tip">固定仓储押金 {{ money(FIXED_WAREHOUSE_DEPOSIT) }}</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="认购款">
              <a-input-number
                v-model:value="form.subscriptionTotal"
                :precision="2"
                addon-after="元"
                style="width: 100%"
                disabled
              />
              <div class="field-tip">按每架 {{ money(SUBSCRIPTION_PER_RACK) }} 自动计算</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="到期可退比例">
              <a-input-number
                v-model:value="refundablePercent"
                :min="0"
                :max="100"
                addon-after="%"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="合同文件" :label-col="{ span: 3 }" required>
              <sys-file-upload
                v-model="form.contractFileId"
                bucket-key="private-files"
                button-text="上传PDF合同"
                :allowed-types="['application/pdf']"
              />
              <div class="field-tip">必须上传双方确认的真实 PDF 合同，最大 10MB</div>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" :label-col="{ span: 3 }">
              <a-textarea v-model:value="form.remark" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="money-summary">
        <span>首次应收：<b>{{ money(firstCollectionAmount) }}</b></span>
        <span>到期满足条件可退：<b>{{ money(refundableAmount) }}</b></span>
        <span>不退服务费：<b>{{ money(serviceAmount) }}</b></span>
      </div>
    </a-modal>

    <a-modal
      v-model:open="receiveOpen"
      title="确认合同资金到账"
      :confirm-loading="saving"
      @ok="receiveContract"
    >
      <a-alert
        type="warning"
        show-icon
        message="确认后将激活合同货架并登记真实收款，操作不可重复。"
        class="notice"
      />
      <a-descriptions v-if="current" bordered size="small" :column="1">
        <a-descriptions-item label="合同">{{ current.contractNo }}</a-descriptions-item>
        <a-descriptions-item label="押金">{{ money(current.warehouseDeposit) }}</a-descriptions-item>
        <a-descriptions-item label="认购款">{{ money(current.subscriptionTotal) }}</a-descriptions-item>
        <a-descriptions-item label="合计到账">
          <b>{{ money(Number(current.warehouseDeposit) + Number(current.subscriptionTotal)) }}</b>
        </a-descriptions-item>
      </a-descriptions>
      <a-textarea
        v-model:value="receiveRemark"
        :rows="2"
        placeholder="可填写银行流水号或收款凭证"
        class="modal-textarea"
      />
    </a-modal>

    <a-drawer v-model:open="fundOpen" title="合同资金明细" :width="820">
      <a-descriptions v-if="current" bordered size="small" :column="2">
        <a-descriptions-item label="合同">{{ current.contractNo }}</a-descriptions-item>
        <a-descriptions-item label="服务商">
          {{ operatorName(current.wmsTenantId) }}
        </a-descriptions-item>
        <a-descriptions-item label="可退认购款">
          {{ money(current.refundableAmount) }}
        </a-descriptions-item>
        <a-descriptions-item label="不退服务费">
          {{ money(current.serviceAmount) }}
        </a-descriptions-item>
        <a-descriptions-item label="合同文件" :span="2">
          <sys-file-upload
            v-if="current.contractFileId"
            :model-value="current.contractFileId"
            :disabled="true"
            :hide-upload-button="true"
          />
          <span v-else>-</span>
        </a-descriptions-item>
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

    <a-modal
      v-model:open="recognizeOpen"
      title="按月确认服务收入"
      :width="680"
      :confirm-loading="recognizeLoading"
      :ok-button-props="{ disabled: !recognizeMonth }"
      ok-text="确认本月"
      @ok="recognize"
    >
      <a-spin :spinning="recognizeLoading">
        <div class="recognition-term">
          认购期：{{ current?.startDate }} 至 {{ current?.endDate }}
        </div>
        <div v-if="recognitionMonthOptions.length" class="recognition-month-grid">
          <button
            v-for="month in recognitionMonthOptions"
            :key="month.value"
            type="button"
            class="recognition-month"
            :class="{
              recognized: recognizedMonths.includes(month.value),
              selected: recognizeMonth === month.value
            }"
            :disabled="recognizedMonths.includes(month.value)"
            @click="recognizeMonth = month.value"
          >
            <span>{{ month.label }}</span>
            <span class="recognition-month-status">
              <CheckCircleOutlined v-if="recognizedMonths.includes(month.value)" />
              {{ recognizedMonths.includes(month.value) ? '已确认' : '待确认' }}
            </span>
          </button>
        </div>
        <a-empty v-else description="合同认购期内没有可确认月份" />
        <a-alert
          type="info"
          show-icon
          :message="recognizeMonth
            ? `${recognizeMonth} 确认不退认购服务费 ${money(current?.monthlyServiceRecognition || 0)}`
            : `每月确认金额 ${money(current?.monthlyServiceRecognition || 0)}`"
          class="recognition-alert"
        />
      </a-spin>
    </a-modal>

    <a-modal
      v-model:open="refundOpen"
      title="合同到期退款核验"
      :confirm-loading="saving"
      :ok-button-props="{ disabled: !refundCheck?.refundable || !noDefault }"
      @ok="refund"
    >
      <a-spin :spinning="refundLoading">
        <a-alert
          v-if="refundCheck"
          :type="refundCheck.refundable ? 'success' : 'warning'"
          show-icon
          :message="refundCheck.message"
          class="notice"
        />
        <a-descriptions v-if="refundCheck" bordered size="small" :column="1">
          <a-descriptions-item label="履约期限">
            <CheckCircleOutlined v-if="refundCheck.termSatisfied" class="ok" />
            <CloseCircleOutlined v-else class="bad" />
            最早退款日期：{{ refundCheck.eligibleDate }}
          </a-descriptions-item>
          <a-descriptions-item label="应收欠款">
            <CheckCircleOutlined v-if="refundCheck.noDebt" class="ok" />
            <CloseCircleOutlined v-else class="bad" />
            {{ money(refundCheck.unpaidAmount) }}
          </a-descriptions-item>
          <a-descriptions-item label="遗留货物">
            <CheckCircleOutlined v-if="refundCheck.noRemainingGoods" class="ok" />
            <CloseCircleOutlined v-else class="bad" />
            库存 {{ refundCheck.remainingQuantity }} 件，预占 {{ refundCheck.reservedQuantity }} 件
          </a-descriptions-item>
        </a-descriptions>
        <a-checkbox v-model:checked="noDefault" class="manual-check">
          人工确认该服务商无违约、无纠纷
        </a-checkbox>
        <a-textarea v-model:value="refundRemark" :rows="2" placeholder="退款凭证或备注" />
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  PlusOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import SysFileUpload from '@/components/Upload/SysFileUpload.vue'
import { listWmsOperators } from '@/api/tenant'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import { previewRacks } from '@/api/wms/rack'
import {
  createServiceContract,
  cancelServiceContract,
  getContractRefundCheck,
  listContractFunds,
  listServiceContracts,
  receiveServiceContract,
  recognizeContractService,
  refundServiceContract
} from '@/api/platform-finance/service-contract'
import { contractMonths, recognizedMonthValues } from './contract-recognition'
import type {
  ContractFundLedger,
  ContractRefundCheck,
  ServiceContract,
  ServiceContractCreate
} from '@/api/platform-finance/service-contract'

const loading = ref(false)
const saving = ref(false)
const rackLoading = ref(false)
const refundLoading = ref(false)
const recognizeLoading = ref(false)
const rows = ref<ServiceContract[]>([])
const funds = ref<ContractFundLedger[]>([])
const operators = ref<{ id: number; tenantName: string }[]>([])
const warehouses = ref<any[]>([])
const current = ref<ServiceContract>()
const createOpen = ref(false)
const receiveOpen = ref(false)
const fundOpen = ref(false)
const recognizeOpen = ref(false)
const refundOpen = ref(false)
const receiveRemark = ref('')
const refundRemark = ref('')
const noDefault = ref(false)
const refundCheck = ref<ContractRefundCheck>()
const recognizeMonth = ref<string>()
const recognizedMonths = ref<string[]>([])
const dateRange = ref<[Dayjs, Dayjs]>()
const rackNos = ref<string[]>([])
const rackOptions = ref<{ label: string; value: string }[]>([])
const refundablePercent = ref(70)
const FIXED_WAREHOUSE_DEPOSIT = 60000
const SUBSCRIPTION_PER_RACK = 60000
const DEFAULT_RACK_COUNT = 3
const form = reactive({
  contractNo: '',
  wmsTenantId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  rackUnitCount: DEFAULT_RACK_COUNT as number | undefined,
  monthlyRentPerUnit: 20000,
  warehouseDeposit: FIXED_WAREHOUSE_DEPOSIT,
  subscriptionTotal: DEFAULT_RACK_COUNT * SUBSCRIPTION_PER_RACK,
  contractFileId: undefined as number | undefined,
  remark: ''
})

const operatorOptions = computed(() =>
  operators.value.map(row => ({ value: row.id, label: row.tenantName }))
)
const warehouseOptions = computed(() =>
  warehouses.value.map(row => ({ value: row.id, label: row.warehouseName }))
)
const selectableRackOptions = computed(() => {
  const limit = form.rackUnitCount || 0
  const reachedLimit = limit > 0 && rackNos.value.length >= limit
  return rackOptions.value.map(option => ({
    ...option,
    disabled: reachedLimit && !rackNos.value.includes(option.value)
  }))
})
const refundableAmount = computed(
  () => (Number(form.subscriptionTotal) * Number(refundablePercent.value)) / 100
)
const serviceAmount = computed(() => Number(form.subscriptionTotal) - refundableAmount.value)
const firstCollectionAmount = computed(
  () => Number(form.warehouseDeposit) + Number(form.subscriptionTotal)
)
const recognitionMonthOptions = computed(() =>
  current.value ? contractMonths(current.value.startDate, current.value.endDate) : []
)

const columns = [
  { title: '合同编号', dataIndex: 'contractNo', width: 170, fixed: 'left' },
  { title: 'WMS服务商', key: 'operator', width: 150 },
  { title: '仓库', key: 'warehouse', width: 140 },
  {
    title: '合同期限',
    width: 210,
    customRender: ({ record }: any) => `${record.startDate} 至 ${record.endDate}`
  },
  { title: '货架数', dataIndex: 'rackUnitCount', width: 90 },
  {
    title: '月租/架',
    dataIndex: 'monthlyRentPerUnit',
    width: 115,
    customRender: ({ value }: any) => money(value)
  },
  {
    title: '押金',
    dataIndex: 'warehouseDeposit',
    width: 115,
    customRender: ({ value }: any) => money(value)
  },
  {
    title: '认购款',
    dataIndex: 'subscriptionTotal',
    width: 120,
    customRender: ({ value }: any) => money(value)
  },
  { title: '到期可退', key: 'refund', width: 165 },
  { title: '状态', key: 'status', width: 100 },
  { title: '到账时间', dataIndex: 'receivedTime', width: 165 },
  { title: '操作', key: 'operate', width: 250, fixed: 'right' }
]
const fundColumns = [
  { title: '时间', dataIndex: 'createTime', width: 165 },
  {
    title: '资金项目',
    dataIndex: 'fundComponent',
    width: 170,
    customRender: ({ value }: any) => componentText(value)
  },
  {
    title: '业务类型',
    dataIndex: 'transactionType',
    width: 110,
    customRender: ({ value }: any) => transactionText(value)
  },
  { title: '账期', dataIndex: 'accountingMonth', width: 90 },
  {
    title: '金额',
    width: 130,
    customRender: ({ record }: any) =>
      `${record.direction === 'OUT' ? '-' : '+'}${money(record.amount)}`
  },
  { title: '说明', dataIndex: 'remark' }
]

async function load() {
  loading.value = true
  try {
    const response = await listServiceContracts()
    if (isSuccess(response)) rows.value = response.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetCreate()
  createOpen.value = true
}

async function loadRackOptions() {
  rackNos.value = []
  rackOptions.value = []
  if (!form.warehouseId || !form.wmsTenantId) return
  rackLoading.value = true
  try {
    const response = await previewRacks(form.warehouseId)
    if (isSuccess(response)) {
      rackOptions.value = (response.data || [])
        .filter(rack => rack.status === 'IDLE')
        .map(rack => ({
          value: rack.rackNo,
          label: `${rack.rackNo}（空闲）`
        }))
    }
  } finally {
    rackLoading.value = false
  }
}

function handleRackCountChange(value: number | null) {
  if (value == null) {
    form.rackUnitCount = undefined
    form.warehouseDeposit = FIXED_WAREHOUSE_DEPOSIT
    form.subscriptionTotal = 0
    rackNos.value = []
    return
  }
  const rackCount = Math.max(1, Math.floor(Number(value)))
  form.rackUnitCount = rackCount
  form.warehouseDeposit = FIXED_WAREHOUSE_DEPOSIT
  form.subscriptionTotal = rackCount * SUBSCRIPTION_PER_RACK
  if (rackNos.value.length > form.rackUnitCount) {
    rackNos.value = rackNos.value.slice(0, form.rackUnitCount)
  }
}

function handleRackSelectionChange(values: string[]) {
  const limit = form.rackUnitCount || 0
  if (limit === 0) {
    rackNos.value = []
    return
  }
  if (values.length > limit) {
    rackNos.value = values.slice(0, limit)
    message.warning(`最多只能选择 ${limit} 个实际货架`)
  }
}

async function createContract() {
  if (
    !form.contractNo.trim() ||
    !form.wmsTenantId ||
    !form.warehouseId ||
    !form.rackUnitCount ||
    !dateRange.value ||
    !form.contractFileId
  ) {
    message.warning('请填写合同信息并上传PDF合同文件')
    return
  }
  if (rackNos.value.length !== form.rackUnitCount) {
    message.warning(`请选择 ${form.rackUnitCount} 个实际货架`)
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
      rackNos: rackNos.value,
      contractFileId: form.contractFileId
    }
    const response = await createServiceContract(payload)
    if (isSuccess(response)) {
      message.success('合同已保存，等待确认收款')
      createOpen.value = false
      await load()
    }
  } finally {
    saving.value = false
  }
}

function openReceive(record: ServiceContract) {
  current.value = record
  receiveRemark.value = ''
  receiveOpen.value = true
}

async function receiveContract() {
  if (!current.value) return
  saving.value = true
  try {
    const response = await receiveServiceContract(current.value.id, receiveRemark.value.trim())
    if (isSuccess(response)) {
      message.success('合同资金已到账，货架租赁已激活')
      receiveOpen.value = false
      await load()
    }
  } finally {
    saving.value = false
  }
}

function cancelContract(record: ServiceContract) {
  Modal.confirm({
    title: '取消待收款合同',
    content: `确定取消合同 ${record.contractNo} 吗？取消后将立即释放已预留货架。`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '返回',
    async onOk() {
      const response = await cancelServiceContract(record.id)
      if (isSuccess(response)) {
        message.success('合同已取消，货架预留已释放')
        await load()
      }
    }
  })
}

async function showFunds(record: ServiceContract) {
  current.value = record
  const response = await listContractFunds(record.id)
  if (isSuccess(response)) funds.value = response.data || []
  fundOpen.value = true
}

async function openRecognize(record: ServiceContract) {
  current.value = record
  recognizeMonth.value = undefined
  recognizedMonths.value = []
  recognizeOpen.value = true
  recognizeLoading.value = true
  try {
    const response = await listContractFunds(record.id)
    if (isSuccess(response)) {
      recognizedMonths.value = recognizedMonthValues(response.data || [])
    }
  } finally {
    recognizeLoading.value = false
  }
}

async function recognize() {
  if (!current.value || !recognizeMonth.value) {
    message.warning('请选择确认月份')
    return
  }
  recognizeLoading.value = true
  try {
    const month = recognizeMonth.value
    const response = await recognizeContractService(current.value.id, month)
    if (isSuccess(response)) {
      recognizedMonths.value = Array.from(new Set([...recognizedMonths.value, month])).sort()
      recognizeMonth.value = undefined
      message.success(`${month} 服务收入已确认`)
    }
  } finally {
    recognizeLoading.value = false
  }
}

async function openRefund(record: ServiceContract) {
  current.value = record
  refundCheck.value = undefined
  noDefault.value = false
  refundRemark.value = ''
  refundOpen.value = true
  refundLoading.value = true
  try {
    const response = await getContractRefundCheck(record.id)
    if (isSuccess(response)) refundCheck.value = response.data
  } finally {
    refundLoading.value = false
  }
}

async function refund() {
  if (!current.value || !refundCheck.value?.refundable || !noDefault.value) return
  saving.value = true
  try {
    const response = await refundServiceContract({
      contractId: current.value.id,
      noDefault: true,
      noDebt: refundCheck.value.noDebt,
      noRemainingGoods: refundCheck.value.noRemainingGoods,
      remark: refundRemark.value.trim()
    })
    if (isSuccess(response)) {
      message.success('退款流水已登记，合同已结算')
      refundOpen.value = false
      await load()
    }
  } finally {
    saving.value = false
  }
}

function resetCreate() {
  Object.assign(form, {
    contractNo: '',
    wmsTenantId: undefined,
    warehouseId: undefined,
    rackUnitCount: DEFAULT_RACK_COUNT,
    monthlyRentPerUnit: 20000,
    warehouseDeposit: FIXED_WAREHOUSE_DEPOSIT,
    subscriptionTotal: DEFAULT_RACK_COUNT * SUBSCRIPTION_PER_RACK,
    contractFileId: undefined,
    remark: ''
  })
  refundablePercent.value = 70
  dateRange.value = undefined
  rackNos.value = []
  rackOptions.value = []
}

function statusText(record: ServiceContract) {
  if (record.contractStatus === 'TERMINATED' || record.paymentStatus === 'CANCELLED') return '已取消'
  if (record.paymentStatus === 'PENDING') return '待收款'
  if (record.paymentStatus === 'REFUNDED') return '已结算'
  return record.contractStatus === 'ACTIVE' ? '履约中' : record.contractStatus
}
function statusColor(record: ServiceContract) {
  if (record.contractStatus === 'TERMINATED' || record.paymentStatus === 'CANCELLED') return 'default'
  if (record.paymentStatus === 'PENDING') return 'orange'
  if (record.paymentStatus === 'REFUNDED') return 'default'
  return 'green'
}
function money(value: number | string) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
}
function percent(value: number | string) {
  return `${Number(value || 0) * 100}%`
}
function operatorName(id: number) {
  return operators.value.find(row => row.id === id)?.tenantName || `#${id}`
}
function warehouseName(id: number) {
  return warehouses.value.find(row => row.id === id)?.warehouseName || `#${id}`
}
function componentText(value: string) {
  return (
    {
      WAREHOUSE_DEPOSIT: '仓储押金',
      SUBSCRIPTION_REFUNDABLE: '可退认购款',
      SUBSCRIPTION_SERVICE: '不退服务费'
    }[value] || value
  )
}
function transactionText(value: string) {
  return (
    {
      RECEIPT: '收款',
      RECOGNITION: '收入确认',
      REFUND: '退款',
      FORFEIT: '扣留',
      ADJUSTMENT: '调整'
    }[value] || value
  )
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
.page {
  min-height: 100%;
  padding: 16px;
  background: #fff;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.subtitle {
  margin-top: 3px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.notice {
  margin-bottom: 16px;
}
.money-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #595959;
}
.field-tip {
  margin-top: 3px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  line-height: 18px;
}
.money-summary b,
.primary-action {
  color: #1677ff;
}
.danger-action {
  color: #ff4d4f;
}
.modal-textarea {
  margin-top: 16px;
}
.fund-table {
  margin-top: 16px;
}
.recognition-term {
  margin-bottom: 12px;
  color: rgba(0, 0, 0, 0.65);
}
.recognition-month-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(136px, 1fr));
  gap: 8px;
}
.recognition-month {
  display: flex;
  min-height: 58px;
  padding: 8px 10px;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 4px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
  color: rgba(0, 0, 0, 0.88);
  cursor: pointer;
}
.recognition-month:hover,
.recognition-month.selected {
  border-color: #1677ff;
  background: #e6f4ff;
}
.recognition-month.recognized {
  border-color: #b7eb8f;
  background: #f6ffed;
  color: #389e0d;
  cursor: default;
}
.recognition-month-status {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.recognition-month.recognized .recognition-month-status {
  color: #52c41a;
}
.recognition-alert {
  margin-top: 16px;
}
.manual-check {
  display: block;
  margin: 16px 0;
}
.ok {
  margin-right: 6px;
  color: #52c41a;
}
.bad {
  margin-right: 6px;
  color: #ff4d4f;
}
</style>
