<template>
  <a-form-item :wrapper-col="{ flex: '1 1 0' }" class="search-actions-wrapper">
    <a-space size="middle">
      <a-space>
        <!-- 自定义内容位置 -->
        <slot />
        <a-button type="primary" :loading="props.loading" @click="emits('search')">
          {{ t('common.search') }}
        </a-button>
        <a-button @click="emits('reset')">{{ t('common.reset') }}</a-button>
      </a-space>
      <a v-if="props.collapsible" @click="() => toggleCollapsed()">
        {{ innerCollapsed ? t('common.expand') : t('common.collapse') }}
        <DownOutlined v-if="innerCollapsed" />
        <UpOutlined v-else />
      </a>
      <a v-if="props.showAdvancedToggle" @click="() => toggleAdvanced()">
        {{
          props.showAdvanced ? t('common.collapseAdvanced') : t('common.expandAdvanced')
        }}
        <DownOutlined v-if="!props.showAdvanced" />
        <UpOutlined v-else />
      </a>
    </a-space>
  </a-form-item>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    loading?: boolean
    collapsible?: boolean
    collapsed?: boolean
    showAdvancedToggle?: boolean
    showAdvanced?: boolean
  }>(),
  {
    loading: false,
    collapsible: false,
    collapsed: true,
    showAdvancedToggle: false,
    showAdvanced: false
  }
)

const emits = defineEmits<{
  (e: 'update:collapsed', collapsed: boolean): void
  (e: 'search'): void
  (e: 'reset'): void
  (e: 'toggle-advanced'): void
}>()

// 双向绑定
const innerCollapsed = useVModel(props, 'collapsed', emits)

const toggleCollapsed = () => {
  innerCollapsed.value = !innerCollapsed.value
}

const toggleAdvanced = () => {
  emits('toggle-advanced')
}
</script>

<script lang="ts">
export default {
  name: 'SearchActions'
}
</script>

<style scoped></style>
