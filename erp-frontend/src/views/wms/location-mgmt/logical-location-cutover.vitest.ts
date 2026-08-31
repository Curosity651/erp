import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('logical location management cutover', () => {
  it('uses the logical location drawer and removes the retired geometry drawer', () => {
    const page = readFileSync(fileURLToPath(new URL('./index.vue', import.meta.url)), 'utf8')
    const retired = fileURLToPath(new URL('./WarehouseLocationDrawer.vue', import.meta.url))

    expect(page).toContain("import LogicalLocationDrawer from './LogicalLocationDrawer.vue'")
    expect(existsSync(retired)).toBe(false)
  })

  it('does not expose retired geometry and pallet write APIs', () => {
    const api = readFileSync(
      fileURLToPath(new URL('../../../api/wms/location-mgmt/index.ts', import.meta.url)),
      'utf8'
    )

    expect(api).not.toContain('/wms/location-mgmt/structure')
    expect(api).not.toContain('/wms/location-mgmt/pallet-rules')
    expect(api).not.toContain('/wms/location-mgmt/generate')
  })
})
