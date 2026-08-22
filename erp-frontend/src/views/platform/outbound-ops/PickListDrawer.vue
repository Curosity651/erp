<template>
  <a-drawer
    :open="open"
    :title="pickList?.taskNo ? `拣货任务 ${pickList.taskNo}` : '拣货任务'"
    width="900"
    @close="handleClose"
  >
    <template #extra>
      <a-space>
        <a-button :disabled="!pickList" @click="handlePrintLabels">
          <qrcode-outlined />
          打印作业标签
        </a-button>
        <a-button type="primary" :disabled="!pickList" @click="handlePrint">
          <printer-outlined />
          打印拣货单
        </a-button>
      </a-space>
    </template>
    <template #footer>
      <div class="drawer-footer">
        <a-button @click="handleClose">关闭</a-button>
        <a-button
          v-if="pickList?.taskStatus === 'PICKING'"
          type="primary"
          :loading="completing"
          :disabled="!pickList?.completable || !pickList?.taskId"
          @click="handleComplete"
        >
          确认拣货完成
        </a-button>
      </div>
    </template>
    <a-spin :spinning="loading">
      <template v-if="pickList">
        <a-descriptions :column="2" size="small" bordered style="margin-bottom: 16px">
          <a-descriptions-item label="任务单号">{{ pickList.taskNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="任务类型">
            {{
              pickList.taskType === 'WAVE'
                ? '批量波次'
                : pickList.taskType === 'PALLET_DIRECT'
                  ? '整托直发'
                  : '按单拣货'
            }}
          </a-descriptions-item>
          <a-descriptions-item label="货主">{{ pickList.ownerName }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ pickList.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="拣货员">{{ pickList.pickerName }}</a-descriptions-item>
          <a-descriptions-item label="任务范围">
            {{ pickList.outboundOrderCount || 1 }} 张出库单
            <template v-if="pickList.sourceType === 'SALES'">
              · {{ pickList.salesOrderCount || 0 }} 个销售订单
            </template>
          </a-descriptions-item>
          <a-descriptions-item label="拣货进度" :span="2">
            <a-progress
              :percent="pickPercent"
              :status="pickList.taskStatus === 'EXCEPTION' ? 'exception' : 'active'"
              size="small"
            />
            <span class="progress-copy">
              {{ pickList.pickedQuantity || 0 }} / {{ pickList.plannedQuantity || totalTake }} 件
            </span>
          </a-descriptions-item>
        </a-descriptions>

        <a-alert
          v-if="pickList.taskStatus === 'EXCEPTION'"
          type="error"
          show-icon
          :message="pickList.exceptionReason || '拣货任务存在待处理异常'"
          style="margin-bottom: 16px"
        >
          <template #action>
            <a-button size="small" danger @click="openResolve">主管处理</a-button>
          </template>
        </a-alert>
        <a-alert
          v-if="pickList.taskStatus === 'RETURNING'"
          type="warning"
          show-icon
          message="短拣任务正在返库。请把已拣商品扫回原托位，全部返库后系统才会释放库存占用。"
          style="margin-bottom: 16px"
        />

        <div class="section-title">
          关联出库单
          <span class="hint">（同一任务号表示同一次批量处理）</span>
        </div>
        <a-table
          :data-source="pickList.outboundOrders || []"
          :pagination="false"
          row-key="outboundOrderId"
          size="small"
          class="outbound-table"
        >
          <a-table-column title="出库单号" data-index="outboundNo" :width="200" />
          <a-table-column
            v-if="pickList.sourceType === 'SALES'"
            title="销售订单"
            data-index="salesOrderCount"
            :width="90"
            align="right"
          />
          <a-table-column title="SKU" data-index="skuCount" :width="70" align="right" />
          <a-table-column title="件数" data-index="totalQuantity" :width="70" align="right" />
          <a-table-column title="格口范围" :width="120">
            <template #default="{ record }">
              <a-tag v-if="record.sortRequired" color="orange">{{ record.toteNo }}</a-tag>
              <span v-else>—</span>
            </template>
          </a-table-column>
        </a-table>

        <a-alert
          v-if="(pickList.secondaryOrderCount || 0) > 0"
          type="info"
          show-icon
          :message="`本任务包含 ${pickList.secondaryOrderCount} 个最终平台订单包裹；拣货完成后直接按格口分货。`"
          style="margin: 16px 0"
        />

        <template v-if="pickList.taskStatus === 'SORTING'">
          <a-alert
            type="info"
            show-icon
            message="分货格是仓库固定物理位置，本任务只临时占用。如果尚未开始扫描，可释放格口并直接到打包签出逐单复核。"
            style="margin-bottom: 16px"
          >
            <template #action>
              <a-button size="small" :loading="skippingSort" @click="handleSkipSorting">
                跳过格口，直接打包
              </a-button>
            </template>
          </a-alert>
          <div class="section-title">
            平台订单分货
            <span class="hint">（每个格口直接对应一个最终平台订单包裹）</span>
          </div>
          <a-input-search
            v-model:value="sortSlotScan"
            placeholder="扫描分货格标签，打开对应平台订单"
            enter-button="打开格口"
            style="margin-bottom: 12px"
            @search="openSortSlot"
          />
          <a-table
            :data-source="sortPackages"
            :pagination="false"
            row-key="id"
            size="small"
            style="margin-bottom: 16px"
          >
            <a-table-column title="出库单" data-index="outboundNo" :width="170" />
            <a-table-column title="格口" data-index="sortCode" :width="90">
              <template #default="{ record }"
                ><a-tag color="blue">{{ record.sortCode }}</a-tag></template
              >
            </a-table-column>
            <a-table-column title="平台订单" data-index="platformOrderId" :width="190" />
            <a-table-column title="商品" :width="240">
              <template #default="{ record }">
                <div v-for="item in record.items" :key="item.skuCode">
                  {{ item.warehouseSkuCode || item.skuCode }}：{{ item.sortedQty || 0 }} /
                  {{ item.qty }}
                </div>
              </template>
            </a-table-column>
            <a-table-column title="操作" :width="160" align="center">
              <template #default="{ record }">
                <a-tag v-if="record.sortStatus !== 'PENDING'" color="green">已分货</a-tag>
                <a-space v-else size="small">
                  <a-button type="link" size="small" @click="openSortScan(record)">扫码</a-button>
                  <a-button
                    type="link"
                    size="small"
                    :loading="sortingId === record.id"
                    :disabled="!packageSortComplete(record)"
                    @click="handleSort(record.id)"
                    >完成</a-button
                  >
                </a-space>
              </template>
            </a-table-column>
          </a-table>
        </template>

        <div class="section-title">
          取货清单
          <span class="hint">（按库位排序，依次取货）</span>
        </div>
        <a-table
          :data-source="pickList.allocations"
          :pagination="false"
          :row-key="allocationRowKey"
          size="small"
        >
          <a-table-column title="序" :width="48" align="center">
            <template #default="{ index }">{{ index + 1 }}</template>
          </a-table-column>
          <a-table-column title="库位" data-index="locationCode" :width="110">
            <template #default="{ record }">
              <a-tag color="blue">{{ record.slotCode || record.locationCode }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="托盘/层位" :width="120">
            <template #default="{ record }">
              <div>{{ record.palletNo || '-' }}</div>
              <div class="sku-name">{{ record.slotCode || '-' }}</div>
            </template>
          </a-table-column>
          <a-table-column title="SKU" :width="150">
            <template #default="{ record }">
              <div>{{ record.warehouseSkuCode || record.skuCode }}</div>
              <div class="sku-name">原SKU：{{ record.skuCode }}</div>
            </template>
          </a-table-column>
          <a-table-column title="批次" data-index="batchNo" :width="130" />
          <a-table-column title="策略" :width="88">
            <template #default="{ record }">
              <a-tag :color="record.pickStrategy === 'WHOLE_PALLET' ? 'green' : 'default'">
                {{ record.pickStrategy === 'WHOLE_PALLET' ? '整托' : '拆零' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="计划/已拣" :width="94" align="right">
            <template #default="{ record }">
              {{ record.plannedQty ?? record.takeQty }} / {{ record.pickedQty || 0 }}
            </template>
          </a-table-column>
          <a-table-column title="状态" :width="92" align="center">
            <template #default="{ record }">
              <a-tag v-if="record.lineStatus === 'COMPLETED'" color="green">已完成</a-tag>
              <a-tag v-else-if="record.lineStatus === 'EXCEPTION'" color="red">异常</a-tag>
              <a-tag v-else-if="record.lineStatus === 'IN_PROGRESS'" color="blue">进行中</a-tag>
              <a-tag v-else-if="record.lineStatus === 'RETURNING'" color="orange">
                返库 {{ record.returnedQty || 0 }}/{{ record.returnRequiredQty || 0 }}
              </a-tag>
              <a-tag v-else-if="record.lineStatus === 'RETURNED'" color="green">已返库</a-tag>
              <a-tag v-else>待拣</a-tag>
            </template>
          </a-table-column>
          <a-table-column
            v-if="pickList.taskStatus === 'PICKING'"
            title="操作"
            :width="130"
            align="center"
          >
            <template #default="{ record }">
              <a-space size="small">
                <a-button
                  type="link"
                  size="small"
                  :disabled="(record.remainingQty ?? record.takeQty) <= 0"
                  @click="openPickScan(record)"
                  >登记</a-button
                >
                <a-button
                  type="link"
                  danger
                  size="small"
                  :disabled="(record.remainingQty ?? record.takeQty) <= 0"
                  @click="openException(record)"
                  >异常</a-button
                >
              </a-space>
            </template>
          </a-table-column>
          <a-table-column
            v-if="pickList.taskStatus === 'RETURNING'"
            title="操作"
            :width="100"
            align="center"
          >
            <template #default="{ record }">
              <a-button
                type="link"
                size="small"
                :disabled="(record.returnedQty || 0) >= (record.returnRequiredQty || 0)"
                @click="openReturnScan(record)"
              >
                返库扫码
              </a-button>
            </template>
          </a-table-column>
        </a-table>

        <div class="total-bar">
          共 {{ pickList.allocations.length }} 个库位点 · 合计取货 {{ totalTake }} 件
        </div>
      </template>
      <a-empty v-else-if="!loading" description="暂无拣货单" />
    </a-spin>

    <a-modal
      v-model:open="scanOpen"
      title="登记实拣数量"
      :confirm-loading="scanSubmitting"
      @ok="submitPickScan"
    >
      <a-form layout="vertical">
        <a-form-item label="任务明细">
          <a-input
            :value="`${scanLine?.locationCode || '-'} · ${scanLine?.skuCode || '-'}`"
            disabled
          />
        </a-form-item>
        <a-form-item v-if="canManual" label="登记方式">
          <a-segmented v-model:value="scanForm.manual" :options="scanModeOptions" block />
        </a-form-item>
        <a-form-item v-if="scanForm.manual" label="手工登记原因" required>
          <a-textarea v-model:value="scanForm.manualReason" :rows="2" />
        </a-form-item>
        <a-form-item v-if="!scanForm.manual" label="库位标签" required>
          <a-input
            v-model:value="scanForm.locationScanCode"
            autofocus
            placeholder="先扫描库位标签"
          />
        </a-form-item>
        <a-form-item :label="scanForm.manual ? 'ERP SKU' : '商品/托盘条码'" required>
          <a-input
            v-model:value="scanForm.scanCode"
            placeholder="扫描商品条码、托盘号，或输入ERP SKU"
          />
        </a-form-item>
        <a-form-item label="本次数量" required>
          <a-input-number
            v-model:value="scanForm.quantity"
            :min="1"
            :max="scanLine?.remainingQty || scanLine?.takeQty || 1"
            :disabled="scanLine?.pickStrategy === 'WHOLE_PALLET'"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="returnOpen"
      title="已拣商品返库"
      :confirm-loading="returnSubmitting"
      @ok="submitReturnScan"
    >
      <a-form layout="vertical">
        <a-form-item label="返库托位">
          <a-input :value="returnLine?.slotCode || returnLine?.locationCode" disabled />
        </a-form-item>
        <a-form-item label="托位标签" required>
          <a-input v-model:value="returnForm.locationScanCode" autofocus />
        </a-form-item>
        <a-form-item
          :label="returnLine?.pickStrategy === 'WHOLE_PALLET' ? '托盘码' : '内部SKU'"
          required
        >
          <a-input v-model:value="returnForm.scanCode" />
        </a-form-item>
        <a-form-item label="返库数量" required>
          <a-input-number
            v-model:value="returnForm.quantity"
            :min="1"
            :max="returnRemaining"
            :disabled="returnLine?.pickStrategy === 'WHOLE_PALLET'"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="exceptionOpen"
      title="报告拣货异常"
      :confirm-loading="exceptionSubmitting"
      @ok="submitException"
    >
      <a-alert type="warning" show-icon message="提交后任务将暂停，必须由主管处理后才能继续。" />
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="缺货数量" required>
          <a-input-number
            v-model:value="exceptionForm.shortageQty"
            :min="1"
            :max="exceptionLine?.remainingQty || exceptionLine?.takeQty || 1"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="异常原因" required>
          <a-textarea
            v-model:value="exceptionForm.reason"
            :rows="3"
            placeholder="例如：库位实物不足、包装破损"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="resolveOpen"
      title="处理拣货异常"
      :confirm-loading="resolveSubmitting"
      @ok="submitResolve"
    >
      <a-form layout="vertical">
        <a-form-item label="处理方式">
          <a-radio-group v-model:value="resolveForm.action">
            <a-radio value="RETRY">原批次继续拣货</a-radio>
            <a-radio value="REALLOCATE">改用推荐批次</a-radio>
            <a-radio value="SHORT_CLOSE">按缺货关闭任务</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="resolveForm.action === 'REALLOCATE'" label="替代批次" required>
          <a-select
            v-model:value="resolveForm.replacementInventoryId"
            :loading="alternativesLoading"
            :options="alternativeOptions"
            placeholder="请选择同SKU可用批次"
          />
        </a-form-item>
        <a-form-item label="处理说明">
          <a-textarea v-model:value="resolveForm.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="sortScanOpen"
      :title="`格口分货 ${sortPackage?.sortCode || ''}`"
      :width="720"
      :confirm-loading="sortScanSubmitting"
      @ok="submitSortScan"
    >
      <div class="sort-package-meta">
        <span>请按照内部 SKU 找到商品，扫描核对后放入当前格口</span>
        <span class="sort-order-reference">订单：{{ sortPackage?.platformOrderId || '-' }}</span>
      </div>

      <div class="sort-items-title">待分货商品</div>
      <a-table
        :data-source="sortItemRows"
        :pagination="false"
        row-key="skuCode"
        size="small"
        class="sort-items-table"
      >
        <a-table-column title="内部 SKU" :width="220">
          <template #default="{ record }">
            <div class="sort-primary-sku">{{ record.warehouseSkuCode }}</div>
            <div v-if="record.skuName" class="sku-name">{{ record.skuName }}</div>
          </template>
        </a-table-column>
        <a-table-column title="原始 SKU" data-index="skuCode" :width="160" />
        <a-table-column title="应放" data-index="requiredQty" :width="70" align="right" />
        <a-table-column title="已放" data-index="sortedQty" :width="70" align="right" />
        <a-table-column title="剩余" :width="80" align="right">
          <template #default="{ record }">
            <a-tag :color="record.remainingQty > 0 ? 'orange' : 'green'">
              {{ record.remainingQty }}
            </a-tag>
          </template>
        </a-table-column>
      </a-table>

      <a-form layout="vertical">
        <a-form-item label="扫描商品内部 SKU 或条码" required>
          <a-input
            v-model:value="sortScanForm.scanCode"
            autofocus
            placeholder="扫描贴在商品上的内部 SKU 标签"
          />
        </a-form-item>
        <a-form-item label="本次数量" required>
          <a-input-number
            v-model:value="sortScanForm.quantity"
            :min="1"
            :max="Math.max(1, sortRemainingTotal)"
            style="width: 100%"
          />
        </a-form-item>
        <a-checkbox v-if="canManual" v-model:checked="sortScanForm.manual">
          手工输入内部SKU
        </a-checkbox>
        <a-form-item v-if="sortScanForm.manual" label="手工登记原因" required>
          <a-textarea v-model:value="sortScanForm.manualReason" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PrinterOutlined, QrcodeOutlined } from '@ant-design/icons-vue'
import { isSuccess } from '@/api'
import {
  completePickTask,
  confirmPackageSort,
  getPickList,
  listPickAlternatives,
  reportPickException,
  resolvePickException,
  scanPackageSort,
  scanPickLine,
  scanPickReturn,
  skipPackageSorting
} from '@/api/wms/outbound-picking'
import { useAuthorize } from '@/hooks/permission'
import type {
  OutboundPackageVO,
  PickAllocationVO,
  PickListVO
} from '@/api/wms/outbound-picking/types'
import QRCode from 'qrcode'
import { buildSortItemRows } from './sort-package-view'

const props = defineProps<{ open: boolean; orderId?: number }>()
const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const { hasPermission } = useAuthorize()
const canManual = computed(() => hasPermission('wms:outbound-exec:supervise'))
const completing = ref(false)
const sortingId = ref<number>()
const skippingSort = ref(false)
const pickList = ref<PickListVO | null>(null)
const scanOpen = ref(false)
const scanSubmitting = ref(false)
const scanLine = ref<PickAllocationVO>()
const scanForm = ref({
  manual: false,
  manualReason: '',
  locationScanCode: '',
  scanCode: '',
  quantity: 1
})
const returnOpen = ref(false)
const returnSubmitting = ref(false)
const returnLine = ref<PickAllocationVO>()
const returnForm = ref({ locationScanCode: '', scanCode: '', quantity: 1 })
const exceptionOpen = ref(false)
const exceptionSubmitting = ref(false)
const exceptionLine = ref<PickAllocationVO>()
const exceptionForm = ref({ shortageQty: 1, reason: '' })
const resolveOpen = ref(false)
const resolveSubmitting = ref(false)
const alternativesLoading = ref(false)
const alternatives = ref<PickAllocationVO[]>([])
const resolveForm = ref<{
  action: 'RETRY' | 'REALLOCATE' | 'SHORT_CLOSE'
  replacementInventoryId?: number
  remark?: string
}>({ action: 'RETRY' })
const sortScanOpen = ref(false)
const sortScanSubmitting = ref(false)
const sortPackage = ref<OutboundPackageVO & { outboundNo: string }>()
const sortScanForm = ref({ scanCode: '', quantity: 1, manual: false, manualReason: '' })
const sortSlotScan = ref('')
const scanModeOptions = [
  { label: '扫码', value: false },
  { label: '手工登记', value: true }
]

const totalTake = computed(() =>
  (pickList.value?.allocations || []).reduce((s, a) => s + a.takeQty, 0)
)
const pickPercent = computed(() => {
  const planned = pickList.value?.plannedQuantity || totalTake.value
  if (!planned) return 0
  return Math.min(100, Math.round(((pickList.value?.pickedQuantity || 0) / planned) * 100))
})
const returnRemaining = computed(() =>
  Math.max(1, (returnLine.value?.returnRequiredQty || 0) - (returnLine.value?.returnedQty || 0))
)

const sortPackages = computed(() =>
  (pickList.value?.outboundOrders || []).flatMap(order =>
    (order.packages || [])
      .filter(pack => pack.sortStatus !== 'NOT_REQUIRED')
      .map(pack => ({ ...pack, outboundNo: order.outboundNo }))
  )
)
const sortItemRows = computed(() => buildSortItemRows(sortPackage.value?.items || []))
const sortRemainingTotal = computed(() =>
  sortItemRows.value.reduce((total, item) => total + item.remainingQty, 0)
)

const allocationRowKey = (record: PickListVO['allocations'][number], index?: number) =>
  `${record.locationCode}-${record.skuCode}-${record.batchNo}-${record.palletId || 0}-${index || 0}`

const alternativeOptions = computed(() =>
  alternatives.value.map(item => ({
    value: item.physicalInventoryId,
    label: `${item.locationCode} · ${item.palletNo || item.batchNo} · 可用 ${item.takeQty}`
  }))
)

async function load(id: number) {
  loading.value = true
  pickList.value = null
  try {
    const res = await getPickList(id)
    if (isSuccess(res) && res.data) pickList.value = res.data
  } finally {
    loading.value = false
  }
}

function handleComplete() {
  const taskId = pickList.value?.taskId
  if (!taskId || !pickList.value?.completable) return
  const needsSorting = (pickList.value?.secondaryOrderCount || 0) > 0
  Modal.confirm({
    title: '确认拣货完成？',
    content: needsSorting
      ? '确认后，任务将进入平台订单分货。请先核对实际取货数量。'
      : '确认后，任务内出库单将直接进入待打包状态。请先核对实际取货数量。',
    okText: '确认完成',
    cancelText: '取消',
    async onOk() {
      completing.value = true
      try {
        const res = await completePickTask(taskId)
        if (isSuccess(res)) {
          message.success(
            needsSorting ? '实物拣货已确认，请继续完成平台订单分货' : '实物拣货已确认，可继续打包'
          )
          emit('success')
          if (props.orderId) await load(props.orderId)
        }
      } finally {
        completing.value = false
      }
    }
  })
}

async function handleSort(packageId: number) {
  const taskId = pickList.value?.taskId
  if (!taskId) return
  sortingId.value = packageId
  try {
    const res = await confirmPackageSort(taskId, packageId)
    if (isSuccess(res)) {
      message.success('该平台订单已完成分货')
      emit('success')
      if (props.orderId) await load(props.orderId)
      if (pickList.value?.taskStatus === 'COMPLETED') handleClose()
    }
  } finally {
    sortingId.value = undefined
  }
}

function handleSkipSorting() {
  const taskId = pickList.value?.taskId
  if (!taskId) return
  Modal.confirm({
    title: '跳过格口分货？',
    content: '仅在尚未开始格口扫码时可执行。确认后将直接进入按平台订单复核打包。',
    okText: '直接进入打包',
    cancelText: '取消',
    async onOk() {
      skippingSort.value = true
      try {
        const res = await skipPackageSorting(taskId)
        if (isSuccess(res)) {
          message.success('已跳过格口分货，可前往打包签出逐单复核')
          emit('success')
          if (props.orderId) await load(props.orderId)
        }
      } finally {
        skippingSort.value = false
      }
    }
  })
}

function openPickScan(line: PickAllocationVO) {
  scanLine.value = line
  scanForm.value = {
    manual: false,
    manualReason: '',
    locationScanCode: '',
    scanCode: '',
    quantity: line.pickStrategy === 'WHOLE_PALLET' ? line.remainingQty || line.takeQty : 1
  }
  scanOpen.value = true
}

function openReturnScan(line: PickAllocationVO) {
  returnLine.value = line
  returnForm.value = {
    locationScanCode: '',
    scanCode: '',
    quantity:
      line.pickStrategy === 'WHOLE_PALLET'
        ? Math.max(1, (line.returnRequiredQty || 0) - (line.returnedQty || 0))
        : 1
  }
  returnOpen.value = true
}

async function submitReturnScan() {
  const taskId = pickList.value?.taskId
  const lineId = returnLine.value?.lineId
  if (
    !taskId ||
    !lineId ||
    !returnForm.value.locationScanCode.trim() ||
    !returnForm.value.scanCode.trim()
  ) {
    message.warning('请扫描返库托位和商品或托盘码')
    return
  }
  returnSubmitting.value = true
  try {
    const res = await scanPickReturn({
      taskId,
      lineId,
      locationScanCode: returnForm.value.locationScanCode.trim(),
      scanCode: returnForm.value.scanCode.trim(),
      quantity: returnForm.value.quantity,
      manual: false
    })
    if (isSuccess(res)) {
      returnOpen.value = false
      message.success('返库数量已登记')
      emit('success')
      if (props.orderId) await load(props.orderId)
    }
  } finally {
    returnSubmitting.value = false
  }
}

async function submitPickScan() {
  const taskId = pickList.value?.taskId
  const lineId = scanLine.value?.lineId
  if (!taskId || !lineId || !scanForm.value.scanCode.trim()) {
    message.warning('请完成库位和商品信息')
    return
  }
  if (!scanForm.value.manual && !scanForm.value.locationScanCode.trim()) {
    message.warning('请先扫描库位标签')
    return
  }
  if (scanForm.value.manual && !scanForm.value.manualReason.trim()) {
    message.warning('请填写手工登记原因')
    return
  }
  scanSubmitting.value = true
  try {
    const res = await scanPickLine({
      taskId,
      lineId,
      scanCode: scanForm.value.scanCode.trim(),
      locationScanCode: scanForm.value.locationScanCode.trim(),
      quantity: scanForm.value.quantity,
      manual: scanForm.value.manual,
      manualReason: scanForm.value.manualReason.trim() || undefined
    })
    if (isSuccess(res)) {
      scanOpen.value = false
      message.success('实拣数量已登记')
      if (props.orderId) await load(props.orderId)
    }
  } finally {
    scanSubmitting.value = false
  }
}

function openException(line: PickAllocationVO) {
  exceptionLine.value = line
  exceptionForm.value = {
    shortageQty: line.remainingQty || line.takeQty,
    reason: ''
  }
  exceptionOpen.value = true
}

async function submitException() {
  const taskId = pickList.value?.taskId
  const lineId = exceptionLine.value?.lineId
  if (!taskId || !lineId || !exceptionForm.value.reason.trim()) {
    message.warning('请填写异常原因')
    return
  }
  exceptionSubmitting.value = true
  try {
    const res = await reportPickException({
      taskId,
      lineId,
      shortageQty: exceptionForm.value.shortageQty,
      reason: exceptionForm.value.reason.trim()
    })
    if (isSuccess(res)) {
      exceptionOpen.value = false
      message.warning('任务已暂停，请由主管处理异常')
      if (props.orderId) await load(props.orderId)
    }
  } finally {
    exceptionSubmitting.value = false
  }
}

async function openResolve() {
  const taskId = pickList.value?.taskId
  const line = pickList.value?.allocations.find(item => item.lineStatus === 'EXCEPTION')
  if (!taskId || !line?.lineId) return
  resolveForm.value = { action: 'RETRY' }
  alternatives.value = []
  resolveOpen.value = true
  alternativesLoading.value = true
  try {
    const res = await listPickAlternatives(taskId, line.lineId)
    if (isSuccess(res)) alternatives.value = res.data || []
  } finally {
    alternativesLoading.value = false
  }
}

async function submitResolve() {
  const taskId = pickList.value?.taskId
  if (!taskId) return
  if (resolveForm.value.action === 'REALLOCATE' && !resolveForm.value.replacementInventoryId) {
    message.warning('请选择替代批次')
    return
  }
  resolveSubmitting.value = true
  try {
    const res = await resolvePickException({ taskId, ...resolveForm.value })
    if (isSuccess(res)) {
      resolveOpen.value = false
      message.success(
        resolveForm.value.action === 'SHORT_CLOSE'
          ? '短拣处理已提交；如有已拣商品，请先完成返库'
          : '异常已处理，可以继续拣货'
      )
      emit('success')
      if (props.orderId) await load(props.orderId)
    }
  } finally {
    resolveSubmitting.value = false
  }
}

function openSortScan(pack: OutboundPackageVO & { outboundNo: string }) {
  sortPackage.value = pack
  sortScanForm.value = { scanCode: '', quantity: 1, manual: false, manualReason: '' }
  sortScanOpen.value = true
}

function openSortSlot() {
  const code = sortSlotScan.value.trim()
  if (!code) return
  const pack = sortPackages.value.find(
    item =>
      item.sortCode?.toLowerCase() === code.toLowerCase() ||
      item.sortSlotScanCode?.toLowerCase() === code.toLowerCase()
  )
  if (!pack) {
    message.error('该分货格不属于当前任务')
    return
  }
  sortSlotScan.value = ''
  openSortScan(pack)
}

async function submitSortScan() {
  const taskId = pickList.value?.taskId
  const pack = sortPackage.value
  if (!taskId || !pack || !sortScanForm.value.scanCode.trim()) return
  if (sortScanForm.value.manual && !sortScanForm.value.manualReason.trim()) {
    message.warning('请填写手工登记原因')
    return
  }
  sortScanSubmitting.value = true
  try {
    const res = await scanPackageSort(taskId, {
      outboundOrderId: pack.outboundOrderId,
      packageId: pack.id,
      scanCode: sortScanForm.value.scanCode.trim(),
      quantity: sortScanForm.value.quantity,
      manual: sortScanForm.value.manual,
      manualReason: sortScanForm.value.manualReason.trim() || undefined
    })
    if (isSuccess(res)) {
      message.success('分货数量已登记')
      const packageId = pack.id
      if (props.orderId) await load(props.orderId)
      const updated = sortPackages.value.find(item => item.id === packageId)
      if (updated && !packageSortComplete(updated)) {
        sortPackage.value = updated
        sortScanForm.value = {
          scanCode: '',
          quantity: 1,
          manual: false,
          manualReason: ''
        }
      } else {
        sortScanOpen.value = false
        message.success('当前格口商品已全部登记，可以确认完成')
      }
    }
  } finally {
    sortScanSubmitting.value = false
  }
}

function packageSortComplete(pack: OutboundPackageVO) {
  return pack.items.every(item => (item.sortedQty || 0) === item.qty)
}

watch(
  () => [props.open, props.orderId] as const,
  ([open, id]) => {
    if (open && id) load(id)
  },
  { immediate: true }
)

function esc(v: unknown): string {
  return String(v ?? '').replace(
    /[&<>"]/g,
    c => (({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }) as Record<string, string>)[c]
  )
}

async function handlePrint() {
  const pl = pickList.value
  if (!pl) return
  const taskQr = await QRCode.toDataURL(pl.taskNo || pl.outboundNo, { margin: 1, width: 128 })
  const now = new Date().toLocaleString('zh-CN')
  const rows = (pl.allocations || [])
    .map(
      (a, i) => `<tr>
        <td class="c">${i + 1}</td>
        <td class="loc">${esc(a.slotCode || a.locationCode)}</td>
        <td>${esc(a.warehouseSkuCode || a.skuCode)}<div class="muted">原SKU：${esc(a.skuCode)}</div></td>
        <td>${esc(a.batchNo)}</td>
        <td class="r">${esc(a.takeQty)}</td>
      </tr>`
    )
    .join('')
  const outboundRows = (pl.outboundOrders || [])
    .map(
      o => `<tr>
        <td>${esc(o.outboundNo)}</td>
        ${pl.sourceType === 'SALES' ? `<td class="r">${esc(o.salesOrderCount)}</td>` : ''}
        <td class="r">${esc(o.skuCount)}</td>
        <td class="r">${esc(o.totalQuantity)}</td>
        <td>${esc(o.toteNo || '-')}</td>
      </tr>`
    )
    .join('')
  const taskScope = `${pl.outboundOrderCount || 1} 张出库单${
    pl.sourceType === 'SALES' ? ` · ${pl.salesOrderCount || 0} 个销售订单` : ''
  }`
  const html = `<!doctype html><html lang="zh"><head><meta charset="utf-8"><title>拣货任务-${esc(pl.taskNo || pl.outboundNo)}</title>
<style>
  * { box-sizing: border-box; }
  body { font-family: -apple-system, "Microsoft YaHei", sans-serif; color: #000; margin: 24px; }
  h1 { font-size: 20px; text-align: center; margin: 0 0 4px; }
  .sub { text-align: center; color: #666; font-size: 12px; margin-bottom: 16px; }
  .meta { width: 100%; border-collapse: collapse; margin-bottom: 12px; font-size: 13px; }
  .meta td { padding: 4px 8px; border: 1px solid #999; }
  .meta .k { background: #f2f2f2; width: 90px; font-weight: 600; }
  table.items { width: 100%; border-collapse: collapse; font-size: 13px; }
  table.items th, table.items td { border: 1px solid #999; padding: 6px 8px; }
  table.items th { background: #f2f2f2; }
  .section { margin: 12px 0 5px; font-size: 13px; font-weight: 600; }
  .c { text-align: center; width: 36px; }
  .r { text-align: right; width: 64px; }
  .loc { font-weight: 600; white-space: nowrap; }
  .muted { color: #666; font-size: 11px; }
  .total { margin-top: 10px; font-size: 13px; text-align: right; }
  .sign { margin-top: 36px; display: flex; justify-content: space-between; font-size: 13px; }
  .task-qr { position: absolute; right: 24px; top: 20px; width: 76px; height: 76px; }
  @media print { body { margin: 12mm; } button { display: none; } }
</style></head><body>
  <img class="task-qr" src="${taskQr}" alt="任务二维码">
  <h1>拣货任务单</h1>
  <div class="sub">打印时间：${esc(now)}</div>
  <table class="meta">
  <tr><td class="k">任务单号</td><td>${esc(pl.taskNo || '-')}</td><td class="k">任务类型</td><td>${esc(pl.taskType === 'WAVE' ? '批量波次' : pl.taskType === 'PALLET_DIRECT' ? '整托直发' : '按单拣货')}</td></tr>
    <tr><td class="k">货主</td><td>${esc(pl.ownerName)}</td><td class="k">仓库</td><td>${esc(pl.warehouseName)}</td></tr>
    <tr><td class="k">拣货员</td><td>${esc(pl.pickerName)}</td><td class="k">任务范围</td><td>${esc(taskScope)}</td></tr>
  </table>
  <div class="section">关联出库单</div>
  <table class="items">
    <thead><tr><th>出库单号</th>${pl.sourceType === 'SALES' ? '<th>销售订单</th>' : ''}<th>SKU</th><th>件数</th><th>格口范围</th></tr></thead>
    <tbody>${outboundRows}</tbody>
  </table>
  <div class="section">取货清单</div>
  <table class="items">
    <thead><tr><th class="c">序</th><th>库位</th><th>SKU</th><th>批次</th><th class="r">取货数</th></tr></thead>
    <tbody>${rows}</tbody>
  </table>
  <div class="total">共 ${(pl.allocations || []).length} 个库位点 · 合计取货 ${totalTake.value} 件</div>
  <div class="sign"><span>拣货员签字：____________</span><span>复核签字：____________</span></div>
</body></html>`
  const w = window.open('', '_blank', 'width=900,height=1000')
  if (!w) {
    message.warning('打印窗口被浏览器拦截，请允许弹出窗口后重试')
    return
  }
  w.document.write(html)
  w.document.close()
  w.focus()
  w.onload = () => {
    w.print()
  }
}

async function handlePrintLabels() {
  const pl = pickList.value
  if (!pl) return
  const labels: Array<{
    type: string
    code: string
    scanCode?: string
    title: string
    sub?: string
  }> = []
  if (pl.taskNo)
    labels.push({ type: 'PICK_TASK', code: pl.taskNo, title: '拣货任务', sub: pl.ownerName })
  const seen = new Set<string>()
  for (const line of pl.allocations || []) {
    if (line.locationCode && !seen.has(`LOCATION:${line.locationCode}`)) {
      seen.add(`LOCATION:${line.locationCode}`)
      labels.push({
        type: 'LOCATION',
        code: line.locationCode,
        title: '库位',
        sub: pl.warehouseName
      })
    }
    if (line.palletNo && !seen.has(`PALLET:${line.palletNo}`)) {
      seen.add(`PALLET:${line.palletNo}`)
      labels.push({ type: 'PALLET', code: line.palletNo, title: '托盘', sub: line.slotCode })
    }
  }
  for (const pack of sortPackages.value) {
    const slotIdentity = pack.sortSlotId || pack.sortSlotScanCode || pack.sortCode
    if (pack.sortCode && slotIdentity && !seen.has(`SORT_SLOT:${slotIdentity}`)) {
      seen.add(`SORT_SLOT:${slotIdentity}`)
      labels.push({
        type: 'SORT_SLOT',
        code: pack.sortCode,
        scanCode: pack.sortSlotScanCode || pack.sortCode,
        title: '固定分货格',
        sub: pl.warehouseName
      })
    }
  }
  const rendered = await Promise.all(
    labels.map(async item => ({
      ...item,
      qr: await QRCode.toDataURL(item.scanCode || item.code, { margin: 1, width: 220 })
    }))
  )
  const cards = rendered
    .map(
      item => `<section class="label">
        <img src="${item.qr}" alt="${esc(item.code)}">
        <div class="kind">${esc(item.title)}</div>
        <div class="code">${esc(item.code)}</div>
        <div class="sub">${esc(item.sub || '')}</div>
      </section>`
    )
    .join('')
  const html = `<!doctype html><html lang="zh"><head><meta charset="utf-8"><title>作业标签-${esc(pl.taskNo)}</title>
<style>
  @page { size: 80mm 50mm; margin: 0; }
  * { box-sizing: border-box; }
  body { margin: 0; font-family: "Microsoft YaHei", sans-serif; }
  .label { width: 80mm; height: 50mm; padding: 4mm; page-break-after: always; display: grid;
    grid-template-columns: 32mm 1fr; grid-template-rows: auto auto 1fr; column-gap: 4mm; align-items: center; }
  img { width: 30mm; height: 30mm; grid-row: 1 / 4; }
  .kind { font-size: 10pt; color: #555; }
  .code { font-size: 19pt; font-weight: 700; overflow-wrap: anywhere; }
  .sub { font-size: 9pt; color: #555; align-self: start; overflow-wrap: anywhere; }
</style></head><body>${cards}</body></html>`
  const w = window.open('', '_blank', 'width=700,height=800')
  if (!w) {
    message.warning('打印窗口被浏览器拦截，请允许弹出窗口后重试')
    return
  }
  w.document.write(html)
  w.document.close()
  w.focus()
  w.onload = () => w.print()
}

function handleClose() {
  emit('update:open', false)
}
</script>

<style scoped>
.section-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}
.section-title .hint {
  font-size: 12px;
  font-weight: 400;
  color: #8c8c8c;
}
.sku-name {
  font-size: 12px;
  color: #8c8c8c;
}
.sort-package-meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  margin-bottom: 16px;
  color: #595959;
  background: #f5f7fa;
  border-left: 3px solid #1677ff;
}
.sort-order-reference {
  flex: 0 0 auto;
  color: #8c8c8c;
  font-size: 12px;
}
.sort-items-title {
  margin-bottom: 8px;
  font-weight: 600;
}
.sort-items-table {
  margin-bottom: 16px;
}
.sort-primary-sku {
  font-size: 15px;
  font-weight: 600;
  color: #1677ff;
  word-break: break-all;
}
.outbound-table {
  margin-bottom: 16px;
}
.total-bar {
  margin-top: 12px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  text-align: right;
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.progress-copy {
  margin-left: 10px;
  color: #595959;
  white-space: nowrap;
}
</style>
