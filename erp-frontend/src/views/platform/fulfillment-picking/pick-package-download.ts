import type { FulfillmentPickPackage } from '@/api/wms/fulfillment/types'

export type PackageDownloadStarter = (url: string, fileName: string) => void
export type PackageDownloadScheduler = (callback: () => void, delay: number) => void

export function downloadPickPackageArchives(
  pickPackage: FulfillmentPickPackage,
  startDownload: PackageDownloadStarter = startBrowserDownload,
  schedule: PackageDownloadScheduler = (callback, delay) => window.setTimeout(callback, delay)
) {
  startDownload(pickPackage.warehouseDownloadUrl, pickPackage.warehouseFileName)
  schedule(() => {
    startDownload(pickPackage.archiveDownloadUrl, pickPackage.archiveFileName)
  }, 350)
}

function startBrowserDownload(url: string, fileName: string) {
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  anchor.target = '_blank'
  anchor.rel = 'noopener noreferrer'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
}
