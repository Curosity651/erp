import QRCode from 'qrcode'
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

const escapeHtml = (value: unknown) =>
  String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')

export function buildWarehouseSkuCode(ownerName: string, skuCode: string) {
  const normalizedOwner = ownerName?.trim().replaceAll(/\s+/g, '_').toUpperCase()
  const normalizedSku = skuCode?.trim()
  if (!normalizedOwner) throw new Error('货主名称未配置，无法生成仓库内部SKU')
  if (!normalizedSku) throw new Error('SKU编码不能为空')
  return `${normalizedOwner}-${normalizedSku}`
}

export function aggregateReceivedGoods(
  ownerName: string,
  items: Array<
    Pick<PurchaseInboundItemVO, 'skuCode' | 'actualQuantity' | 'skuBrief'>
  >
): ReceivedGoodsPrintItem[] {
  const aggregated = new Map<string, ReceivedGoodsPrintItem>()
  items.forEach(item => {
    const actualQuantity = Number(item.actualQuantity || 0)
    if (actualQuantity <= 0) return
    const originalSkuCode = item.skuCode.trim()
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
      warehouseSkuCode: buildWarehouseSkuCode(ownerName, originalSkuCode),
      skuName: item.skuBrief?.skuName || '-',
      mainImage: item.skuBrief?.mainImage,
      actualQuantity
    })
  })
  return Array.from(aggregated.values())
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
  const goods = aggregateReceivedGoods(detail.ownerName || '', detail.items || [])
  if (!goods.length) throw new Error('该入库单没有实收商品，无法打印')
  return goods
}

export async function printReceivedSkuLabels(
  detail: PurchaseInboundDetailVO,
  existingPage?: Window | null
) {
  const page = resolvePage(existingPage, '商品标签')
  try {
    const goods = assertPrintable(detail)
    const labelGroups = await Promise.all(
      goods.map(async item => ({
        item,
        qrCode: await QRCode.toDataURL(item.warehouseSkuCode, { margin: 1, width: 260 })
      }))
    )
    const labels = labelGroups.flatMap(({ item, qrCode }) =>
      Array.from(
        { length: item.actualQuantity },
        (_, index) => `<section class="label">
          <img class="qr" src="${qrCode}" alt="${escapeHtml(item.warehouseSkuCode)}">
          <div class="content">
            <div class="owner">${escapeHtml(detail.ownerName || detail.ownerCode || '-')}</div>
            <h1>${escapeHtml(item.warehouseSkuCode)}</h1>
            <div class="name">${escapeHtml(item.skuName)}</div>
            <div class="meta"><b>原始 SKU：</b>${escapeHtml(item.originalSkuCode)}</div>
            <div class="meta"><b>入库单：</b>${escapeHtml(detail.inboundNo)}</div>
            <div class="copy">第 ${index + 1} / ${item.actualQuantity} 件</div>
          </div>
        </section>`
      )
    )

    page.document.open()
    page.document.write(`<!doctype html>
      <html lang="zh-CN"><head><meta charset="UTF-8"><title>商品标签</title>
      <style>
        @page { size: 80mm 50mm; margin: 0; }
        * { box-sizing: border-box; }
        body { margin: 0; color: #111; font-family: Arial, "Microsoft YaHei", sans-serif; }
        .label {
          position: relative; width: 80mm; height: 50mm; padding: 3.5mm;
          display: grid; grid-template-columns: 29mm 1fr; gap: 3mm;
          align-items: center; page-break-after: always; overflow: hidden;
        }
        .label:last-child { page-break-after: auto; }
        .qr { width: 28mm; height: 28mm; }
        .content { min-width: 0; }
        .owner { font-size: 9pt; font-weight: 700; margin-bottom: 1mm; }
        h1 { margin: 0 0 1mm; font-size: 13pt; line-height: 1.12; overflow-wrap: anywhere; }
        .name { font-size: 9pt; line-height: 1.3; margin-bottom: 1mm; max-height: 8mm; overflow: hidden; }
        .meta { font-size: 7.5pt; line-height: 1.35; overflow-wrap: anywhere; }
        .copy { position: absolute; right: 3mm; bottom: 2mm; color: #666; font-size: 7pt; }
      </style></head><body>${labels.join('')}</body></html>`)
    page.document.close()
    page.focus()
    window.setTimeout(() => page.print(), 300)
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
    window.setTimeout(() => page.print(), 700)
    return true
  } catch (error) {
    page.close()
    throw error
  }
}
