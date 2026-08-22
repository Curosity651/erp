import JsBarcode from 'jsbarcode'
import type {
  PurchaseInboundDetailVO,
  PurchaseInboundItemVO
} from '@/api/wms/purchase-inbound/types'

export interface ReceivedGoodsPrintItem {
  originalSkuCode: string
  warehouseSkuCode: string
  skuName: string
  mainImage?: string
  actualQuantity: number
}

export interface ReceivedSkuBarcodeLabelGroup {
  item: ReceivedGoodsPrintItem
  barcodeDataUrl: string
}

const escapeHtml = (value: unknown) =>
  String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')

export function aggregateReceivedGoods(
  items: Array<
    Pick<PurchaseInboundItemVO, 'skuCode' | 'warehouseSkuCode' | 'actualQuantity' | 'skuBrief'>
  >
): ReceivedGoodsPrintItem[] {
  const aggregated = new Map<string, ReceivedGoodsPrintItem>()
  items.forEach(item => {
    const actualQuantity = Number(item.actualQuantity || 0)
    if (actualQuantity <= 0) return
    const originalSkuCode = item.skuCode.trim()
    const warehouseSkuCode = item.warehouseSkuCode?.trim()
    if (!warehouseSkuCode) {
      throw new Error(`SKU ${originalSkuCode} 缺少仓库内部编码，请刷新后重试`)
    }
    const key = originalSkuCode.toUpperCase()
    const existing = aggregated.get(key)
    if (existing) {
      existing.actualQuantity += actualQuantity
      if (!existing.mainImage && item.skuBrief?.mainImage) {
        existing.mainImage = item.skuBrief.mainImage
      }
      return
    }
    aggregated.set(key, {
      originalSkuCode,
      warehouseSkuCode,
      skuName: item.skuBrief?.skuName || '-',
      mainImage: item.skuBrief?.mainImage,
      actualQuantity
    })
  })
  return Array.from(aggregated.values())
}

interface PrintableImage {
  complete: boolean
  addEventListener(type: 'load' | 'error', listener: () => void, options?: { once: boolean }): void
}

export function waitForImageElements(images: Iterable<PrintableImage>) {
  return Promise.all(Array.from(images, image => {
    if (image.complete) return Promise.resolve()
    return new Promise<void>(resolve => {
      image.addEventListener('load', resolve, { once: true })
      image.addEventListener('error', resolve, { once: true })
    })
  }))
}

async function waitForPageAssets(page: Window) {
  const fonts = page.document.fonts?.ready
  await Promise.race([
    Promise.all([
      waitForImageElements(page.document.images),
      fonts || Promise.resolve()
    ]),
    new Promise<void>(resolve => window.setTimeout(resolve, 10000))
  ])
}

function preparePage(page: Window, title: string, message: string) {
  page.document.open()
  page.document.write(`<!doctype html><html lang="zh-CN"><head><meta charset="UTF-8">
    <title>${escapeHtml(title)}</title></head>
    <body style="font-family:Arial,'Microsoft YaHei',sans-serif;padding:24px">${escapeHtml(message)}</body>
    </html>`)
  page.document.close()
}

function resolvePage(existingPage: Window | null | undefined, title: string) {
  const page = existingPage || window.open('', '_blank', 'width=920,height=760')
  if (!page) throw new Error('浏览器阻止了打印窗口，请允许弹出窗口后重试')
  preparePage(page, title, '正在生成打印内容...')
  return page
}

function assertPrintable(detail: PurchaseInboundDetailVO) {
  const goods = aggregateReceivedGoods(detail.items || [])
  if (!goods.length) throw new Error('该入库单没有实收商品，无法打印')
  return goods
}

function createCode128DataUrl(value: string) {
  const canvas = document.createElement('canvas')
  JsBarcode(canvas, value, {
    format: 'CODE128',
    displayValue: false,
    width: 2,
    height: 72,
    margin: 10,
    background: '#fff',
    lineColor: '#000'
  })
  return canvas.toDataURL('image/png')
}

export function buildReceivedSkuLabelDocument(groups: ReceivedSkuBarcodeLabelGroup[]) {
  const labels = groups.flatMap(({ item, barcodeDataUrl }) =>
    Array.from(
      { length: item.actualQuantity },
      () => `<section class="label">
        <img class="barcode" src="${escapeHtml(barcodeDataUrl)}" alt="${escapeHtml(item.warehouseSkuCode)}">
        <div class="sku-code${item.warehouseSkuCode.length > 32 ? ' compact' : ''}">${escapeHtml(item.warehouseSkuCode)}</div>
      </section>`
    )
  )

  return `<!doctype html>
    <html lang="zh-CN"><head><meta charset="UTF-8"><title>商品标签</title>
    <style>
      @page { size: 50mm 25mm; margin: 0; }
      * { box-sizing: border-box; }
      html, body { margin: 0; padding: 0; color: #000; background: #fff; }
      body { font-family: Arial, "Microsoft YaHei", sans-serif; }
      .label {
        width: 50mm; height: 25mm; padding: 1.5mm;
        display: flex; flex-direction: column; align-items: center;
        justify-content: flex-start; gap: 0.8mm;
        page-break-after: always; overflow: hidden;
      }
      .label:last-child { page-break-after: auto; }
      .barcode {
        display: block; width: auto; max-width: 47mm; height: 17mm;
        object-fit: contain; flex: 0 0 17mm;
      }
      .sku-code {
        width: 47mm; height: 3.2mm; margin: 0;
        color: #000; font-size: 7pt; line-height: 3.2mm;
        font-weight: 500; letter-spacing: 0; text-align: center;
        white-space: nowrap; overflow: hidden;
      }
      .sku-code.compact { font-size: 6pt; }
    </style></head><body>${labels.join('')}</body></html>`
}

export async function printReceivedSkuLabels(
  detail: PurchaseInboundDetailVO,
  existingPage?: Window | null
) {
  const page = resolvePage(existingPage, '商品标签')
  try {
    const goods = assertPrintable(detail)
    const labelGroups = goods.map(item => ({
        item,
        barcodeDataUrl: createCode128DataUrl(item.warehouseSkuCode)
      }))

    page.document.open()
    page.document.write(buildReceivedSkuLabelDocument(labelGroups))
    page.document.close()
    page.focus()
    await waitForPageAssets(page)
    page.print()
    return true
  } catch (error) {
    page.close()
    throw error
  }
}

export async function printReceivedGoodsReference(
  detail: PurchaseInboundDetailVO,
  existingPage?: Window | null
) {
  const page = resolvePage(existingPage, '货物对照表')
  try {
    const goods = assertPrintable(detail)
    const rows = goods
      .map(
        item => `<tr>
          <td class="image-cell">
            ${
              item.mainImage
                ? `<img src="${escapeHtml(item.mainImage)}" alt="${escapeHtml(item.skuName)}"
                    onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">`
                : ''
            }
            <span class="image-empty" style="${item.mainImage ? 'display:none' : 'display:flex'}">暂无图片</span>
          </td>
          <td class="code">${escapeHtml(item.warehouseSkuCode)}</td>
          <td class="code">${escapeHtml(item.originalSkuCode)}</td>
          <td>${escapeHtml(item.skuName)}</td>
          <td class="number">${item.actualQuantity}</td>
        </tr>`
      )
      .join('')
    const total = goods.reduce((sum, item) => sum + item.actualQuantity, 0)

    page.document.open()
    page.document.write(`<!doctype html>
      <html lang="zh-CN"><head><meta charset="UTF-8"><title>货物对照表</title>
      <style>
        @page { size: A4 portrait; margin: 12mm; }
        * { box-sizing: border-box; }
        body { margin: 0; color: #111; font: 12px/1.4 Arial, "Microsoft YaHei", sans-serif; }
        h1 { margin: 0 0 10px; text-align: center; font-size: 22px; }
        .meta { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 10px; }
        .meta span { min-width: 0; overflow-wrap: anywhere; }
        table { width: 100%; border-collapse: collapse; table-layout: fixed; }
        th, td { border: 1px solid #222; padding: 6px; text-align: left; vertical-align: middle; }
        th { background: #f2f2f2; font-weight: 700; }
        .image-cell { width: 86px; height: 86px; padding: 4px; }
        .image-cell img { display: block; width: 76px; height: 76px; object-fit: contain; margin: auto; }
        .image-empty { width: 76px; height: 76px; align-items: center; justify-content: center; color: #888; background: #f5f5f5; }
        .code { overflow-wrap: anywhere; }
        .number { width: 68px; text-align: right; font-weight: 700; }
        .summary { margin-top: 8px; text-align: right; font-weight: 700; }
      </style></head><body>
        <h1>入库货物对照表</h1>
        <div class="meta">
          <span><b>入库单：</b>${escapeHtml(detail.inboundNo)}</span>
          <span><b>货主：</b>${escapeHtml(detail.ownerName || detail.ownerCode || '-')}</span>
          <span><b>仓库：</b>${escapeHtml(detail.warehouseName || '-')}</span>
        </div>
        <table>
          <thead><tr>
            <th style="width:86px">商品图片</th>
            <th>内部 SKU</th>
            <th>原始 SKU</th>
            <th>商品名称</th>
            <th class="number">实收件数</th>
          </tr></thead>
          <tbody>${rows}</tbody>
        </table>
        <div class="summary">${goods.length} 种 SKU · 共 ${total} 件</div>
      </body></html>`)
    page.document.close()
    page.focus()
    await waitForPageAssets(page)
    page.print()
    return true
  } catch (error) {
    page.close()
    throw error
  }
}
