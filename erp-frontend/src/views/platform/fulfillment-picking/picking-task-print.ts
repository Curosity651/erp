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

export interface PickingTaskPrintLabels {
  title: string
  taskNo: string
  warehouse: string
  orderCount: string
  totalPieces: string
  printedAt: string
  order: (index: number) => string
  orderNo: string
  owner: string
  platform: string
  pieces: string
  sequence: string
  location: string
  internalSku: string
  productSku: string
  quantity: string
}

const DEFAULT_LABELS: PickingTaskPrintLabels = {
  title: '拣货单',
  taskNo: '拣货任务号',
  warehouse: '仓库',
  orderCount: '订单数',
  totalPieces: '总件数',
  printedAt: '打印时间',
  order: index => `订单 ${index}`,
  orderNo: '订单号',
  owner: '货主',
  platform: '平台',
  pieces: '件数',
  sequence: '序号',
  location: '取货库位',
  internalSku: '内部 SKU',
  productSku: '商品 SKU',
  quantity: '应取数量'
}

const escapeHtml = (value: unknown) =>
  String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

export function buildPickingTaskPrintHtml(
  data: PickingTaskPrintData,
  labels: PickingTaskPrintLabels = DEFAULT_LABELS
) {
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
        <div class="order-title">${escapeHtml(labels.order(orderIndex + 1))}</div>
        <div class="order-meta">
          <span>${escapeHtml(labels.orderNo)}：${escapeHtml(order.orderNo)}</span>
          <span>${escapeHtml(labels.owner)}：${escapeHtml(order.ownerName)}</span>
          <span>${escapeHtml(labels.platform)}：${escapeHtml(order.sourceType)}</span>
          <span>${escapeHtml(labels.pieces)}：${orderQuantity}</span>
        </div>
        <table>
          <thead><tr><th>${escapeHtml(labels.sequence)}</th><th>${escapeHtml(labels.location)}</th><th>${escapeHtml(labels.internalSku)}</th><th>${escapeHtml(labels.productSku)}</th><th>${escapeHtml(labels.quantity)}</th></tr></thead>
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
    <h1>${escapeHtml(labels.title)}</h1>
    <div class="summary">
      <span>${escapeHtml(labels.taskNo)}：${escapeHtml(data.taskNo)}</span>
      <span>${escapeHtml(labels.warehouse)}：${escapeHtml(data.warehouseName)}</span>
      <span>${escapeHtml(labels.orderCount)}：${escapeHtml(data.orderCount)}</span>
      <span>${escapeHtml(labels.totalPieces)}：${escapeHtml(data.totalQuantity)}</span>
      <span>${escapeHtml(labels.printedAt)}：${escapeHtml(data.printedAt)}</span>
    </div>
    ${orderSections}
    <script>window.onload=()=>window.print()</script></body></html>`
}
