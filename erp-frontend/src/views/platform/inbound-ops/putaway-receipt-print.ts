import type { PutawayReceiptLineVO } from '@/api/wms/inbound-execution'
import type { PurchaseInboundPageVO } from '@/api/wms/purchase-inbound/types'

function escapeHtml(value: unknown) {
  return String(value ?? '-')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function qualityText(value: string) {
  return value === 'DAMAGED' ? '残次品' : '良品'
}

export function printPutawayReceipt(
  record: PurchaseInboundPageVO,
  lines: PutawayReceiptLineVO[]
) {
  if (!lines.length) return false
  const page = window.open('', '_blank', 'width=960,height=760')
  if (!page) return false

  const total = lines.reduce((sum, line) => sum + Number(line.quantity || 0), 0)
  const skuCount = new Set(lines.map(line => line.skuCode)).size
  const locationCount = new Set(lines.map(line => line.locationId || line.locationCode || line.slotCode)).size
  const rows = lines
    .map(
      (line, index) => `<tr>
        <td>${index + 1}</td>
        <td>${escapeHtml(line.locationCode || line.slotCode)}</td>
        <td>${escapeHtml(line.warehouseSkuCode || line.skuCode)}</td>
        <td>${qualityText(line.quality)}</td>
        <td class="number">${escapeHtml(line.quantity)}</td>
        <td>${escapeHtml(line.overrideReason)}</td>
      </tr>`
    )
    .join('')

  page.document.write(`<!doctype html>
    <html>
      <head>
        <meta charset="UTF-8">
        <title>上架单-${escapeHtml(record.inboundNo)}</title>
        <style>
          @page { size: A4; margin: 14mm; }
          * { box-sizing: border-box; }
          body { margin: 0; color: #111; font: 12px/1.5 Arial, "Microsoft YaHei", sans-serif; }
          h1 { margin: 0 0 18px; text-align: center; font-size: 22px; }
          .meta { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 28px; margin-bottom: 16px; }
          .meta span { border-bottom: 1px solid #bbb; padding: 3px 0; }
          table { width: 100%; border-collapse: collapse; }
          th, td { border: 1px solid #555; padding: 7px 8px; text-align: left; }
          th { background: #f3f4f6; }
          .number { text-align: right; }
          .summary { margin-top: 12px; text-align: right; font-weight: 700; }
          .signatures { display: grid; grid-template-columns: 1fr 1fr; gap: 60px; margin-top: 42px; }
          .signature { border-bottom: 1px solid #555; padding-bottom: 5px; }
        </style>
      </head>
      <body>
        <h1>入库上架单</h1>
        <div class="meta">
          <span><b>入库单号：</b>${escapeHtml(record.inboundNo)}</span>
          <span><b>上架时间：</b>${escapeHtml(record.putawayTime || new Date().toLocaleString())}</span>
          <span><b>仓库：</b>${escapeHtml(record.warehouseName)}</span>
          <span><b>操作员：</b>${escapeHtml(record.putawayByName)}</span>
          <span><b>货主：</b>${escapeHtml(record.ownerName)}</span>
          <span><b>服务商：</b>${escapeHtml(record.operatorName)}</span>
        </div>
        <table>
          <thead>
            <tr><th>序号</th><th>逻辑库位</th><th>内部 SKU</th><th>品质</th><th>数量</th><th>现场说明</th></tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
        <div class="summary">合计：${skuCount} 种 SKU · ${locationCount} 个库位 · ${total} 件</div>
        <div class="signatures">
          <div class="signature">上架人签字：</div>
          <div class="signature">复核人签字：</div>
        </div>
      </body>
    </html>`)
  page.document.close()
  page.focus()
  window.setTimeout(() => page.print(), 200)
  return true
}
