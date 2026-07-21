import { defineStore } from 'pinia'
import type { DashboardFilters } from '@/api/dashboard/types'

/**
 * Dashboard 筛选条件共享 Store
 * 用于在 Dashboard 和 SkuRanking 页面之间传递筛选条件
 */
export const useDashboardFilterStore = defineStore('dashboardFilter', {
  state: () => ({
    // 共享的筛选条件
    sharedFilters: null as DashboardFilters | null,
    // 是否有待消费的筛选条件
    hasFilters: false
  }),
  actions: {
    /**
     * 设置筛选条件（从 Dashboard 跳转时调用）
     */
    setFilters(filters: DashboardFilters) {
      this.sharedFilters = { ...filters }
      this.hasFilters = true
    },
    /**
     * 获取并消费筛选条件（SkuRanking 页面加载时调用）
     * 消费后会清空，避免重复使用
     */
    consumeFilters(): DashboardFilters | null {
      if (!this.hasFilters || !this.sharedFilters) {
        return null
      }
      const filters = { ...this.sharedFilters }
      // 消费后清空
      this.sharedFilters = null
      this.hasFilters = false
      return filters
    },
    /**
     * 清空筛选条件
     */
    clearFilters() {
      this.sharedFilters = null
      this.hasFilters = false
    }
  }
})
