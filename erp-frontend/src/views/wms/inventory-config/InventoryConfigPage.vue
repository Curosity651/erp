<template>
  <div class="inventory-config-page">
    <div class="mb-4">
      <GlobalConfigSection ref="globalConfigRef" />
    </div>

    <SkuConfigTable
      :threshold-days="globalThresholdDays"
      :default-safety-stock="globalSafetyStock"
      @refresh="refreshGlobalConfig"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import GlobalConfigSection from './components/GlobalConfigSection.vue'
import SkuConfigTable from './components/SkuConfigTable.vue'

defineOptions({ name: 'InventoryConfigPage' })

const globalConfigRef = ref<InstanceType<typeof GlobalConfigSection>>()

const globalThresholdDays = computed(() => {
  return globalConfigRef.value?.config?.notifyThresholdDays ?? 7
})

const globalSafetyStock = computed(() => {
  return globalConfigRef.value?.config?.safetyStock ?? 200
})

function refreshGlobalConfig() {
  globalConfigRef.value?.loadConfig()
}
</script>

<style scoped>
.inventory-config-page {
  padding: 0;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
