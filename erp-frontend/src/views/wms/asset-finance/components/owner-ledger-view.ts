export interface MonthRow {
  month: string
}

export function currentMonthKey(now = new Date()) {
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${now.getFullYear()}-${month}`
}

export function defaultExpandedMonths(_rows: MonthRow[], _now = new Date()) {
  return []
}
