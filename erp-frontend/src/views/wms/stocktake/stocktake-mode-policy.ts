import type { StocktakeMode } from '@/api/wms/stocktake/types'

export type CreatableStocktakeMode = Exclude<StocktakeMode, 'SPECIAL'>

export const creatableStocktakeModes: Array<{
  value: CreatableStocktakeMode
  label: string
  description: string
}> = [
  { value: 'FULL', label: '全仓盘点', description: '逐库位盘完整个仓库' },
  { value: 'CYCLE', label: '循环盘点', description: '选择部分库位执行' }
]
