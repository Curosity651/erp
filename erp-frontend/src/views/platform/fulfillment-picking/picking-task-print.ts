export interface PickingTaskPrintLine {
  locationCode: string
  warehouseSkuCode: string
  skuCode: string
  plannedQuantity: number
}

export interface PickingTaskPrintOrder {
  orderNo: string
  ownerName: string
  sourceType: string
  lines: PickingTaskPrintLine[]
}

export interface PickingTaskPrintData {
  taskNo: string
  warehouseName: string
  printedAt: string
  orderCount: number
  totalQuantity: number
  orders: PickingTaskPrintOrder[]
}

const escapeHtml = (value: unknown) =>
  String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

export function buildPickingTaskPrintHtml(data: PickingTaskPrintData) {
  const orderSections = data.orders
    .map((order, orderIndex) => {
      const rows = order.lines
        .map(
          (line, lineIndex) => `<tr>
            <td>${lineIndex + 1}</td>
            <td>${escapeHtml(line.locationCode)}</td>
            <td>${escapeHtml(line.warehouseSkuCode)}</td>
            <td>${escapeHtml(line.skuCode)}</td>
            <td class="number">${escapeHtml(line.plannedQuantity)}</td>
          </tr>`
        )
        .join('')
      const orderQuantity = order.lines.reduce(
        (sum, line) => sum + Number(line.plannedQuantity || 0),
        0
      )
      return `<section class="order-block">
        <div class="order-title">订单 ${orderIndex + 1}</div>
        <div class="order-meta">
          <span>订单号：${escapeHtml(order.orderNo)}</span>
          <span>货主：${escapeHtml(order.ownerName)}</span>
          <span>平台：${escapeHtml(order.sourceType)}</span>
          <span>件数：${orderQuantity}</span>
        </div>
        <table>
          <thead><tr><th>序号</th><th>取货库位</th><th>内部 SKU</th><th>商品 SKU</th><th>应取数量</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </section>`
    })
    .join('')

  return `<!doctype html><html><head><meta charset="UTF-8"><title>${escapeHtml(data.taskNo)}</title>
    <style>
      @page{size:A4;margin:12mm}*{box-sizing:border-box}body{font-family:Arial,"Microsoft YaHei",sans-serif;color:#111;margin:0;font-size:12px}
      h1{font-size:20px;text-align:center;margin:0 0 14px}.summary{display:grid;grid-template-columns:1fr 1fr;gap:7px 24px;border:1px solid #222;padding:10px 12px;margin-bottom:14px}
      .order-block{break-inside:avoid;margin-bottom:16px}.order-title{font-size:14px;font-weight:700;border-left:4px solid #111;padding-left:8px;margin-bottom:7px}
      .order-meta{display:grid;grid-template-columns:2fr 1fr 1fr 1fr;gap:8px;border:1px solid #333;border-bottom:0;padding:7px 8px}
      table{width:100%;border-collapse:collapse;table-layout:fixed}th,td{border:1px solid #333;padding:7px 6px;text-align:left;overflow-wrap:anywhere}th{background:#f2f2f2}.number{text-align:right}
      th:nth-child(1),td:nth-child(1){width:8%}th:nth-child(2),td:nth-child(2){width:18%}th:nth-child(5),td:nth-child(5){width:14%}
    </style></head><body>
    <h1>拣货单</h1>
    <div class="summary">
      <span>拣货任务号：${escapeHtml(data.taskNo)}</span>
      <span>仓库：${escapeHtml(data.warehouseName)}</span>
      <span>订单数：${escapeHtml(data.orderCount)}</span>
      <span>总件数：${escapeHtml(data.totalQuantity)}</span>
      <span>打印时间：${escapeHtml(data.printedAt)}</span>
    </div>
    ${orderSections}
    <script>window.onload=()=>window.print()</script></body></html>`
}
