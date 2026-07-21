<template>
  <div class="placeholder-page">
    <a-result status="info" :title="title" sub-title="本模块开发中，当前为占位页面">
      <template #icon>
        <tool-outlined style="color: #1677ff" />
      </template>
      <template #extra>
        <a-typography-paragraph type="secondary" style="margin: 0">
          页面已按身份分配占位，后续阶段将填充具体功能。
        </a-typography-paragraph>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
import { ToolOutlined } from '@ant-design/icons-vue'
import { useRoute } from 'vue-router'

const route = useRoute()
// 标题取自菜单（route.meta.name），同一占位组件被多个菜单复用
const title = computed(() => (route.meta?.name as string) || '占位页面')
</script>

<script lang="ts">
export default {
  name: 'CommonPlaceholderPage'
}
</script>

<style scoped>
.placeholder-page {
  padding: 24px;
}
</style>
