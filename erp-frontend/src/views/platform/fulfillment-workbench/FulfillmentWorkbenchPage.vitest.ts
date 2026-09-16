import { computed, createSSRApp, defineComponent, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { describe, expect, it, vi } from 'vitest'

import FulfillmentWorkbenchPage from './FulfillmentWorkbenchPage.vue'

vi.mock('@/components/Search', () => ({
  SearchActions: { name: 'SearchActionsStub', render: () => null }
}))

vi.mock('@/components/Lov/UserSelect.vue', () => ({
  default: { name: 'UserSelectStub', render: () => null }
}))

vi.mock('ant-design-vue', () => ({ message: { success: vi.fn() } }))

vi.mock('@/api', () => ({ isSuccess: () => false }))

vi.mock('@/api/wms/fulfillment', () => ({
  confirmOutboundPickReview: vi.fn(),
  getFulfillmentPickTask: vi.fn(),
  getOutboundPickReviewSummary: vi.fn(),
  listFulfillmentPickTasks: vi.fn()
}))

vi.mock('@/api/wms/warehouse', () => ({ getWarehouseOptions: vi.fn() }))

vi.mock('@/hooks/use-user-data', () => ({
  useUserData: () => ({
    allUsers: computed(() => []),
    loading: computed(() => false),
    loadAllUsers: vi.fn(),
    getUserName: (id: number) => `用户${id}`
  })
}))

const passthrough = (name: string) =>
  defineComponent({
    name,
    inheritAttrs: false,
    setup(props, { attrs, slots }) {
      return () =>
        h('section', { ...attrs, 'data-ui': name }, [
          attrs.title ? h('h2', String(attrs.title)) : null,
          slots.extra ? h('aside', slots.extra()) : null,
          slots.default?.()
        ])
    }
  })

const renderPage = async () => {
  const app = createSSRApp(FulfillmentWorkbenchPage)
  for (const name of [
    'a-card',
    'a-form',
    'a-form-item',
    'a-date-picker',
    'a-select',
    'a-input',
    'a-tag',
    'a-button',
    'a-space',
    'a-table',
    'a-modal',
    'a-descriptions',
    'a-descriptions-item',
    'a-textarea',
    'a-drawer',
    'a-spin'
  ]) {
    app.component(name, passthrough(name))
  }
  return renderToString(app)
}

describe('fulfillment workbench page layout', () => {
  it('presents search and outbound review in the same compact two-card structure as picking tasks', async () => {
    const html = await renderPage()

    expect(html.match(/data-ui="a-card"/g)).toHaveLength(2)
    expect(html).toContain('<h2>出库作业</h2>')
    expect(html).not.toContain('当日出库复核')
    expect(html).not.toContain('当日拣货任务')
    expect(html).toContain('class="workbench-summary"')
    expect(html).toContain('任务总数')
    expect(html).toContain('异常任务')
    expect(html.indexOf('class="workbench-summary"')).toBeLessThan(
      html.indexOf('data-ui="a-table"')
    )
  })
})
