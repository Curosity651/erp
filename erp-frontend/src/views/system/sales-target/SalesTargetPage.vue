<template>
  <div class="sales-target-page">
    <!-- 年份选择器和操作按钮 -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-space size="middle">
        <span>{{ t('system.salesTarget.year') }}:</span>
        <a-date-picker
          v-model:value="selectedYear"
          picker="year"
          style="width: 120px"
          @change="handleYearChange"
        />
        <a-button @click="loadData">
          <template #icon>
            <ReloadOutlined />
          </template>
          {{ t('system.salesTarget.refresh') }}
        </a-button>
        <a-button
          v-if="hasPermission('system:sales-target:add')"
          type="primary"
          @click="handleCreateTarget"
        >
          <template #icon>
            <PlusOutlined />
          </template>
          {{ t('system.salesTarget.createTarget') }}
        </a-button>
        <a-button
          v-if="hasPermission('system:sales-target:edit') && overview?.monthlyTargets?.length"
          @click="handleBatchEdit"
        >
          <template #icon>
            <EditOutlined />
          </template>
          {{ t('system.salesTarget.batchEdit') }}
        </a-button>
      </a-space>
    </a-card>

    <!-- 年度概览卡片 -->
    <a-card
      v-if="overview?.annualTarget"
      :bordered="false"
      :title="t('system.salesTarget.annualOverview')"
      style="margin-bottom: 16px"
    >
      <a-descriptions :column="3">
        <a-descriptions-item :label="t('system.salesTarget.annualTarget')">
          {{
            formatAmount(overview.annualTarget.targetAmount, overview.annualTarget.currency, false)
          }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('system.salesTarget.annualActual')">
          <span :style="{ color: getAmountColor(overview.annualAchievementRate) }">
            {{
              formatAmount(overview.annualActualAmount || 0, overview.annualTarget.currency, false)
            }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item :label="t('system.salesTarget.annualRate')">
          <span :style="{ color: getAchievementColor(overview.annualAchievementRate) }">
            {{ formatAchievementRate(overview.annualAchievementRate) }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item :label="t('system.salesTarget.monthlyTotal')">
          {{ formatAmount(overview.monthlySum, overview.annualTarget.currency, false) }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('system.salesTarget.difference')">
          <span :style="{ color: overview.difference >= 0 ? '#52c41a' : '#ff4d4f' }">
            {{ formatAmount(overview.difference, overview.annualTarget.currency, false) }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item :label="t('common.remarks')">
          {{ overview.annualTarget.remark || '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('system.salesTarget.createdBy')">
          {{ overview.annualTarget.createdBy }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('common.updateTime')">
          {{ overview.annualTarget.updateTime }}
        </a-descriptions-item>
      </a-descriptions>
      <template #extra>
        <a-space>
          <a-button
            v-if="hasPermission('system:sales-target:edit')"
            size="small"
            @click="handleEditAnnual"
          >
            {{ t('action.edit') }}
          </a-button>
          <a-button
            v-if="hasPermission('system:sales-target:del')"
            size="small"
            danger
            @click="handleDeleteAnnual"
          >
            {{ t('action.delete') }}
          </a-button>
        </a-space>
      </template>
    </a-card>

    <!-- 空状态 -->
    <a-card v-else :bordered="false" style="margin-bottom: 16px">
      <a-empty :description="t('system.salesTarget.noAnnualData')">
        <a-button
          v-if="hasPermission('system:sales-target:add')"
          type="primary"
          @click="handleCreateTarget"
        >
          {{ t('system.salesTarget.createAnnual') }}
        </a-button>
      </a-empty>
    </a-card>

    <!-- 月度目标表格 -->
    <a-card :bordered="false">
      <template #title>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>{{ t('system.salesTarget.monthlyTargets') }}</span>
          <a-tag v-if="overview?.annualTarget" color="blue">
            {{ t('system.salesTarget.monthsConfigured', { count: overview.monthlyCount }) }}
          </a-tag>
        </div>
      </template>
      <a-table
        :columns="monthlyColumns"
        :data-source="monthlyTableData"
        :loading="loading"
        :pagination="false"
        row-key="month"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'month'">
            {{ getMonthName(record.month) }}
          </template>
          <template v-if="column.key === 'targetAmount'">
            {{ formatAmount(record.targetAmount, record.currency, false) }}
          </template>
          <template v-if="column.key === 'actualAmount'">
            <span
              v-if="record.actualAmount !== undefined"
              :style="{ color: getAmountColor(record.achievementRate) }"
            >
              {{ formatAmount(record.actualAmount, record.currency, false) }}
            </span>
            <span v-else style="color: #999">{{ t('system.salesTarget.noData') }}</span>
          </template>
          <template v-if="column.key === 'achievementRate'">
            <span
              v-if="record.achievementRate !== undefined"
              :style="{ color: getAchievementColor(record.achievementRate) }"
            >
              {{ formatAchievementRate(record.achievementRate) }}
            </span>
            <span v-else>-</span>
          </template>
          <template v-if="column.key === 'operate'">
            <a
              v-if="hasPermission('system:sales-target:edit') && record.id"
              @click="handleEditMonthly(record)"
            >
              {{ t('action.edit') }}
            </a>
            <a
              v-if="
                hasPermission('system:sales-target:add') && !record.id && overview?.annualTarget
              "
              @click="handleCreateMonthly(record)"
            >
              {{ t('action.create') }}
            </a>
          </template>
        </template>
        <template #summary>
          <a-table-summary>
            <a-table-summary-row>
              <a-table-summary-cell :index="0">{{
                t('system.salesTarget.total')
              }}</a-table-summary-cell>
              <a-table-summary-cell :index="1">
                <strong>
                  {{
                    formatAmount(overview?.monthlySum || 0, overview?.annualTarget?.currency, false)
                  }}
                </strong>
              </a-table-summary-cell>
              <a-table-summary-cell :index="2">-</a-table-summary-cell>
              <a-table-summary-cell :index="3">-</a-table-summary-cell>
              <a-table-summary-cell :index="4">-</a-table-summary-cell>
              <a-table-summary-cell :index="5">-</a-table-summary-cell>
            </a-table-summary-row>
          </a-table-summary>
        </template>
      </a-table>
    </a-card>

    <!-- 批量创建/编辑模态框 -->
    <sales-target-batch-form-modal ref="batchFormModalRef" @submit-success="loadData" />

    <!-- 统一的编辑模态框 -->
    <sales-target-edit-modal ref="editModalRef" @submit-success="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Modal } from 'ant-design-vue'
import { ReloadOutlined, PlusOutlined, EditOutlined } from '@ant-design/icons-vue'
import { useAuthorize } from '@/hooks/permission'
import { doRequest } from '@/utils/axios/request'
import { getYearlyOverview, deleteSalesTarget } from '@/api/system/sales-target'
import type { SalesTargetYearlyOverviewVO } from '@/api/system/sales-target/types'
import SalesTargetBatchFormModal from './SalesTargetBatchFormModal.vue'
import SalesTargetEditModal from './SalesTargetEditModal.vue'
import dayjs, { Dayjs } from 'dayjs'
import { formatAmount } from '@/utils/currency-utils'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'SalesTargetPage' })

const { hasPermission } = useAuthorize()
const { t } = useI18n()

// 年份选择
const currentYear = new Date().getFullYear()
const selectedYear = ref<Dayjs>(dayjs().year(currentYear))

// 数据加载
const loading = ref(false)
const overview = ref<SalesTargetYearlyOverviewVO>()

// 模态框引用
const batchFormModalRef = ref()
const editModalRef = ref()

// 月份名称映射
const getMonthName = (month: number) => {
  return t('system.salesTarget.monthName', {
    month,
    name: t(`system.salesTarget.months.${month}`)
  })
}

// 获取达成率颜色
const getAchievementColor = (rate?: number) => {
  if (rate === undefined || rate === null) return '#999'
  if (rate >= 100) return '#52c41a' // 绿色
  if (rate >= 80) return '#faad14' // 橙色
  return '#ff4d4f' // 红色
}

// 获取金额颜色(基于达成率)
const getAmountColor = (rate?: number) => {
  if (rate === undefined || rate === null) return '#999'
  return getAchievementColor(rate)
}

// 格式化达成率
const formatAchievementRate = (rate?: number) => {
  if (rate === undefined || rate === null) return '-'
  return `${rate.toFixed(2)}%`
}

// 月度目标表格列定义
const monthlyColumns = computed(() => [
  {
    title: t('system.salesTarget.month'),
    key: 'month',
    dataIndex: 'month',
    width: 150
  },
  {
    title: t('system.salesTarget.targetAmount'),
    key: 'targetAmount',
    dataIndex: 'targetAmount',
    width: 150
  },
  {
    title: t('system.salesTarget.actualAmount'),
    key: 'actualAmount',
    width: 150
  },
  {
    title: t('system.salesTarget.achievementRate'),
    key: 'achievementRate',
    width: 120
  },
  {
    title: t('common.remarks'),
    dataIndex: 'remark',
    ellipsis: true
  },
  {
    title: t('common.operation'),
    key: 'operate',
    width: 100,
    align: 'center'
  }
])

// 月度目标表格数据(始终显示12个月)
const monthlyTableData = computed(() => {
  const data = []
  for (let month = 1; month <= 12; month++) {
    const target = overview.value?.monthlyTargets?.find(t => t.targetMonth === month)
    data.push({
      month,
      id: target?.id,
      targetAmount: target?.targetAmount,
      currency: target?.currency || overview.value?.annualTarget?.currency,
      remark: target?.remark || '-',
      actualAmount: target?.actualAmount,
      achievementRate: target?.achievementRate
    })
  }
  return data
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const year = selectedYear.value.year()
    const { data } = await getYearlyOverview(year)
    overview.value = data
  } catch (error) {
    console.error(t('system.salesTarget.loadFailed'), error)
  } finally {
    loading.value = false
  }
}

// 年份变更
const handleYearChange = () => {
  loadData()
}

// 创建目标
const handleCreateTarget = () => {
  const year = selectedYear.value.year()
  // 如果没有年度目标,使用编辑模态框创建年度目标
  if (!overview.value?.annualTarget) {
    editModalRef.value?.open('createAnnual', { year })
  } else {
    // 如果已有年度目标,使用批量表单创建月度目标
    batchFormModalRef.value?.open('create', year)
  }
}

// 批量编辑
const handleBatchEdit = () => {
  const year = selectedYear.value.year()
  batchFormModalRef.value?.open('edit', year, overview.value)
}

// 编辑年度目标
const handleEditAnnual = () => {
  const year = selectedYear.value.year()
  if (overview.value?.annualTarget) {
    editModalRef.value?.open('editAnnual', {
      year,
      annualTarget: overview.value.annualTarget
    })
  }
}

// 删除年度目标
const handleDeleteAnnual = () => {
  Modal.confirm({
    title: t('system.salesTarget.confirmDelete'),
    content: t('system.salesTarget.deleteAnnualContent'),
    onOk: async () => {
      if (overview.value?.annualTarget?.id) {
        await doRequest(deleteSalesTarget(overview.value.annualTarget.id), {
          successMessage: t('message.removeSuccess'),
          onSuccess: () => loadData()
        })
      }
    }
  })
}

// 编辑单个月度目标
const handleEditMonthly = (record: any) => {
  const year = selectedYear.value.year()
  editModalRef.value?.open('editMonthly', {
    year,
    record
  })
}

// 创建单个月度目标
const handleCreateMonthly = (record: any) => {
  const year = selectedYear.value.year()
  editModalRef.value?.open('createMonthly', {
    year,
    record
  })
}

// 初始化
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="less">
.sales-target-page {
  padding: 0;
}
</style>
