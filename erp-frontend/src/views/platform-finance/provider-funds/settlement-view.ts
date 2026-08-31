export type SettlementView = 'account' | 'bill'

export function settlementViewPolicy(view: SettlementView) {
  return {
    account: view === 'account',
    bill: view === 'bill'
  }
}
