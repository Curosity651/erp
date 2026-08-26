import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('asset finance page sections', () => {
  it('separates assets, payables and WMS fund accounts', () => {
    const source = readFileSync(fileURLToPath(new URL('./AssetFinancePage.vue', import.meta.url)), 'utf8')

    expect(source).toContain("value: 'assets'")
    expect(source).toContain("value: 'payables'")
    expect(source).toContain("value: 'wms-account'")
    expect(source).toContain('getAssetOverview')
    expect(source).toContain('getPayablesOverview')
  })

  it('keeps overview metrics and positions compact without summary tables', () => {
    const source = readFileSync(fileURLToPath(new URL('./AssetFinancePage.vue', import.meta.url)), 'utf8')

    expect(source).toContain('compact-summary-grid')
    expect(source).toContain('position-strip')
    expect(source).toContain('payable-summary-grid')
    expect(source).not.toContain(':data-source="payablesOverview?.supplierPayable ?? []"')
    expect(source).not.toContain(':data-source="payablesOverview ? [payablesOverview.providerPayable] : []"')
  })

  it('keeps WMS fund balances in one compact horizontal strip', () => {
    const source = readFileSync(fileURLToPath(new URL('./components/WmsFundAccountPanel.vue', import.meta.url)), 'utf8')

    expect(source).toContain('fund-summary-strip')
    expect(source).toContain('fund-summary-item')
    expect(source).toContain('account-detail-tabs')
  })

  it('places WMS account actions and update time in the page toolbar', () => {
    const page = readFileSync(fileURLToPath(new URL('./AssetFinancePage.vue', import.meta.url)), 'utf8')
    const panel = readFileSync(fileURLToPath(new URL('./components/WmsFundAccountPanel.vue', import.meta.url)), 'utf8')

    expect(page).toContain('ref="wmsFundAccountPanel"')
    expect(page).toContain('activeSection === \'wms-account\'')
    expect(page).toContain('登记充值')
    expect(page.indexOf('登记充值')).toBeLessThan(page.indexOf('更新于'))
    expect(page).toContain('finance-section-switcher')
    expect(page).toContain('.ant-segmented-item-selected')
    expect(panel).toContain('defineExpose')
    expect(panel).not.toContain('class="panel-actions"')
  })
})
