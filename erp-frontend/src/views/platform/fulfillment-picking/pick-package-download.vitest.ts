import { describe, expect, it } from 'vitest'
import { downloadPickPackageArchives } from './pick-package-download'

describe('fulfillment pick package download', () => {
  it('downloads the Russian warehouse archive first and the Chinese archive second', () => {
    const downloads: Array<{ url: string; fileName: string }> = []
    const delays: number[] = []

    downloadPickPackageArchives(
      {
        batchNo: 'FPP202609150001',
        taskId: 12,
        snapshotHash: 'hash',
        warehouseFileName: 'Sklad-FPT-001.zip',
        warehouseDownloadUrl: 'https://files.example/warehouse.zip',
        warehouseSha256: 'warehouse-sha',
        archiveFileName: '拣货任务-FPT-001-中文.zip',
        archiveDownloadUrl: 'https://files.example/archive.zip',
        archiveSha256: 'archive-sha',
        orderCount: 3,
        totalQuantity: 8,
        generatedTime: '2026-09-15T12:00:00'
      },
      (url, fileName) => downloads.push({ url, fileName }),
      (callback, delay) => {
        delays.push(delay)
        callback()
      }
    )

    expect(downloads).toEqual([
      { url: 'https://files.example/warehouse.zip', fileName: 'Sklad-FPT-001.zip' },
      { url: 'https://files.example/archive.zip', fileName: '拣货任务-FPT-001-中文.zip' }
    ])
    expect(delays).toEqual([350])
  })
})
