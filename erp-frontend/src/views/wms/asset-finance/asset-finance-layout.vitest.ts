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
})
