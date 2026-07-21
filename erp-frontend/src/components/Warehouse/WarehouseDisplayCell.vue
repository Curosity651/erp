<template>
  <div class="warehouse-display-cell" :title="display?.warehouseName">
    <span class="warehouse-display-cell__icon">
      <HomeOutlined v-if="display?.warehouseType === 'OWN'" />
      <ShopOutlined v-else />
    </span>
    <span class="warehouse-display-cell__name">
      {{ display?.warehouseName || '-' }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { HomeOutlined, ShopOutlined } from '@ant-design/icons-vue'
import type { WarehouseDisplayVO } from '@/api/wms/inventory/types'

defineOptions({ name: 'WarehouseDisplayCell' })

defineProps<{
  display?: WarehouseDisplayVO
}>()
</script>

<style scoped lang="less">
.warehouse-display-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  &__icon {
    color: #8c8c8c;
    font-size: 15px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
  }

  &__name {
    font-size: 13px;
    color: #262626;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
