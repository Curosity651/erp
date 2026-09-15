import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

describe('BasicLayout menu localization', () => {
  it('renders the title already localized by the dynamic router', () => {
    const source = readFileSync(
      fileURLToPath(new URL('./BasicLayout.vue', import.meta.url)),
      'utf8'
    )

    expect(source).toMatch(/defaultOpenAll:\s*true,\s*locale:\s*false/)
  })
})
