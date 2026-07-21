<template>
  <a-modal
    :open="open"
    :title="`SKU 映射详情`"
    :body-style="{ paddingTop: '12px' }"
    :width="800"
    :footer="null"
    @cancel="handleClose"
  >
    <!-- SKU 信息展示 -->
    <div v-if="skuInfo" class="sku-info-section">
      <div class="sku-info-content">
        <div class="sku-image-wrapper">
          <img :src="getProductImageUrl()" alt="产品图片" class="sku-image" />
        </div>
        <div class="sku-text-info">
          <div class="sku-code-row">
            <span class="sku-label">SKU:</span>
            <span class="sku-code">{{ skuInfo.skuCode }}</span>
          </div>
          <div class="sku-name-row">
            <span class="sku-label">名称:</span>
            <span class="sku-name">{{ skuInfo.skuName }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 映射列表 -->
    <div class="mapping-list-section">
      <div class="section-header">
        <span class="section-title">平台映射列表 ({{ mappings.length }})</span>
        <a-button
          v-if="hasPermission('product:sku-mapping:add')"
          type="primary"
          size="small"
          @click="handleAddMapping"
        >
          <template #icon>
            <plus-outlined />
          </template>
          添加映射
        </a-button>
      </div>

      <a-spin :spinning="loading">
        <a-empty v-if="!loading && mappings.length === 0" description="暂无映射" />

        <div v-else class="mapping-list">
          <div v-for="mapping in mappings" :key="mapping.id" class="mapping-item">
            <div class="mapping-info">
              <div class="mapping-id">
                <span class="label">平台商品 ID:</span>
                <span class="value">{{ mapping.platformItemId }}</span>
              </div>
              <div class="mapping-time">
                <span class="label">创建时间:</span>
                <span class="value">{{ formatTime(mapping.createTime) }}</span>
              </div>
            </div>
            <div class="mapping-actions">
              <a-popconfirm
                v-if="hasPermission('product:sku-mapping:del')"
                title="确定要删除此映射吗？"
                @confirm="handleDeleteMapping(mapping)"
              >
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>
      </a-spin>
    </div>

    <!-- 快速添加映射表单 -->
    <sku-mapping-quick-add-modal
      v-model:open="quickAddVisible"
      :sku-code="skuCode"
      :sku-name="skuInfo?.skuName"
      :sku-files="skuFiles"
      @success="handleMappingChanged"
    />
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useAuthorize } from '@/hooks/permission'
import { getSkuMappingsBySkuCode, deleteSkuMapping } from '@/api/product/sku-mapping'
import type { SkuMappingListItem } from '@/api/product/sku-mapping/types'
import SkuMappingQuickAddModal from './SkuMappingQuickAddModal.vue'

interface Props {
  open: boolean
  skuCode: string
  skuName?: string
  skuFiles?: any
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const { hasPermission } = useAuthorize()

const loading = ref(false)
const mappings = ref<SkuMappingListItem[]>([])
const quickAddVisible = ref(false)

import { getSkuMainImage } from '@/utils/sku-utils'

const skuInfo = ref<{ skuCode: string; skuName: string } | null>(null)

const ossDomain = import.meta.env.VITE_OSS_DOMAIN

// 获取产品图片URL - getSkuMainImage 已经处理了所有回退逻辑
const getProductImageUrl = () => {
  return getSkuMainImage(props.skuFiles, ossDomain)
}

// 监听 visible 变化，加载数据
watch(
  () => props.open,
  newVal => {
    if (newVal) {
      skuInfo.value = {
        skuCode: props.skuCode,
        skuName: props.skuName || props.skuCode
      }
      loadMappings()
    }
  }
)

// 加载映射列表
const loadMappings = async () => {
  if (!props.skuCode) return

  loading.value = true
  try {
    const res = await getSkuMappingsBySkuCode(props.skuCode)
    if (res.code === 200) {
      mappings.value = res.data || []
    }
  } catch (error) {
    console.error('加载映射列表失败:', error)
    message.error('加载映射列表失败')
  } finally {
    loading.value = false
  }
}

// 格式化时间
const formatTime = (timeStr: string) => {
  if (!timeStr) return '-'
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 添加映射
const handleAddMapping = () => {
  quickAddVisible.value = true
}

// 删除映射
const handleDeleteMapping = async (mapping: SkuMappingListItem) => {
  try {
    const res = await deleteSkuMapping(mapping.id)
    if (res.code === 200) {
      message.success('删除成功')
      loadMappings()
      emit('success')
    }
  } catch (error) {
    console.error('删除映射失败:', error)
    message.error('删除映射失败')
  }
}

// 映射变更成功
const handleMappingChanged = () => {
  loadMappings()
  emit('success')
}

// 关闭模态框
const handleClose = () => {
  emit('update:open', false)
}
</script>

<style scoped>
.sku-info-section {
  margin-bottom: 20px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #f0f2f5 100%);
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.sku-info-content {
  display: flex;
  align-items: center;
  gap: 18px;
}

.sku-image-wrapper {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border: 2px solid #d9d9d9;
  border-radius: 10px;
  background: #ffffff;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.sku-image-wrapper:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.sku-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.2s ease;
}

.sku-image:hover {
  opacity: 0.9;
}

.sku-text-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.sku-code-row,
.sku-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sku-label {
  font-size: 12px;
  font-weight: 500;
  color: #8c8c8c;
  white-space: nowrap;
  min-width: 45px;
}

.sku-code {
  font-size: 14px;
  font-weight: 700;
  color: #1890ff;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  letter-spacing: 0.5px;
  word-break: break-all;
}

.sku-name {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
  word-break: break-word;
  line-height: 1.4;
}

.mapping-list-section {
  margin-top: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background: linear-gradient(180deg, #1890ff 0%, #096dd9 100%);
  border-radius: 2px;
}

.mapping-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mapping-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: all 0.25s ease;
  position: relative;
}

.mapping-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #1890ff;
  border-radius: 8px 0 0 8px;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.mapping-item:hover {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.12);
  transform: translateY(-1px);
}

.mapping-item:hover::before {
  opacity: 1;
}

.mapping-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mapping-id,
.mapping-time {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mapping-id .label,
.mapping-time .label {
  font-size: 12px;
  font-weight: 500;
  color: #8c8c8c;
  min-width: 100px;
}

.mapping-id .value {
  font-size: 13px;
  font-weight: 700;
  color: #262626;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  letter-spacing: 0.3px;
}

.mapping-time .value {
  font-size: 12px;
  color: #595959;
  font-weight: 500;
}

.mapping-actions {
  flex-shrink: 0;
}
</style>
