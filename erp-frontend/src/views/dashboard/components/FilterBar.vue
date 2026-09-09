<template>
  <div class="filter-bar-card">
    <div class="filter-content">
      <!-- 第一行：时间和平台筛选 -->
      <div class="filter-row">
        <div class="filter-group">
          <label class="filter-label">{{ t('dashboard.timeRange') }}</label>
          <a-radio-group
            :value="quickTimeRange"
            button-style="solid"
            @change="handleQuickTimeChange"
          >
            <a-radio-button value="today">{{ t('dashboard.today') }}</a-radio-button>
            <a-radio-button value="yesterday">{{ t('dashboard.yesterday') }}</a-radio-button>
            <a-radio-button value="last7days">{{ t('dashboard.last7Days') }}</a-radio-button>
            <a-radio-button value="thisMonth">{{ t('dashboard.thisMonth') }}</a-radio-button>
            <a-radio-button value="lastMonth">{{ t('dashboard.lastMonth') }}</a-radio-button>
          </a-radio-group>
          <a-range-picker
            v-model:value="localDateRange"
            class="date-picker"
            :allow-clear="false"
            @change="handleDateRangeChange"
          />
        </div>

        <div class="filter-group">
          <label class="filter-label">{{ t('dashboard.platform') }}</label>
          <PlatformSelect
            :value="platform"
            width="150px"
            allow-clear
            @change="handlePlatformChange"
          />
        </div>
      </div>

      <!-- 第二行：SKU、店铺、品类筛选 -->
      <div class="filter-row">
        <div class="filter-group">
          <label class="filter-label">SKU</label>
          <SkuSelectInput
            :model-value="localSkuCodes"
            :multiple="true"
            :placeholder="t('dashboard.selectSku')"
            style="min-width: 200px; max-width: 400px"
            @update:model-value="handleSkuCodesChange"
          />
        </div>

        <div class="filter-group">
          <label class="filter-label">{{ t('dashboard.shop') }}</label>
          <ShopSelectInput
            :model-value="localShopIds"
            :multiple="true"
            :placeholder="t('dashboard.selectShop')"
            style="min-width: 200px"
            @update:model-value="handleShopIdsChange"
          />
        </div>

        <div class="filter-group">
          <label class="filter-label">{{ t('dashboard.category') }}</label>
          <CategoryTreeSelect
            :value="localCategoryId"
            :placeholder="t('dashboard.selectCategory')"
            style="width: 180px"
            @update:value="handleCategoryChange"
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
            {{ t('dashboard.refresh') }}
          </a-button>
        </div>
      </div>
    </div>

    <div class="filter-footer">
      <span class="moscow-time">{{ t('dashboard.moscowTime', { time: moscowTime }) }}</span>
      <span v-if="lastUpdateTime" class="update-time">{{ t('dashboard.lastUpdated', { time: lastUpdateTime }) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ReloadOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { formatMoscowTime } from '../utils/timezone'
import ShopSelectInput from '@/components/ShopSelectInput.vue'
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import CategoryTreeSelect from '@/components/Lov/CategoryTreeSelect.vue'
import { PlatformSelect } from '@/components/Platform'

const { t } = useI18n()

const props = defineProps<{
  quickTimeRange: string
  dateRange: { start: string; end: string }
  platform: string
  lastUpdateTime: string
  skuCodes?: string[]
  shopIds?: number[]
  categoryId?: number
}>()

const emit = defineEmits<{
  (e: 'update:quickTimeRange', value: string): void
  (e: 'update:customDate', start: string, end: string): void
  (e: 'update:platform', value: string): void
  (e: 'update:skuCodes', value: string[]): void
  (e: 'update:shopIds', value: number[]): void
  (e: 'update:categoryId', value: number | undefined): void
  (e: 'refresh'): void
}>()

// 刷新状态
const refreshing = ref(false)

// 本地展示用的日期选择器值（双向绑定）
const localDateRange = computed({
  get: (): [Dayjs, Dayjs] => [dayjs(props.dateRange.start), dayjs(props.dateRange.end)],
  set: (value: [Dayjs, Dayjs]) => {
    if (value && value.length === 2) {
      emit('update:customDate', value[0].format('YYYY-MM-DD'), value[1].format('YYYY-MM-DD'))
    }
  }
})

// 本地 SKU 编码列表
const localSkuCodes = computed({
  get: () => props.skuCodes || [],
  set: (value: string[]) => emit('update:skuCodes', value)
})

// 本地店铺 ID 列表
const localShopIds = computed({
  get: () => props.shopIds || [],
  set: (value: number[]) => emit('update:shopIds', value)
})

// 本地品类 ID
const localCategoryId = computed({
  get: () => props.categoryId,
  set: (value: number | undefined) => emit('update:categoryId', value)
})

// 快捷时间变化
function handleQuickTimeChange(e: any) {
  emit('update:quickTimeRange', e.target.value)
}

// 日期范围变化（手动选择日期）
function handleDateRangeChange() {
  // localDateRange 的 setter 会自动触发 emit
}

// 平台变化
function handlePlatformChange(value: string) {
  emit('update:platform', value)
}

// SKU 编码变化
function handleSkuCodesChange(value: string | string[] | undefined) {
  const codes = Array.isArray(value) ? value : value ? [value] : []
  emit('update:skuCodes', codes)
}

// 店铺 ID 变化
function handleShopIdsChange(value: number | number[] | undefined) {
  const ids = Array.isArray(value) ? value : value !== undefined ? [value] : []
  emit('update:shopIds', ids)
}

// 品类变化
function handleCategoryChange(value: number | number[] | undefined) {
  // CategoryTreeSelect 可能返回 number 或 number[]，这里只处理单选情况
  const categoryId = Array.isArray(value) ? value[0] : value
  emit('update:categoryId', categoryId)
}

// 手动刷新按钮
function handleManualRefresh() {
  refreshing.value = true
  emit('refresh')
  // 模拟刷新动画
  setTimeout(() => {
    refreshing.value = false
  }, 600)
}

// 计算莫斯科当前时间
const moscowTime = computed(() => formatMoscowTime())
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

/* 刷新按钮旋转动画 */
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

/* 响应式布局 */
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
