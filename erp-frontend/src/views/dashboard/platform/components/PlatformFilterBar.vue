<template>
  <div class="filter-bar-card">
    <div class="filter-content">
      <div class="filter-row">
        <div class="filter-group">
          <label class="filter-label">{{ t('platform.dashboard.filter.timeRange') }}</label>
          <a-radio-group
            :value="quickTimeRange"
            button-style="solid"
            @change="handleQuickTimeChange"
          >
            <a-radio-button value="today">{{ t('platform.dashboard.filter.today') }}</a-radio-button>
            <a-radio-button value="yesterday">{{ t('platform.dashboard.filter.yesterday') }}</a-radio-button>
            <a-radio-button value="last7days">{{ t('platform.dashboard.filter.last7Days') }}</a-radio-button>
            <a-radio-button value="thisMonth">{{ t('platform.dashboard.filter.thisMonth') }}</a-radio-button>
            <a-radio-button value="lastMonth">{{ t('platform.dashboard.filter.lastMonth') }}</a-radio-button>
          </a-radio-group>
          <a-range-picker
            v-model:value="localDateRange"
            class="date-picker"
            :allow-clear="false"
            @change="handleDateRangeChange"
          />
        </div>
      </div>

      <div class="filter-row">
        <div class="filter-group">
          <label class="filter-label">{{ t('platform.common.warehouse') }}</label>
          <a-select
            :value="warehouseIds"
            mode="multiple"
            :placeholder="t('platform.dashboard.filter.allWarehouses')"
            allow-clear
            :max-tag-count="2"
            :loading="warehouseLoading"
            :options="warehouseOptions"
            style="min-width: 220px; max-width: 420px"
            @change="handleWarehouseChange"
          />
        </div>

        <div class="filter-group">
          <label class="filter-label">{{ t('platform.dashboard.filter.wmsProvider') }}</label>
          <a-select
            :value="wmsTenantIds"
            mode="multiple"
            :placeholder="t('platform.dashboard.filter.allProviders')"
            allow-clear
            :max-tag-count="2"
            :loading="operatorLoading"
            :options="operatorOptions"
            style="min-width: 220px; max-width: 420px"
            @change="handleOperatorChange"
          />
        </div>

        <div class="filter-actions">
          <a-button
            type="primary"
            :loading="refreshing"
            :class="{ rotating: refreshing }"
            @click="handleManualRefresh"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
            {{ t('platform.common.refresh') }}
          </a-button>
        </div>
      </div>
    </div>

    <div class="filter-footer">
      <span class="moscow-time">{{ t('platform.dashboard.filter.moscowTime', { time: moscowTime }) }}</span>
      <span v-if="lastUpdateTime" class="update-time">
        {{ t('platform.dashboard.filter.lastUpdated', { time: lastUpdateTime }) }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ReloadOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { formatMoscowTime } from '../../utils/timezone'
import { isSuccess } from '@/api'
import { getWarehouseOptions } from '@/api/wms/warehouse'
import { listWmsOperators } from '@/api/tenant'

const { t } = useI18n()
const props = defineProps<{
  quickTimeRange: string
  dateRange: { start: string; end: string }
  warehouseIds: number[]
  wmsTenantIds: number[]
  lastUpdateTime: string
}>()

const emit = defineEmits<{
  (e: 'update:quickTimeRange', value: string): void
  (e: 'update:customDate', start: string, end: string): void
  (e: 'update:warehouseIds', value: number[]): void
  (e: 'update:wmsTenantIds', value: number[]): void
  (e: 'refresh'): void
}>()

const refreshing = ref(false)

const localDateRange = computed({
  get: (): [Dayjs, Dayjs] => [dayjs(props.dateRange.start), dayjs(props.dateRange.end)],
  set: (value: [Dayjs, Dayjs]) => {
    if (value && value.length === 2) {
      emit('update:customDate', value[0].format('YYYY-MM-DD'), value[1].format('YYYY-MM-DD'))
    }
  }
})

// 仓库 / 货主选项（平台视角，best-effort 加载；业务数据为空时下拉为空不影响 mock 预览）
const warehouseLoading = ref(false)
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const operatorLoading = ref(false)
const operatorOptions = ref<{ label: string; value: number }[]>([])

async function loadWarehouseOptions() {
  warehouseLoading.value = true
  try {
    const res = await getWarehouseOptions()
    if (isSuccess(res) && res.data) {
      warehouseOptions.value = res.data.map(w => ({ label: w.warehouseName, value: w.id }))
    }
  } catch (e) {
    console.error('加载仓库选项失败', e)
  } finally {
    warehouseLoading.value = false
  }
}

async function loadOperatorOptions() {
  operatorLoading.value = true
  try {
    const res = await listWmsOperators()
    if (isSuccess(res) && res.data) {
      operatorOptions.value = res.data.map(o => ({ label: o.tenantName, value: o.id }))
    }
  } catch (e) {
    console.error('加载服务商选项失败', e)
  } finally {
    operatorLoading.value = false
  }
}

function handleQuickTimeChange(e: any) {
  emit('update:quickTimeRange', e.target.value)
}

function handleDateRangeChange() {
  // localDateRange 的 setter 会自动触发 emit
}

function handleWarehouseChange(value: unknown) {
  emit('update:warehouseIds', (value as number[]) || [])
}

function handleOperatorChange(value: unknown) {
  emit('update:wmsTenantIds', (value as number[]) || [])
}

function handleManualRefresh() {
  refreshing.value = true
  emit('refresh')
  setTimeout(() => {
    refreshing.value = false
  }, 600)
}

const moscowTime = computed(() => formatMoscowTime())

onMounted(() => {
  loadWarehouseOptions()
  loadOperatorOptions()
})
</script>

<style scoped>
.filter-bar-card {
  background: var(--ant-color-bg-container);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 20px;
  border: 1px solid var(--ant-color-border-secondary);
  position: sticky;
  top: 0;
  z-index: 10;
}

.filter-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 13px;
  color: var(--ant-color-text-secondary);
  font-weight: 500;
  white-space: nowrap;
  margin: 0;
}

.date-picker {
  width: 240px;
}

.filter-actions {
  margin-left: auto;
}

.filter-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--ant-color-border-secondary);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.update-time,
.moscow-time {
  font-size: 12px;
  color: var(--ant-color-text-tertiary);
}

.moscow-time {
  font-family: 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif;
}

.rotating :deep(.anticon-reload) {
  animation: rotate 0.6s linear;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 767px) {
  .filter-bar-card {
    padding: 16px;
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .filter-group {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    margin-left: 0;
  }

  .filter-actions button {
    width: 100%;
  }

  .filter-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
