/**
 * 金额计算工具函数
 * 用于采购单等业务场景的金额计算
 */

/**
 * 计算行金额
 * @param quantity 数量
 * @param unitPrice 单价
 * @returns 行金额（数量 × 单价）
 */
export function calculateItemAmount(quantity: number, unitPrice: number): number {
  const qty = Number(quantity) || 0
  const price = Number(unitPrice) || 0
  // 使用 toFixed 避免浮点数精度问题，然后转回数字
  return Number((qty * price).toFixed(2))
}

/**
 * 计算总金额
 * @param items 明细项数组，每项需包含 quantity 和 unitPrice
 * @returns 总金额（所有行金额之和）
 */
export function calculateTotalAmount(
  items: Array<{ quantity?: number; unitPrice?: number }>
): number {
  if (!items || items.length === 0) return 0

  const total = items.reduce((sum, item) => {
    const itemAmount = calculateItemAmount(item.quantity || 0, item.unitPrice || 0)
    return sum + itemAmount
  }, 0)

  return Number(total.toFixed(2))
}

/**
 * 计算首付款金额
 * @param totalAmount 合同总金额
 * @param ratio 首付款比例（0-100）
 * @returns 首付款金额
 */
export function calculatePrepayAmount(totalAmount: number, ratio: number): number {
  const amount = Number(totalAmount) || 0
  const r = Number(ratio) || 0

  // 确保比例在 0-100 范围内
  const validRatio = Math.max(0, Math.min(100, r))

  return Number(((amount * validRatio) / 100).toFixed(2))
}

/**
 * 格式化金额显示
 * @param amount 金额数值
 * @param options 格式化选项
 * @returns 格式化后的金额字符串（千分位、两位小数）
 */
export function formatAmount(
  amount: number | undefined | null,
  options?: {
    locale?: string
    minimumFractionDigits?: number
    maximumFractionDigits?: number
  }
): string {
  const value = Number(amount) || 0
  const { locale = 'zh-CN', minimumFractionDigits = 2, maximumFractionDigits = 2 } = options || {}

  return value.toLocaleString(locale, {
    minimumFractionDigits,
    maximumFractionDigits
  })
}

/**
 * 解析金额字符串为数字
 * @param amountStr 金额字符串（可能包含千分位）
 * @returns 数字金额
 */
export function parseAmount(amountStr: string | number | undefined | null): number {
  if (amountStr === undefined || amountStr === null) return 0
  if (typeof amountStr === 'number') return amountStr

  // 移除千分位分隔符和空格
  const cleaned = amountStr.replace(/[,\s]/g, '')
  const parsed = parseFloat(cleaned)

  return isNaN(parsed) ? 0 : parsed
}
