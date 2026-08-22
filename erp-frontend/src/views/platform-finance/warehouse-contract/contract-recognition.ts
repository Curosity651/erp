export interface RecognitionFundRow {
  accountingMonth?: string
  transactionType?: string
  fundComponent?: string
  direction?: string
  status?: string
}

export interface ContractMonthOption {
  value: string
  label: string
}

function monthIndex(date: string) {
  const match = /^(\d{4})-(\d{2})/.exec(date || '')
  if (!match) return undefined
  const year = Number(match[1])
  const month = Number(match[2])
  if (month < 1 || month > 12) return undefined
  return year * 12 + month - 1
}

export function contractMonths(startDate: string, endDate: string): ContractMonthOption[] {
  const start = monthIndex(startDate)
  const end = monthIndex(endDate)
  if (start == null || end == null || end < start) return []

  const result: ContractMonthOption[] = []
  for (let index = start; index <= end; index += 1) {
    const year = Math.floor(index / 12)
    const month = (index % 12) + 1
    const monthText = String(month).padStart(2, '0')
    result.push({ value: `${year}-${monthText}`, label: `${year}年${monthText}月` })
  }
  return result
}

export function recognizedMonthValues(rows: RecognitionFundRow[]) {
  return Array.from(
    new Set(
      rows
        .filter(
          row =>
            row.transactionType === 'RECOGNITION' &&
            row.fundComponent === 'SUBSCRIPTION_SERVICE' &&
            row.direction === 'IN' &&
            row.status === 'POSTED' &&
            /^\d{4}-\d{2}$/.test(row.accountingMonth || '')
        )
        .map(row => row.accountingMonth as string)
    )
  ).sort()
}
