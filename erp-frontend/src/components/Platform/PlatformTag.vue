<script setup lang="ts">
/**
 * 平台标签组件
 *
 * 用于展示电商平台标识，统一渐变背景样式
 *
 * @example
 * <PlatformTag platform="wildberries" />
 * <PlatformTag platform="ozon" />
 */
import { computed } from 'vue'
import { PLATFORM_CONFIG, type PlatformType } from '@/constants/platform'

defineOptions({ name: 'PlatformTag' })

interface Props {
  /** 平台类型 */
  platform: PlatformType
}

const props = defineProps<Props>()

const config = computed(() => PLATFORM_CONFIG[props.platform])
</script>

<template>
  <span :class="['platform-tag', config?.tagClass]">
    {{ config?.label }}
  </span>
</template>

<style scoped>
.platform-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  border-radius: 4px;
  letter-spacing: 0.2px;
  white-space: nowrap;
  color: #fff;
}

/* Wildberries - 紫蓝渐变 */
.platform-tag--wb {
  background: linear-gradient(45deg, #6a11cb, #2575fc);
}

/* Ozon - 蓝青渐变 */
.platform-tag--ozon {
  background: linear-gradient(45deg, #007bff, #00d4ff);
}

/* Yandex - 橙黄渐变 */
.platform-tag--yandex {
  background: linear-gradient(45deg, #ff6b00, #ffc107);
}
</style>
