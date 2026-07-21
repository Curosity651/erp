<template>
  <!-- items 模式：纵向堆叠 -->
  <div v-if="items && items.length > 0" class="sku-info-list">
    <div v-for="(item, index) in items" :key="index" class="order-cell sku-info">
      <div class="sku-info__thumb" :style="{ backgroundImage: toCssBgUrl(item.mainImage) }"></div>
      <div class="sku-info__text">
        <div class="sku-info__row">
          <span class="sku-info__erp-code">{{ item.skuCode || ' ' }}</span>
          <span class="sku-info__erp-name">{{ item.skuName ? `(${item.skuName})` : ' ' }}</span>
          <span v-if="item.quantity > 1" class="sku-info__qty">&times;{{ item.quantity }}</span>
        </div>
        <div class="sku-info__row minor">
          <span>{{ item.platformItemId || '-' }}</span>
        </div>
      </div>
    </div>
  </div>

  <!-- 旧 props 兼容模式（降级） -->
  <div v-else-if="skuCode || article" class="order-cell sku-info" :title="titleText">
    <div class="sku-info__thumb" :style="{ backgroundImage: toCssBgUrl(mainImage) }"></div>
    <div class="sku-info__text">
      <div class="sku-info__row">
        <span class="sku-info__erp-code">{{ skuCode || ' ' }}</span>
        <span class="sku-info__erp-name">{{ skuName ? `(${skuName})` : ' ' }}</span>
      </div>
      <div class="sku-info__row minor">
        <span>{{ article || '-' }}</span>
      </div>
    </div>
  </div>

  <!-- 空状态 -->
  <span v-else class="sku-info-empty">-</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { toCssBgUrl } from '@/utils/style-utils'
import type { OrderItemVO } from '@/api/order/types'

interface Props {
  /** 新：items 数组 */
  items?: OrderItemVO[]
  /** @deprecated 旧 props，兼容模式 */
  mainImage?: string
  skuCode?: string
  skuName?: string
  article?: string
  skuCount?: number
  title?: string
}

const props = defineProps<Props>()

const titleText = computed(() => {
  if (props.title) return props.title
  const base = props.skuName || ''
  const more = props.skuCount && props.skuCount > 1 ? ` +${props.skuCount - 1}` : ''
  return `${base}${more}`
})
</script>

<style scoped lang="less">
.sku-info-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-cell.sku-info {
  display: flex;
  align-items: center;

  .sku-info__thumb {
    width: 48px;
    height: 48px;
    border-radius: 6px;
    margin-right: 8px;
    overflow: hidden;
    background: #f0f0f0 center/cover no-repeat;
    flex-shrink: 0;
  }

  .sku-info__text {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .sku-info__row {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }
  .sku-info__erp-code {
    font-weight: 600;
    font-size: 14px;
    white-space: nowrap;
    overflow: visible;
    text-overflow: clip;
    flex: 0 0 auto;
  }
  .sku-info__erp-name {
    color: #8c8c8c;
    font-size: 13px;
    flex: 1 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .sku-info__qty {
    color: #fa8c16;
    font-weight: 600;
    font-size: 13px;
    flex-shrink: 0;
  }
  .minor {
    font-size: 13px;
    color: #8c8c8c;
  }
}

.sku-info-empty {
  color: #8c8c8c;
}
</style>
