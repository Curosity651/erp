<template>
  <a-modal
    :open="open"
    title="财务报表同步"
    width="600px"
    :confirm-loading="loading"
    :ok-button-props="{ disabled: syncSuccess }"
    @ok="handleSync"
    @cancel="handleClose"
  >
    <a-form :model="formModel" layout="vertical">
      <!-- 周期类型选择（必填） -->
      <a-form-item label="报表周期" required>
        <a-radio-group v-model:value="formModel.periodType" button-style="solid">
          <a-radio-button value="weekly"> <calendar-outlined /> 周报 Weekly </a-radio-button>
          <a-radio-button value="daily"> <schedule-outlined /> 日报 Daily </a-radio-button>
        </a-radio-group>
        <div class="form-tip">
          <info-circle-outlined /> 周报数据按周汇总，日报数据按日汇总，请根据业务需要选择
        </div>
      </a-form-item>

      <!-- 日期范围选择（统一入口） -->
      <a-form-item label="日期范围">
        <a-range-picker
          v-model:value="dateRange"
          format="YYYY-MM-DD"
          style="width: 100%"
          :disabled-date="disabledDate"
          placeholder="['开始日期', '结束日期']"
        />
        <div class="form-tip">不选择日期时，将默认同步最近一年的数据</div>
      </a-form-item>

      <!-- 店铺选择（可选，多选，跨页保持） -->
      <a-form-item label="选择店铺（可选）">
        <shop-select-input
          v-model="formModel.shopIds"
          :multiple="true"
          placeholder="不选择则同步所有启用的店铺"
          @shop-selected="handleShopSelected"
        />
        <div class="form-tip">
          可以选择特定店铺进行同步，不选择则同步所有启用的 Wildberries 店铺
        </div>
      </a-form-item>
    </a-form>

    <!-- 同步结果展示 -->
    <a-alert
      v-if="syncResult"
      :message="syncResult.success ? '同步任务创建成功' : '同步任务创建失败'"
      :description="syncResult.message"
      :type="syncResult.success ? 'success' : 'error'"
      show-icon
      style="margin-top: 16px"
    />
  </a-modal>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { syncWbReport } from '@/api/financial/wb-report'
import type { FullSyncRequest, SyncTaskResponse, PeriodType } from '@/api/financial/wb-report/types'
import ShopSelectInput from '@/components/ShopSelectInput.vue'
import type { ShopVO } from '@/api/shop/types'
import { CalendarOutlined, ScheduleOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  open: boolean
}>()

const emits = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success', response: SyncTaskResponse): void
}>()

const loading = ref(false)
const syncSuccess = ref(false) // 同步成功标记，用于禁用确认按钮防重复提交
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const formModel = reactive({
  periodType: 'weekly' as PeriodType, // 默认周报
  shopIds: [] as number[]
})

const syncResult = ref<{
  success: boolean
  message: string
} | null>(null)

const handleShopSelected = (shops: ShopVO | ShopVO[]) => {
  // 事件主要用于后续如需联动展示信息，这里暂不做额外处理
  // 选中的 ID 已通过 v-model=formModel.shopIds 同步
}

/**
 * 禁用未来日期
 */
const disabledDate = (current: Dayjs) => {
  return current && current > dayjs().endOf('day')
}

/**
 * 验证表单
 */
const validateForm = (): boolean => {
  if (!formModel.periodType) {
    message.warning('请选择报表周期类型')
    return false
  }
  return true
}

/**
 * 执行同步
 */
const handleSync = async () => {
  // 防止重复提交
  if (syncSuccess.value) {
    return
  }

  if (!validateForm()) {
    return
  }

  loading.value = true
  syncResult.value = null

  try {
    const request: FullSyncRequest = {
      periodType: formModel.periodType, // 新增周期类型
      dateFrom:
        dateRange.value && dateRange.value[0] ? dateRange.value[0].format('YYYY-MM-DD') : undefined,
      dateTo:
        dateRange.value && dateRange.value[1] ? dateRange.value[1].format('YYYY-MM-DD') : undefined,
      shopIds: formModel.shopIds.length > 0 ? formModel.shopIds : undefined
    }

    const result = await syncWbReport(request)
    const response = result.data

    // 标记同步成功，禁用确认按钮防止重复提交
    syncSuccess.value = true

    // 显示成功结果（包含周期类型）
    const periodLabel = formModel.periodType === 'weekly' ? '周报' : '日报'
    syncResult.value = {
      success: true,
      message: `${periodLabel}同步任务已创建！作业ID: ${response.jobId}, 作业编号: ${response.jobCode}, 待同步店铺: ${response.totalShops}个`
    }

    // 触发成功事件
    emits('success', response)

    // 延迟关闭弹窗，让用户看到结果
    setTimeout(() => {
      handleClose()
    }, 2000)
  } catch (error: any) {
    console.error('同步失败:', error)

    syncResult.value = {
      success: false,
      message: error.message || '同步任务创建失败，请稍后重试'
    }

    message.error('同步任务创建失败')
  } finally {
    loading.value = false
  }
}

/**
 * 关闭弹窗
 */
const handleClose = () => {
  // 重置表单
  dateRange.value = null
  formModel.periodType = 'weekly' // 重置为默认值
  formModel.shopIds = []
  syncResult.value = null
  syncSuccess.value = false // 重置成功标记

  emits('update:open', false)
}

/**
 * 过滤选项（用于搜索）
 */
const filterOption = (input: string, option: any) => {
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

/**
 * 监听弹窗打开，设置默认日期范围
 */
watch(
  () => props.open,
  newVal => {
    if (newVal) {
      // 默认设置最近30天
      dateRange.value = [dayjs().subtract(30, 'day'), dayjs()]
    }
  }
)
</script>

<style scoped>
.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}
</style>
