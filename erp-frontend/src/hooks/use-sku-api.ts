import { ref, reactive } from 'vue'
import type { PageParam } from '@/api/types'
import type {
  SkuDTO,
  SkuPageParam,
  SkuPageVO,
  SkuDetailVO,
  SkuSearchParam,
  SkuFilterOptions,
  SkuOverviewStats
} from '@/api/product/sku/types'
import * as skuApi from '@/api/product/sku'
import { initSkuInterceptors } from '@/utils/axios/sku-interceptor-init'

/**
 * SKU API操作的组合式函数
 */
export function useSkuApi() {
  // 初始化SKU拦截器
  initSkuInterceptors()

  // 加载状态
  const loading = ref(false)
  const submitting = ref(false)
  const validating = ref(false)

  // 数据状态
  const skuList = ref<SkuPageVO[]>([])
  const skuDetail = ref<SkuDetailVO | null>(null)
  const filterOptions = ref<SkuFilterOptions | null>(null)
  const overviewStats = ref<SkuOverviewStats | null>(null)

  // 分页信息
  const pagination = reactive({
    current: 1,
    size: 20,
    total: 0
  })

  // ==================== 基础CRUD操作 ====================

  /**
   * 分页查询SKU
   */
  const querySkuPage = async (pageParam: PageParam, searchParam?: SkuSearchParam) => {
    loading.value = true
    try {
      const params: SkuPageParam = { ...pageParam, ...searchParam }
      const response = await skuApi.pageSku(params)

      if (response.data) {
        skuList.value = response.data.records
        pagination.total = response.data.total
        pagination.current = pageParam.current || 1
        pagination.size = pageParam.size || 20
      }

      return response.data
    } catch (error) {
      console.error('查询SKU列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取SKU详情
   */
  const getSkuDetail = async (id: number) => {
    loading.value = true
    try {
      const response = await skuApi.getSkuDetail(id)
      skuDetail.value = response.data
      return response.data
    } catch (error) {
      console.error('获取SKU详情失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建SKU
   */
  const createSku = async (dto: SkuDTO) => {
    submitting.value = true
    try {
      const response = await skuApi.createSku(dto)
      return response
    } catch (error) {
      console.error('创建SKU失败:', error)
      throw error
    } finally {
      submitting.value = false
    }
  }

  /**
   * 更新SKU
   */
  const updateSku = async (dto: SkuDTO) => {
    submitting.value = true
    try {
      const response = await skuApi.updateSku(dto)
      return response
    } catch (error) {
      console.error('更新SKU失败:', error)
      throw error
    } finally {
      submitting.value = false
    }
  }

  /**
   * 删除SKU
   */
  const deleteSku = async (id: number) => {
    submitting.value = true
    try {
      const response = await skuApi.deleteSku(id)
      return response
    } catch (error) {
      console.error('删除SKU失败:', error)
      throw error
    } finally {
      submitting.value = false
    }
  }

  // ==================== 业务相关操作 ====================

  /**
   * 验证SKU编码唯一性
   */
  const validateSkuCode = async (code: string, excludeId?: number) => {
    if (!code.trim()) {
      return { isValid: false, message: 'SKU编码不能为空' }
    }

    validating.value = true
    try {
      const response = await skuApi.validateSkuCode(code, excludeId)
      const isValid = response.data === true

      return {
        isValid,
        message: isValid ? '' : 'SKU编码已存在'
      }
    } catch (error) {
      console.error('验证SKU编码失败:', error)
      return { isValid: false, message: '验证失败，请稍后重试' }
    } finally {
      validating.value = false
    }
  }

  /**
   * 验证SKU序号唯一性
   */
  const validateSkuNo = async (skuNo: number, excludeId?: number) => {
    if (!skuNo || skuNo <= 0) {
      return { isValid: false, message: 'SKU序号不能为空或小于等于0' }
    }

    validating.value = true
    try {
      const response = await skuApi.validateSkuNo(skuNo, excludeId)
      const isValid = response.data === true

      return {
        isValid,
        message: isValid ? '' : 'SKU序号已存在'
      }
    } catch (error) {
      console.error('验证SKU序号失败:', error)
      return { isValid: false, message: '验证失败，请稍后重试' }
    } finally {
      validating.value = false
    }
  }

  /**
   * 根据编码获取SKU
   */
  const getSkuByCode = async (code: string) => {
    loading.value = true
    try {
      const response = await skuApi.getSkuByCode(code)
      return response.data
    } catch (error) {
      console.error('根据编码获取SKU失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // ==================== 高级查询操作 ====================

  /**
   * 高级搜索
   */
  const advancedSearch = async (pageParam: PageParam, searchParam: SkuSearchParam) => {
    loading.value = true
    try {
      const response = await skuApi.advancedSearchSku(pageParam, searchParam)

      if (response.data) {
        skuList.value = response.data.records
        pagination.total = response.data.total
        pagination.current = pageParam.current || 1
        pagination.size = pageParam.size || 20
      }

      return response.data
    } catch (error) {
      console.error('高级搜索失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 关键字搜索
   */
  const searchByKeyword = async (pageParam: PageParam, keyword: string) => {
    loading.value = true
    try {
      const response = await skuApi.searchSkuByKeyword(pageParam, keyword)

      if (response.data) {
        skuList.value = response.data.records
        pagination.total = response.data.total
        pagination.current = pageParam.current || 1
        pagination.size = pageParam.size || 20
      }

      return response.data
    } catch (error) {
      console.error('关键字搜索失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 智能搜索
   */
  const smartSearch = async (pageParam: PageParam, searchText: string) => {
    loading.value = true
    try {
      const response = await skuApi.smartSearchSku(pageParam, searchText)

      if (response.data) {
        skuList.value = response.data.records
        pagination.total = response.data.total
        pagination.current = pageParam.current || 1
        pagination.size = pageParam.size || 20
      }

      return response.data
    } catch (error) {
      console.error('智能搜索失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // ==================== 筛选选项操作 ====================

  /**
   * 获取筛选选项
   */
  const getFilterOptions = async () => {
    loading.value = true
    try {
      const response = await skuApi.getSkuFilterOptions()
      filterOptions.value = response.data
      return response.data
    } catch (error) {
      console.error('获取筛选选项失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取品牌列表
   */
  const getBrands = async () => {
    try {
      const response = await skuApi.getDistinctBrands()
      return response.data
    } catch (error) {
      console.error('获取品牌列表失败:', error)
      throw error
    }
  }

  /**
   * 获取项目组列表
   */
  const getProjectGroups = async () => {
    try {
      const response = await skuApi.getDistinctProjectGroups()
      return response.data
    } catch (error) {
      console.error('获取项目组列表失败:', error)
      throw error
    }
  }

  /**
   * 获取供应商编码列表
   */
  const getSupplierCodes = async () => {
    try {
      const response = await skuApi.getDistinctSupplierCodes()
      return response.data
    } catch (error) {
      console.error('获取供应商编码列表失败:', error)
      throw error
    }
  }

  /**
   * 获取销售国家列表
   */
  const getSalesCountries = async () => {
    try {
      const response = await skuApi.getDistinctSalesCountries()
      return response.data
    } catch (error) {
      console.error('获取销售国家列表失败:', error)
      throw error
    }
  }

  // ==================== 统计分析操作 ====================

  /**
   * 获取概览统计
   */
  const getOverviewStats = async () => {
    loading.value = true
    try {
      const response = await skuApi.getSkuOverviewStats()
      overviewStats.value = response.data
      return response.data
    } catch (error) {
      console.error('获取概览统计失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取状态统计
   */
  const getStatusStats = async () => {
    try {
      const response = await skuApi.getSkuCountByStatus()
      return response.data
    } catch (error) {
      console.error('获取状态统计失败:', error)
      throw error
    }
  }

  /**
   * 获取品牌统计
   */
  const getBrandStats = async () => {
    try {
      const response = await skuApi.getSkuCountByBrand()
      return response.data
    } catch (error) {
      console.error('获取品牌统计失败:', error)
      throw error
    }
  }

  /**
   * 获取供应商统计
   */
  const getSupplierStats = async () => {
    try {
      const response = await skuApi.getSkuCountBySupplier()
      return response.data
    } catch (error) {
      console.error('获取供应商统计失败:', error)
      throw error
    }
  }

  // ==================== 批量操作 ====================

  /**
   * 批量更新产品状态
   */
  const batchUpdateStatus = async (skuIds: number[], productStatus: number) => {
    submitting.value = true
    try {
      const response = await skuApi.batchUpdateProductStatus(skuIds, productStatus)
      return response.data
    } catch (error) {
      console.error('批量更新状态失败:', error)
      throw error
    } finally {
      submitting.value = false
    }
  }

  /**
   * 导出SKU数据
   */
  const exportSkus = async (searchParam?: SkuSearchParam) => {
    loading.value = true
    try {
      const response = await skuApi.exportSkus(searchParam)
      return response.data
    } catch (error) {
      console.error('导出SKU数据失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // ==================== 工具方法 ====================

  /**
   * 重置数据状态
   */
  const resetData = () => {
    skuList.value = []
    skuDetail.value = null
    filterOptions.value = null
    overviewStats.value = null
    pagination.current = 1
    pagination.size = 20
    pagination.total = 0
  }

  /**
   * 刷新当前页数据
   */
  const refreshCurrentPage = async (searchParam?: SkuSearchParam) => {
    const pageParam = {
      current: pagination.current,
      size: pagination.size
    }
    return await querySkuPage(pageParam, searchParam)
  }

  return {
    // 状态
    loading,
    submitting,
    validating,
    skuList,
    skuDetail,
    filterOptions,
    overviewStats,
    pagination,

    // 基础CRUD
    querySkuPage,
    getSkuDetail,
    createSku,
    updateSku,
    deleteSku,

    // 业务相关
    validateSkuCode,
    validateSkuNo,
    getSkuByCode,

    // 高级查询
    advancedSearch,
    searchByKeyword,
    smartSearch,

    // 筛选选项
    getFilterOptions,
    getBrands,
    getProjectGroups,
    getSupplierCodes,
    getSalesCountries,

    // 统计分析
    getOverviewStats,
    getStatusStats,
    getBrandStats,
    getSupplierStats,

    // 批量操作
    batchUpdateStatus,
    exportSkus,

    // 工具方法
    resetData,
    refreshCurrentPage
  }
}
