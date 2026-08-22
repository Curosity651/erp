<template>
  <!-- 这里必须也要保持 keepAlive，否则嵌套路由处理时，子组件会被创建多次 -->
  <!-- eslint-disable-next-line vue/no-template-shadow -->
  <router-view v-slot="{ Component, route }">
    <keep-alive :include="includeComponentNames">
      <component
        :is="multiTabStore.contentLoading ? emptyNode : Component"
        :key="getComponentKey(Component, route)"
      />
    </keep-alive>
  </router-view>
</template>

<script setup lang="ts">
import { useMultiTabStore } from '@/stores/multitab-store'
import type { VNode } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { emptyNodeName } from '@/config'

const emptyNode = h('div') as VNode

const multiTabStore = useMultiTabStore()
const includeComponentNames = computed(() => multiTabStore.includeComponentNames)

// 每个标签页使用独立缓存键，避免不同菜单共用 RouterLayout 时复用错误的页面树。
const getComponentKey = (Component: VNode, route: RouteLocationNormalizedLoaded) => {
  if (multiTabStore.contentLoading) return emptyNodeName
  if (Component) {
    // 如果路由有自定义缓存策略，使用自定义缓存键
    const matched = route.matched.find(r => r.meta?.cacheStrategy && r.meta?.cacheKey)
    if (matched && matched.meta?.cacheKey) {
      const customCacheKey = (matched.meta.cacheKey as Function)(route)
      return customCacheKey
    }

    return route.fullPath
  }
}
</script>

<script lang="ts">
import { routerLayoutName } from '@/config'

export default {
  name: routerLayoutName
}
</script>
