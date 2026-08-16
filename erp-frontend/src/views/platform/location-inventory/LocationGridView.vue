<template>
  <a-empty v-if="!rows.length" description="暂无库位" />
  <div v-else class="rack-list">
    <section v-for="rack in groupedRows" :key="rack.name" class="rack-band">
      <h3>{{ rack.name }}</h3>
      <div class="location-grid">
        <button
          v-for="row in rack.rows"
          :key="row.locationId"
          type="button"
          class="location-cell"
          :class="[
            utilizationLevel(row.utilizationPercent),
            { exceeded: row.volumeExceeded || row.weightExceeded }
          ]"
          :style="fillStyle(row)"
          @click="$emit('select', row.locationId)"
        >
          <span class="fill" />
          <span class="cell-content">
            <strong>{{ row.locationCode }}</strong>
            <span>{{ row.utilizationPercent.toFixed(1) }}%</span>
            <small>{{ row.totalQuantity }} 件 · {{ row.skuKindCount }} SKU</small>
          </span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { LocationInventoryGrid } from '@/api/wms/location-inventory/types'
import { calculateUtilization, utilizationLevel } from './location-utilization'

const props = defineProps<{ rows: LocationInventoryGrid[] }>()
defineEmits<{ (event: 'select', locationId: number): void }>()

const groupedRows = computed(() => {
  const groups = new Map<string, LocationInventoryGrid[]>()
  props.rows.forEach(row => {
    const name = row.rackNo || '未分排'
    groups.set(name, [...(groups.get(name) || []), row])
  })
  return Array.from(groups.entries()).map(([name, rows]) => ({ name, rows }))
})

const fillStyle = (row: LocationInventoryGrid) => ({
  '--fill': `${calculateUtilization(row.usedVolumeMm3, row.capacityVolumeMm3)}%`
})
</script>

<style scoped>
.rack-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.rack-band h3 {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
}

.location-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
}

.location-cell {
  position: relative;
  height: 92px;
  overflow: hidden;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  padding: 0;
  text-align: left;
  cursor: pointer;
}

.location-cell:hover {
  border-color: #1677ff;
}

.fill {
  position: absolute;
  inset: auto 0 0;
  height: var(--fill);
  background: #d9f7be;
}

.medium .fill {
  background: #fff1b8;
}

.high .fill {
  background: #ffd8bf;
}

.exceeded {
  border-color: #ff4d4f;
}

.cell-content {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
  flex-direction: column;
  justify-content: center;
  padding: 10px 12px;
  gap: 3px;
}

.cell-content strong {
  font-size: 14px;
}

.cell-content span {
  font-variant-numeric: tabular-nums;
  font-size: 18px;
  font-weight: 600;
}

.cell-content small {
  color: #595959;
}
</style>
