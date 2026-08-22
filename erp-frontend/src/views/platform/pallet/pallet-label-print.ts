import QRCode from 'qrcode'
import type { PalletSummaryVO } from '@/api/wms/pallet'

function escapeHtml(value: unknown) {
  return String(value ?? '-')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function itemLines(pallet: PalletSummaryVO) {
  if (!pallet.items?.length) return '<div class="empty">暂无库存明细</div>'
  return pallet.items
    .map(item => `<div>${escapeHtml(item.warehouseSkuCode)} × ${escapeHtml(item.quantity)}</div>`)
    .join('')
}

export async function printPalletLabels(
  pallets: PalletSummaryVO[],
  existingPage?: Window | null
) {
  if (!pallets.length) {
    existingPage?.close()
    return false
  }

  // Must open during the click event; opening after QR generation may be blocked by the browser.
  const page = existingPage || window.open('', '_blank', 'width=760,height=680')
  if (!page) return false

  page.document.write(
    '<!doctype html><html><head><meta charset="UTF-8"><title>正在生成托盘标签</title></head>' +
      '<body style="font-family:Arial,Microsoft YaHei;padding:24px">正在生成托盘标签...</body></html>'
  )
  page.document.close()

  try {
    const labels = await Promise.all(
      pallets.map(async pallet => {
        const qr = await QRCode.toDataURL(pallet.palletNo, { margin: 1, width: 260 })
        return `<section class="label">
          <img class="qr" src="${qr}" alt="${escapeHtml(pallet.palletNo)}">
          <div class="content">
            <h1>${escapeHtml(pallet.palletNo)}</h1>
            <div class="meta"><b>货主：</b>${escapeHtml(pallet.ownerName)}</div>
            <div class="meta"><b>服务商：</b>${escapeHtml(pallet.wmsTenantName)}</div>
            <div class="meta"><b>层位：</b>${escapeHtml(pallet.slotCode)}</div>
            <div class="items">${itemLines(pallet)}</div>
          </div>
        </section>`
      })
    )

    page.document.open()
    page.document.write(`<!doctype html>
      <html>
        <head>
          <meta charset="UTF-8">
          <title>托盘标签</title>
          <style>
            @page { size: 80mm 50mm; margin: 0; }
            * { box-sizing: border-box; }
            body { margin: 0; font-family: Arial, "Microsoft YaHei", sans-serif; color: #111; }
            .label {
              width: 80mm;
              height: 50mm;
              padding: 3.5mm;
              display: grid;
              grid-template-columns: 30mm 1fr;
              gap: 3mm;
              align-items: center;
              page-break-after: always;
              overflow: hidden;
            }
            .label:last-child { page-break-after: auto; }
            .qr { width: 29mm; height: 29mm; }
            .content { min-width: 0; }
            h1 { margin: 0 0 1.5mm; font-size: 15pt; line-height: 1.1; overflow-wrap: anywhere; }
            .meta { font-size: 8.5pt; line-height: 1.4; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
            .items { margin-top: 1.5mm; font-size: 8pt; line-height: 1.35; max-height: 14mm; overflow: hidden; }
            .empty { color: #777; }
          </style>
        </head>
        <body>${labels.join('')}</body>
      </html>`)
    page.document.close()
    page.focus()
    window.setTimeout(() => page.print(), 250)
    return true
  } catch (error) {
    page.close()
    throw error
  }
}
