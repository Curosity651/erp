const LOCALE_BY_CURRENCY: Record<string, string> = {
  CNY: 'zh-CN',
  RUB: 'ru-RU',
  USD: 'en-US'
}

export function formatCurrency(value: number | string | undefined, currency?: string) {
  const amount = Number(value ?? 0)
  const code = (currency || 'UNKNOWN').trim().toUpperCase()
  if (!LOCALE_BY_CURRENCY[code]) {
    return `${amount.toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })} ${code}`
  }
  return new Intl.NumberFormat(LOCALE_BY_CURRENCY[code], {
    style: 'currency',
    currency: code,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}
