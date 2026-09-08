<template>
  <a-modal
    v-model:open="visible"
    :title="modalTitle"
    width="1000px"
    :confirm-loading="loading"
    :body-style="{ maxHeight: 'calc(100vh - 200px)', overflowY: 'auto', padding: '16px' }"
    style="top: 40px"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="formData" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
      <!-- 基本信息 -->
      <a-card
        size="small"
        :title="t('system.salesTarget.basicInfo')"
        :body-style="{ padding: '12px' }"
        style="margin-bottom: 12px"
      >
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item
              :label="t('system.salesTarget.year')"
              name="year"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 18 }"
              :rules="[{ required: true, message: t('system.salesTarget.validation.year') }]"
            >
              <a-date-picker
                v-model:value="formData.yearValue"
                picker="year"
                style="width: 100%"
                :disabled="mode !== 'create'"
                @change="handleYearChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              :label="t('system.salesTarget.annualTarget')"
              name="annualAmount"
              :label-col="{ span: 8 }"
              :wrapper-col="{ span: 16 }"
              :rules="[
                { required: true, message: t('system.salesTarget.validation.annualAmount') },
                {
                  type: 'number',
                  min: 0.01,
                  message: t('system.salesTarget.validation.amountPositive')
                }
              ]"
            >
              <a-input-number
                v-model:value="formData.annualAmount"
                :precision="2"
                :min="0.01"
                addon-before="₽"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              :label="t('system.salesTarget.currency')"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 18 }"
            >
              <a-input value="RUB" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="24">
            <a-form-item
              :label="t('common.remarks')"
              name="annualRemark"
              :label-col="{ span: 2 }"
              :wrapper-col="{ span: 22 }"
            >
              <a-input
                v-model:value="formData.annualRemark"
                :placeholder="t('system.salesTarget.optional')"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-card>

      <!-- 月度目标 -->
      <a-card size="small">
        <template #title>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span>{{ t('system.salesTarget.monthlyAllocation') }}</span>
            <a-tag color="blue">{{
              t('system.salesTarget.monthsFilled', { count: filledMonthsCount })
            }}</a-tag>
          </div>
        </template>

        <!-- 统计信息和差额提示 -->
        <a-card
          v-if="formData.annualAmount && monthlySum > 0"
          size="small"
          :body-style="{ padding: '8px 12px' }"
          style="margin-bottom: 8px"
        >
          <a-space :size="16">
            <a-statistic
              :title="t('system.salesTarget.monthlyTotal')"
              :value="formatNumber(monthlySum)"
              :value-style="{ fontSize: '14px', color: '#1890ff' }"
              prefix="₽"
            />
            <a-statistic
              :title="t('system.salesTarget.difference')"
              :value="formatNumber(Math.abs(difference))"
              :value-style="{
                fontSize: '14px',
                color: difference >= 0 ? '#52c41a' : '#ff4d4f'
              }"
              :prefix="difference >= 0 ? '+₽' : '-₽'"
            />
            <a-statistic
              :title="t('system.salesTarget.completionRate')"
              :value="completionRate"
              suffix="%"
              :precision="1"
              :value-style="{ fontSize: '14px' }"
            />
          </a-space>
        </a-card>

        <!-- 月度目标表格 -->
        <div>
          <a-table
            :columns="monthlyColumns"
            :data-source="formData.monthlyTargets"
            :pagination="false"
            size="small"
            bordered
            :row-class-name="
              (_record: any, index: number) => (index % 2 === 1 ? 'table-striped' : '')
            "
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'month'">
                {{ getMonthName(record.month) }}
              </template>
              <template v-if="column.key === 'amount'">
                <a-input-number
                  v-model:value="record.amount"
                  :precision="2"
                  :min="0"
                  addon-before="₽"
                  style="width: 100%"
                  size="small"
                  @change="calculateSum"
                />
              </template>
              <template v-if="column.key === 'remark'">
                <a-input
                  v-model:value="record.remark"
                  :placeholder="t('system.salesTarget.optional')"
                  size="small"
                />
              </template>
            </template>
          </a-table>
        </div>
      </a-card>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { doRequest } from '@/utils/axios/request'
import { batchCreateSalesTarget, batchUpdateMonthly } from '@/api/system/sales-target'
import type {
  SalesTargetBatchCreateDTO,
  SalesTargetYearlyOverviewVO,
  MonthlyTargetInputDTO
} from '@/api/system/sales-target/types'
import dayjs, { Dayjs } from 'dayjs'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()

const emit = defineEmits(['submit-success'])

const visible = ref(false)
const loading = ref(false)
const formRef = ref()
const mode = ref<'create' | 'edit' | 'editAnnual'>('create')
const currentOverview = ref<SalesTargetYearlyOverviewVO>()

// 表单数据
const formData = reactive({
  year: new Date().getFullYear(),
  yearValue: dayjs() as Dayjs,
  currency: 'RUB',
  annualAmount: undefined as number | undefined,
  annualRemark: '',
  monthlyTargets: [] as MonthlyTargetInputDTO[]
})

// 模态框标题
const modalTitle = computed(() => {
  if (mode.value === 'create') return t('system.salesTarget.createSalesTarget')
  if (mode.value === 'edit') return t('system.salesTarget.batchEditMonthly')
  return t('system.salesTarget.editAnnual')
})

// 月份名称
const getMonthName = (month: number) => t('system.salesTarget.monthShort', { month })

// 月度目标表格列
const monthlyColumns = computed(() => [
  { title: t('system.salesTarget.month'), key: 'month', width: 80 },
  { title: t('system.salesTarget.targetAmount'), key: 'amount', width: 180 },
  { title: t('common.remarks'), key: 'remark' }
])

// 计算月度合计
const monthlySum = computed(() => {
  return formData.monthlyTargets.reduce((sum, item) => {
    return sum + (item.amount || 0)
  }, 0)
})

// 计算差额
const difference = computed(() => {
  return (formData.annualAmount || 0) - monthlySum.value
})

// 计算已填写月份数量
const filledMonthsCount = computed(() => {
  return formData.monthlyTargets.filter(item => item.amount && item.amount > 0).length
})

// 计算完成比例
const completionRate = computed(() => {
  if (!formData.annualAmount || formData.annualAmount === 0) return 0
  return (monthlySum.value / formData.annualAmount) * 100
})

// 格式化数字
const formatNumber = (num: number) => {
  return num.toLocaleString(locale.value, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 计算合计(触发响应式更新)
const calculateSum = () => {
  // 触发计算属性更新
}

// 年份变更处理
const handleYearChange = (date: Dayjs | null) => {
  if (date) {
    formData.year = date.year()
  }
}

// 初始化月度目标数据
const initMonthlyTargets = (overview?: SalesTargetYearlyOverviewVO) => {
  formData.monthlyTargets = []
  for (let month = 1; month <= 12; month++) {
    const existing = overview?.monthlyTargets?.find(t => t.targetMonth === month)
    formData.monthlyTargets.push({
      month,
      amount: existing ? parseFloat(existing.targetAmount) : undefined,
      remark: existing?.remark || ''
    })
  }
}

// 打开模态框
const open = (
  openMode: 'create' | 'edit' | 'editAnnual',
  year: number,
  overview?: SalesTargetYearlyOverviewVO
) => {
  mode.value = openMode
  formData.year = year
  formData.yearValue = dayjs().year(year)
  currentOverview.value = overview

  if (openMode === 'create') {
    // 创建模式:清空表单
    formData.currency = 'RUB'
    formData.annualAmount = undefined
    formData.annualRemark = ''
    initMonthlyTargets()
  } else {
    // 编辑模式:填充现有数据
    if (overview?.annualTarget) {
      formData.currency = 'RUB'
      formData.annualAmount = parseFloat(overview.annualTarget.targetAmount)
      formData.annualRemark = overview.annualTarget.remark || ''
    }
    initMonthlyTargets(overview)
  }

  visible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    if (mode.value === 'create') {
      // 批量创建
      const dto: SalesTargetBatchCreateDTO = {
        year: formData.year,
        currency: formData.currency,
        annualTarget: {
          amount: formData.annualAmount!,
          remark: formData.annualRemark
        },
        monthlyTargets: formData.monthlyTargets
          .filter(item => item.amount && item.amount > 0)
          .map(item => ({
            month: item.month,
            amount: item.amount,
            remark: item.remark
          }))
      }

      await doRequest(batchCreateSalesTarget(dto), {
        successMessage: t('system.salesTarget.createSuccess'),
        onSuccess: () => {
          visible.value = false
          emit('submit-success')
        }
      })
    } else if (mode.value === 'edit') {
      // 批量编辑:更新年度目标 + 更新/创建月度目标
      const dto: any = {
        year: formData.year,
        currency: formData.currency,
        annualTargetId: currentOverview.value?.annualTarget?.id,
        annualTarget: {
          amount: formData.annualAmount!,
          remark: formData.annualRemark
        },
        updateTargets: [] as any[],
        createTargets: [] as any[]
      }

      // 分类处理月度目标:已存在的更新,不存在的创建
      formData.monthlyTargets.forEach(item => {
        if (!item.amount || item.amount <= 0) {
          return // 跳过没有金额的
        }

        const existing = currentOverview.value?.monthlyTargets?.find(
          t => t.targetMonth === item.month
        )
        if (existing?.id) {
          // 已存在,添加到更新列表
          dto.updateTargets.push({
            id: existing.id,
            targetAmount: item.amount,
            remark: item.remark || ''
          })
        } else {
          // 不存在,添加到创建列表
          dto.createTargets.push({
            month: item.month,
            amount: item.amount,
            remark: item.remark || ''
          })
        }
      })

      await doRequest(batchUpdateMonthly(dto), {
        successMessage: t('system.salesTarget.updateSuccess'),
        onSuccess: () => {
          visible.value = false
          emit('submit-success')
        }
      })
    }
  } catch (error) {
    console.error(t('system.salesTarget.submitFailed'), error)
  } finally {
    loading.value = false
  }
}

// 取消
const handleCancel = () => {
  visible.value = false
  formRef.value?.resetFields()
}

defineExpose({ open })
</script>

<style scoped>
:deep(.table-striped) {
  background-color: #fafafa;
}

:deep(.ant-statistic-title) {
  font-size: 12px;
  color: #666;
}
</style>
