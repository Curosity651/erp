import QRCode from 'qrcode'
import type { PurchaseInboundDetailVO } from '@/api/wms/purchase-inbound/types'

const escapeHtml = (value: unknown) =>
  String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')

export async function printPurchaseInbound(
  detail: PurchaseInboundDetailVO,
  printPopup?: Window | null
) {
  const qrCode = await QRCode.toDataURL(detail.inboundNo, { margin: 1, width: 180 })
  const rows = (detail.items || [])
    .map(
      item => `<tr>
        <td>${escapeHtml(item.skuCode)}</td>
        <td>${escapeHtml(item.skuBrief?.skuName || '-')}</td>
        <td>${escapeHtml(item.purchaseOrderNo || '-')}</td>
        <td class="number">${item.expectedQuantity ?? 0}</td>
      </tr>`
    )
    .join('')
  const total = (detail.items || []).reduce((sum, item) => sum + (item.expectedQuantity || 0), 0)
  const skuCount = new Set((detail.items || []).map(item => item.skuCode)).size
  const popup = printPopup || window.open('', '_blank', 'width=900,height=760')
  if (!popup) throw new Error('浏览器阻止了打印窗口，请允许弹出窗口后重试')

  popup.document.write(`<!doctype html>
  <html lang="zh-CN"><head><meta charset="UTF-8"><title>${escapeHtml(detail.inboundNo)}</title>
  <style>
    @page { size: A4; margin: 14mm; }
    body { margin: 0; color: #111; font: 14px/1.5 Arial, "Microsoft YaHei", sans-serif; }
    h1 { margin: 0 0 14px; text-align: center; font-size: 24px; letter-spacing: 0; }
    .head { display: grid; grid-template-columns: 1fr 190px; gap: 20px; align-items: start; }
    .meta { display: grid; grid-template-columns: 110px 1fr; border: 1px solid #222; }
    .meta div { padding: 7px 9px; border-bottom: 1px solid #bbb; }
    .meta div:nth-last-child(-n+2) { border-bottom: 0; }
    .label, th { background: #f3f3f3; font-weight: 600; }
    .qr { text-align: center; font-weight: 700; overflow-wrap: anywhere; }
    .qr img { display: block; width: 170px; height: 170px; margin: 0 auto 4px; }
    table { width: 100%; margin-top: 16px; border-collapse: collapse; table-layout: fixed; }
    th, td { padding: 7px 8px; border: 1px solid #222; text-align: left; overflow-wrap: anywhere; }
    .number { width: 90px; text-align: right; }
    .summary { margin-top: 10px; text-align: right; font-weight: 700; }
    .footer { margin-top: 34px; display: flex; justify-content: space-between; }
  </style></head><body>
    <h1>采购入库单</h1>
    <div class="head">
      <div class="meta">
        <div class="label">完整入库单号</div><div>${escapeHtml(detail.inboundNo)}</div>
        <div class="label">货主</div><div>${escapeHtml(detail.ownerName || detail.ownerCode || '-')}</div>
        <div class="label">入库仓库</div><div>${escapeHtml(detail.warehouseName || '-')}</div>
        <div class="label">关联物流单</div><div>${escapeHtml(detail.shippingOrderNo || '-')}</div>
        <div class="label">入库日期</div><div>${escapeHtml(detail.inboundDate || '-')}</div>
        <div class="label">创建时间</div><div>${escapeHtml(detail.createTime || '-')}</div>
      </div>
      <div class="qr"><img src="${qrCode}" alt="入库单二维码">${escapeHtml(detail.inboundNo)}</div>
    </div>
    <table>
      <thead><tr><th>SKU编码</th><th>商品名称</th><th>采购单号</th><th class="number">应收数量</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <div class="summary">${detail.items.length} 条明细 · ${skuCount} 种 SKU · ${total} 件</div>
    <div class="footer"><span>发货交接：____________</span><span>仓库收货：____________</span></div>
  </body></html>`)
  popup.document.close()
  popup.focus()
  window.setTimeout(() => popup.print(), 250)
}
