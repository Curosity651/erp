import type { PageParam } from '@/api/types'

/** 区域数据传输对象 */
export interface RegionDTO {
  id?: number
  regionCode: string
  regionName: string
  status: number
  remark?: string
}

/** 区域查询对象 */
export interface RegionQO {
  regionCode?: string
  regionName?: string
  status?: number
}

/** 区域分页查询参数 */
export type RegionPageParam = RegionQO & PageParam

/** 区域分页视图对象 */
export interface RegionPageVO {
  id: number
  regionCode: string
  regionName: string
  status: number
  remark?: string
  platforms: string[]
  warehouseCount: number
  createTime: string
  updateTime: string
}

/** 区域下拉选项 */
export interface RegionOptionVO {
  id: number
  regionCode: string
  regionName: string
}

/** 平台区域映射视图对象 */
export interface PlatformRegionMappingVO {
  id: number
  platform: string
  regionId: number
}

/** 平台区域映射数据传输对象 */
export interface PlatformRegionMappingDTO {
  platform: string
}

/** 保存平台映射请求 */
export interface SavePlatformMappingDTO {
  platform: string
  regionId: number | undefined
}
