export type RechargeTarget = 'owner' | 'platform'

const OWNER_CURRENCIES = ['RUB', 'CNY', 'USD', 'EUR']

export function defaultRechargeCurrency(target: RechargeTarget) {
  return target === 'platform' ? 'CNY' : 'RUB'
}

export function rechargeCurrencyOptions(target: RechargeTarget) {
  const currencies = target === 'platform' ? ['CNY'] : OWNER_CURRENCIES
  return currencies.map(value => ({ label: value, value }))
}
