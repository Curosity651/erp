<template>
  <a-modal
    title="平台区域映射"
    :open="visible"
    :footer="null"
    :width="520"
    :mask-closable="false"
    class="platform-mapping-modal"
    @cancel="handleClose"
  >
    <div class="modal-description">
      配置各电商平台与销售区域的对应关系，用于订单预占和库存管理
    </div>

    <div class="platform-mapping-list">
      <div
        v-for="item in mappingList"
        :key="item.platform"
        class="mapping-card"
        :class="[
          `platform-${item.platform}`,
          { 'is-modified': item.modified, 'is-saved': item.saved }
        ]"
      >
        <div class="platform-info">
          <div class="platform-icon" :class="`icon-${item.platform}`">
            <component :is="getPlatformIcon(item.platform)" />
          </div>
          <div class="platform-name">
            {{ getPlatformLabel(item.platform) }}
          </div>
        </div>

        <div class="mapping-control">
          <a-select
            v-model:value="item.regionId"
            placeholder="未配置"
            :options="regionOptions"
            :field-names="{ label: 'regionName', value: 'id' }"
            :loading="regionLoading"
            :disabled="item.saving"
            allow-clear
            class="region-selector"
            @change="() => handleRegionChange(item)"
          />

          <div class="action-col">
            <transition name="action-fade" mode="out-in">
              <a-button
                v-if="item.modified"
                type="primary"
                size="small"
                :loading="item.saving"
                class="save-btn"
                @click="handleSave(item)"
              >
                <template #icon><save-outlined /></template>
                保存
              </a-button>
              <span v-else-if="item.saved" class="save-success">
                <check-circle-filled />
              </span>
            </transition>
          </div>
        </div>
      </div>
    </div>

    <!-- 未保存提示 -->
    <a-modal
      v-model:open="confirmVisible"
      title="提示"
      :width="360"
      centered
      @ok="confirmClose"
      @cancel="confirmVisible = false"
    >
      <div class="confirm-content">
        <exclamation-circle-outlined class="confirm-icon" />
        <span>有未保存的修改，确认关闭？</span>
      </div>
    </a-modal>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import {
  CheckCircleFilled,
  SaveOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'
import { useModal } from '@/hooks/modal'
import { isSuccess } from '@/api'
import { doRequest } from '@/utils/axios/request'
import { listAllPlatformMappings, savePlatformMapping } from '@/api/wms/region'
import { listRegionOptions } from '@/api/wms/region'
import { getPlatformLabel, PLATFORM_OPTIONS } from '@/components/Platform'
import type { PlatformRegionMappingVO, RegionOptionVO } from '@/api/wms/region/types'

interface MappingItem {
  platform: string
  regionId: number | undefined
  originalRegionId: number | undefined
  modified: boolean
  saving: boolean
  saved: boolean
}

const { visible, openModal, closeModal } = useModal()

const mappingList = ref<MappingItem[]>([])
const regionOptions = ref<RegionOptionVO[]>([])
const regionLoading = ref(false)
const confirmVisible = ref(false)

// 是否有未保存的修改
const hasUnsavedChanges = computed(() => mappingList.value.some(item => item.modified))

// 平台图标组件
const getPlatformIcon = (platform: string) => {
  const icons: Record<string, () => ReturnType<typeof h>> = {
    ozon: () => h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('text', { x: '12', y: '16', 'text-anchor': 'middle', fill: '#fff', 'font-size': '10', 'font-weight': 'bold' }, 'O')
    ]),
    wildberries: () => h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('text', { x: '12', y: '16', 'text-anchor': 'middle', fill: '#fff', 'font-size': '10', 'font-weight': 'bold' }, 'W')
    ]),
    yandex: () => h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('text', { x: '12', y: '16', 'text-anchor': 'middle', fill: '#fff', 'font-size': '10', 'font-weight': 'bold' }, 'Y')
    ])
  }
  return icons[platform] || icons.ozon
}

/* 加载区域选项 */
const fetchRegionOptions = async () => {
  regionLoading.value = true
  try {
    const result = await listRegionOptions()
    if (isSuccess(result)) {
      regionOptions.value = result.data ?? []
    }
  } finally {
    regionLoading.value = false
  }
}

/* 加载平台映射数据 */
const fetchMappings = async () => {
  const result = await listAllPlatformMappings()
  const existingMappings: PlatformRegionMappingVO[] = isSuccess(result) ? (result.data ?? []) : []

  // 构建映射 Map
  const mappingMap = new Map<string, number>()
  existingMappings.forEach(m => mappingMap.set(m.platform, m.regionId))

  // 基于所有平台构建列表
  mappingList.value = PLATFORM_OPTIONS.map(opt => {
    const regionId = mappingMap.get(opt.value) ?? undefined
    return {
      platform: opt.value,
      regionId,
      originalRegionId: regionId,
      modified: false,
      saving: false,
      saved: false
    }
  })
}

/* 区域选择变更 */
const handleRegionChange = (item: MappingItem) => {
  item.modified = item.regionId !== item.originalRegionId
  item.saved = false
}

/* 保存单个映射 */
const handleSave = (item: MappingItem) => {
  item.saving = true
  doRequest(savePlatformMapping({ platform: item.platform, regionId: item.regionId }), {
    successMessage: '保存成功',
    onSuccess: () => {
      item.originalRegionId = item.regionId
      item.modified = false
      item.saved = true
      // 3秒后隐藏成功图标
      setTimeout(() => {
        item.saved = false
      }, 3000)
    },
    onFinally: () => {
      item.saving = false
    }
  })
}

/* 关闭弹窗 */
const handleClose = () => {
  if (hasUnsavedChanges.value) {
    confirmVisible.value = true
  } else {
    closeModal()
  }
}

/* 确认关闭 */
const confirmClose = () => {
  confirmVisible.value = false
  closeModal()
}

defineExpose({
  open() {
    openModal()
    fetchRegionOptions()
    fetchMappings()
  }
})
</script>

<style scoped>
.modal-description {
  color: var(--text-secondary-color, rgba(0, 0, 0, 0.45));
  font-size: 13px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color-split, #f0f0f0);
}

.platform-mapping-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mapping-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-radius: 10px;
  background: var(--component-background, #fff);
  border: 1px solid var(--border-color-split, #f0f0f0);
  transition: all 0.25s ease;
}

.mapping-card:hover {
  border-color: var(--primary-color-hover, #40a9ff);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.mapping-card.is-modified {
  border-color: var(--warning-color, #faad14);
  background: linear-gradient(135deg, rgba(250, 173, 20, 0.02) 0%, transparent 100%);
}

.mapping-card.is-saved {
  border-color: var(--success-color, #52c41a);
  background: linear-gradient(135deg, rgba(82, 196, 26, 0.02) 0%, transparent 100%);
}

.platform-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.platform-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  flex-shrink: 0;
}

.platform-icon :deep(svg) {
  width: 24px;
  height: 24px;
}

/* 平台品牌色 */
.icon-ozon {
  background: linear-gradient(135deg, #005bff 0%, #0041b3 100%);
}

.icon-wildberries {
  background: linear-gradient(135deg, #cb11ab 0%, #8e0c78 100%);
}

.icon-yandex {
  background: linear-gradient(135deg, #fc3f1d 0%, #d62c0e 100%);
}

.platform-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary-color, rgba(0, 0, 0, 0.85));
}

.mapping-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

.region-selector {
  width: 160px;
}

.region-selector :deep(.ant-select-selector) {
  border-radius: 6px;
}

.save-btn {
  min-width: 72px;
  border-radius: 6px;
  font-weight: 500;
}

.action-col {
  width: 80px;
  min-width: 80px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  align-items: center;
}

.save-success {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  color: var(--success-color, #52c41a);
  font-size: 20px;
  animation: success-pop 0.3s ease;
}

@keyframes success-pop {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 动画过渡 */
.action-fade-enter-active,
.action-fade-leave-active {
  transition: all 0.2s ease;
}

.action-fade-enter-from,
.action-fade-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

/* 确认弹窗 */
.confirm-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.confirm-icon {
  font-size: 22px;
  color: var(--warning-color, #faad14);
}

/* 弹窗样式覆盖 */
:deep(.ant-modal-header) {
  border-bottom: none;
  padding-bottom: 0;
}

:deep(.ant-modal-title) {
  font-size: 18px;
  font-weight: 600;
}

:deep(.ant-modal-body) {
  padding-top: 16px;
}
</style>
