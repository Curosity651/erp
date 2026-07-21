<template>
  <div class="sku-display-cell" :title="`${skuName || ''}\n${skuCode}`">
    <div
      class="sku-display-cell__thumb"
      :style="mainImage ? { backgroundImage: toCssBgUrl(mainImage) } : {}"
    >
      <PictureOutlined v-if="!mainImage" class="sku-display-cell__icon" />
    </div>
    <div class="sku-display-cell__text">
      <span class="sku-display-cell__name">{{ skuName || '-' }}</span>
      <span class="sku-display-cell__code">{{ skuCode }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { PictureOutlined } from '@ant-design/icons-vue'
import { toCssBgUrl } from '@/utils/style-utils'
import type { SkuBriefVO } from '@/api/wms/inventory/types'

defineOptions({ name: 'SkuDisplayCell' })

const props = defineProps<{
  brief?: SkuBriefVO
}>()

const skuCode = computed(() => props.brief?.skuCode || '-')
const skuName = computed(() => props.brief?.skuName)
const mainImage = computed(() => props.brief?.mainImage)
</script>

<style scoped lang="less">
.sku-display-cell {
  display: flex;
  align-items: center;

  &__thumb {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    margin-right: 8px;
    overflow: hidden;
    background-color: #f5f5f5;
    background-position: center;
    background-size: cover;
    background-repeat: no-repeat;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #f0f0f0;
  }

  &__icon {
    font-size: 20px;
    color: #bfbfbf;
  }

  &__text {
    display: flex;
    flex-direction: column;
    min-width: 0;
    line-height: 1.4;
  }

  &__name {
    font-size: 14px;
    color: #262626;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__code {
    font-size: 12px;
    color: #8c8c8c;
    white-space: nowrap;
  }
}
</style>
