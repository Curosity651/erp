<template>
  <div class="sku-search-container">
    <!-- 快速搜索区域 -->
    <a-card class="quick-search-card" :bordered="false">
      <div class="search-header">
        <div class="search-title">
          <search-outlined class="search-icon" />
          <span>{{ t('product.sku.search.title') }}</span>
        </div>
        <div class="search-actions-inline">
          <a-button type="primary" :loading="props.loading" @click="search">
            <template #icon><search-outlined /></template>
            {{ t('action.search') }}
          </a-button>
          <a-button @click="reset">
            <template #icon><reload-outlined /></template>
            {{ t('action.reset') }}
          </a-button>
          <a-button type="link" @click="toggleAdvanced">
            <template #icon>
              <down-outlined v-if="!showAdvanced" />
              <up-outlined v-else />
            </template>
            {{ showAdvanced ? t('product.sku.search.collapse') : t('product.sku.search.advanced') }}
          </a-button>
        </div>
      </div>

      <a-form :model="formModel" class="quick-search-form">
        <!-- 核心搜索字段 - 使用Ant Design栅格系统 -->
        <a-row :gutter="16">
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">{{ t('product.sku.search.skuCode') }}</label>
              <a-input
                v-model:value="formModel.skuCode"
                :placeholder="t('product.sku.search.skuCodePlaceholder')"
                allow-clear
              />
            </div>
          </a-col>
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">{{ t('product.sku.search.productName') }}</label>
              <a-input
                v-model:value="formModel.skuName"
                :placeholder="t('product.sku.search.productNamePlaceholder')"
                allow-clear
              />
            </div>
          </a-col>
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">{{ t('product.sku.search.productStatus') }}</label>
              <dict-select
                v-model:value="formModel.productStatus"
                dict-code="product_status"
                :placeholder="t('product.sku.search.statusPlaceholder')"
                allow-clear
              />
            </div>
          </a-col>
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">{{ t('common.createTime') }}</label>
              <a-range-picker
                v-model:value="formModel.createTimeRange"
                format="YYYY-MM-DD"
                :placeholder="[t('product.sku.search.startDate'), t('product.sku.search.endDate')]"
              />
            </div>
          </a-col>
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">{{ t('product.sku.search.skuNo') }}</label>
              <a-input-number
                v-model:value="formModel.skuNo"
                :placeholder="t('product.sku.search.skuNoPlaceholder')"
                style="width: 100%"
              />
            </div>
          </a-col>
          <a-col v-bind="colConfig">
            <div class="search-group">
              <label class="search-label">{{ t('product.sku.search.spuCode') }}</label>
              <a-input
                v-model:value="formModel.spuCode"
                :placeholder="t('product.sku.search.spuCodePlaceholder')"
                allow-clear
              />
            </div>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 高级筛选区域 - 使用Ant Design栅格系统 -->
    <a-card v-if="showAdvanced" class="advanced-filter-card" :bordered="false">
      <!-- 业务相关筛选 -->
      <a-row :gutter="16">
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <filter-outlined class="label-icon" />
              {{ t('product.sku.search.category') }}
            </label>
            <category-tree-select
              v-model:value="formModel.categoryId"
              :placeholder="t('product.sku.search.categoryPlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <filter-outlined class="label-icon" />
              {{ t('product.sku.search.brand') }}
            </label>
            <brand-select
              v-model:value="formModel.brandCode"
              :placeholder="t('product.sku.search.brandPlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <filter-outlined class="label-icon" />
              {{ t('product.sku.salesCountry') }}
            </label>
            <country-select
              v-model:value="formModel.salesCountry"
              :placeholder="t('product.sku.search.countryPlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <filter-outlined class="label-icon" />
              {{ t('product.sku.search.projectGroup') }}
            </label>
            <project-group-select
              v-model:value="formModel.projectGroupCode"
              :placeholder="t('product.sku.search.projectGroupPlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <filter-outlined class="label-icon" />
              {{ t('product.sku.supplier') }}
            </label>
            <supplier-select
              v-model:value="formModel.supplierCode"
              :placeholder="t('product.sku.search.supplierPlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <setting-outlined class="label-icon" />
              {{ t('product.sku.search.shippingType') }}
            </label>
            <dict-select
              v-model:value="formModel.shippingType"
              dict-code="shipping_type"
              :placeholder="t('product.sku.search.shippingTypePlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
      </a-row>

      <!-- 物流属性筛选 -->
      <a-row :gutter="16" style="margin-top: 16px">
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <setting-outlined class="label-icon" />
              {{ t('product.sku.packageType') }}
            </label>
            <dict-select
              v-model:value="formModel.packageType"
              dict-code="package_type"
              :placeholder="t('product.sku.search.packageTypePlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <setting-outlined class="label-icon" />
              {{ t('product.sku.search.billingWeightType') }}
            </label>
            <dict-select
              v-model:value="formModel.billingWeightType"
              dict-code="billing_weight_type"
              :placeholder="t('product.sku.search.billingWeightPlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <appstore-outlined class="label-icon" />
              {{ t('product.sku.surfaceColor') }}
            </label>
            <a-input
              v-model:value="formModel.surfaceColor"
              :placeholder="t('product.sku.search.surfaceColorPlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <appstore-outlined class="label-icon" />
              {{ t('product.sku.search.frameColor') }}
            </label>
            <a-input
              v-model:value="formModel.frameColor"
              :placeholder="t('product.sku.search.frameColorPlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <appstore-outlined class="label-icon" />
              {{ t('product.sku.search.material') }}
            </label>
            <a-input
              v-model:value="formModel.material"
              :placeholder="t('product.sku.search.materialPlaceholder')"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <team-outlined class="label-icon" />
              {{ t('product.sku.roleDeveloper') }}
            </label>
            <user-select
              v-model:value="formModel.developerId"
              :placeholder="t('product.sku.search.developerPlaceholder')"
              :options="allUsers"
              :loading="usersLoading"
              allow-clear
            />
          </div>
        </a-col>
      </a-row>

      <!-- 团队人员和产品特性筛选 -->
      <a-row :gutter="16" style="margin-top: 16px">
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <team-outlined class="label-icon" />
              {{ t('product.sku.roleOperator') }}
            </label>
            <user-select
              v-model:value="formModel.operatorId"
              :placeholder="t('product.sku.search.operatorPlaceholder')"
              :options="allUsers"
              :loading="usersLoading"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <team-outlined class="label-icon" />
              {{ t('product.sku.roleQc') }}
            </label>
            <user-select
              v-model:value="formModel.qualityInspectorId"
              :placeholder="t('product.sku.search.qcPlaceholder')"
              :options="allUsers"
              :loading="usersLoading"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="colConfig">
          <div class="filter-group">
            <label class="filter-label">
              <team-outlined class="label-icon" />
              {{ t('product.sku.rolePurchaser') }}
            </label>
            <user-select
              v-model:value="formModel.purchaserId"
              :placeholder="t('product.sku.search.purchaserPlaceholder')"
              :options="allUsers"
              :loading="usersLoading"
              allow-clear
            />
          </div>
        </a-col>
        <a-col v-bind="featuresColConfig">
          <div class="filter-group">
            <label class="features-label">
              <span>
                <appstore-outlined class="label-icon" />
                {{ t('product.sku.search.features') }}
              </span>
              <a-button size="small" type="link" class="inline-clear-btn" @click="clearAllFeatures">
                <close-outlined />
              </a-button>
            </label>
            <div class="features-horizontal-container">
              <div class="feature-horizontal-item">
                <span class="feature-name-label">
                  <span class="feature-icon power">⚡</span>
                  {{ t('product.sku.search.powerStrip') }}
                </span>
                <div class="feature-state-controls">
                  <a-tooltip :title="t('product.sku.search.unrestricted')">
                    <a-button
                      size="small"
                      :type="formModel.needsPower === undefined ? 'primary' : 'default'"
                      class="mini-state-btn"
                      @click="handleFeatureStateChange('needsPower', undefined)"
                    >
                      —
                    </a-button>
                  </a-tooltip>
                  <a-tooltip :title="t('product.sku.search.mustInclude')">
                    <a-button
                      size="small"
                      :type="formModel.needsPower === 1 ? 'primary' : 'default'"
                      class="mini-state-btn include"
                      @click="handleFeatureStateChange('needsPower', 1)"
                    >
                      ✓
                    </a-button>
                  </a-tooltip>
                  <a-tooltip :title="t('product.sku.search.mustExclude')">
                    <a-button
                      size="small"
                      :type="formModel.needsPower === 0 ? 'primary' : 'default'"
                      class="mini-state-btn exclude"
                      @click="handleFeatureStateChange('needsPower', 0)"
                    >
                      ✕
                    </a-button>
                  </a-tooltip>
                </div>
              </div>
              <div class="feature-horizontal-item">
                <span class="feature-name-label">
                  <span class="feature-icon seasonal">🌟</span>
                  {{ t('product.sku.featureSeasonal') }}
                </span>
                <div class="feature-state-controls">
                  <a-tooltip :title="t('product.sku.search.unrestricted')">
                    <a-button
                      size="small"
                      :type="formModel.seasonal === undefined ? 'primary' : 'default'"
                      class="mini-state-btn"
                      @click="handleFeatureStateChange('seasonal', undefined)"
                    >
                      —
                    </a-button>
                  </a-tooltip>
                  <a-tooltip :title="t('product.sku.search.mustInclude')">
                    <a-button
                      size="small"
                      :type="formModel.seasonal === 1 ? 'primary' : 'default'"
                      class="mini-state-btn include"
                      @click="handleFeatureStateChange('seasonal', 1)"
                    >
                      ✓
                    </a-button>
                  </a-tooltip>
                  <a-tooltip :title="t('product.sku.search.mustExclude')">
                    <a-button
                      size="small"
                      :type="formModel.seasonal === 0 ? 'primary' : 'default'"
                      class="mini-state-btn exclude"
                      @click="handleFeatureStateChange('seasonal', 0)"
                    >
                      ✕
                    </a-button>
                  </a-tooltip>
                </div>
              </div>
              <div class="feature-horizontal-item">
                <span class="feature-name-label">
                  <span class="feature-icon rgb">🌈</span>
                  RGB
                </span>
                <div class="feature-state-controls">
                  <a-tooltip :title="t('product.sku.search.unrestricted')">
                    <a-button
                      size="small"
                      :type="formModel.hasRgbLight === undefined ? 'primary' : 'default'"
                      class="mini-state-btn"
                      @click="handleFeatureStateChange('hasRgbLight', undefined)"
                    >
                      —
                    </a-button>
                  </a-tooltip>
                  <a-tooltip :title="t('product.sku.search.mustInclude')">
                    <a-button
                      size="small"
                      :type="formModel.hasRgbLight === 1 ? 'primary' : 'default'"
                      class="mini-state-btn include"
                      @click="handleFeatureStateChange('hasRgbLight', 1)"
                    >
                      ✓
                    </a-button>
                  </a-tooltip>
                  <a-tooltip :title="t('product.sku.search.mustExclude')">
                    <a-button
                      size="small"
                      :type="formModel.hasRgbLight === 0 ? 'primary' : 'default'"
                      class="mini-state-btn exclude"
                      @click="handleFeatureStateChange('hasRgbLight', 0)"
                    >
                      ✕
                    </a-button>
                  </a-tooltip>
                </div>
              </div>
              <div class="feature-horizontal-item">
                <span class="feature-name-label">
                  <span class="feature-icon glass">🔍</span>
                  {{ t('product.sku.search.glass') }}
                </span>
                <div class="feature-state-controls">
                  <a-tooltip :title="t('product.sku.search.unrestricted')">
                    <a-button
                      size="small"
                      :type="formModel.hasGlass === undefined ? 'primary' : 'default'"
                      class="mini-state-btn"
                      @click="handleFeatureStateChange('hasGlass', undefined)"
                    >
                      —
                    </a-button>
                  </a-tooltip>
                  <a-tooltip :title="t('product.sku.search.mustInclude')">
                    <a-button
                      size="small"
                      :type="formModel.hasGlass === 1 ? 'primary' : 'default'"
                      class="mini-state-btn include"
                      @click="handleFeatureStateChange('hasGlass', 1)"
                    >
                      ✓
                    </a-button>
                  </a-tooltip>
                  <a-tooltip :title="t('product.sku.search.mustExclude')">
                    <a-button
                      size="small"
                      :type="formModel.hasGlass === 0 ? 'primary' : 'default'"
                      class="mini-state-btn exclude"
                      @click="handleFeatureStateChange('hasGlass', 0)"
                    >
                      ✕
                    </a-button>
                  </a-tooltip>
                </div>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { Form } from 'ant-design-vue'
import CategoryTreeSelect from '@/components/Lov/CategoryTreeSelect.vue'
import UserSelect from '@/components/Lov/UserSelect.vue'
import BrandSelect from '@/components/Lov/BrandSelect.vue'
import SupplierSelect from '@/components/Lov/SupplierSelect.vue'
import ProjectGroupSelect from '@/components/Lov/ProjectGroupSelect.vue'
import CountrySelect from '@/components/Lov/CountrySelect.vue'
import DictSelect from '@/components/Dict/group/DictSelect.vue'
import { useUserData } from '@/hooks/use-user-data'
import type { SkuQO } from '@/api/product/sku/types'
import type { Dayjs } from 'dayjs'
import { useI18n } from 'vue-i18n'
import {
  SearchOutlined,
  ReloadOutlined,
  DownOutlined,
  UpOutlined,
  FilterOutlined,
  SettingOutlined,
  AppstoreOutlined,
  TeamOutlined,
  CloseOutlined
} from '@ant-design/icons-vue'

const useForm = Form.useForm
const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  { loading: false }
)

const emits = defineEmits<{
  (e: 'search', params: Record<string, any>): void
}>()

// 高级搜索展开状态
const showAdvanced = ref(false)

// 统一的响应式栅格配置
const colConfig = {
  xs: 24,
  sm: 12,
  md: 8,
  lg: 8,
  xl: 6,
  xxl: 4
}

// 产品特性区域的响应式配置 - 固定占用合理空间
const featuresColConfig = {
  xs: 24, // 超小屏：占满整行
  sm: 24, // 小屏及以上：占用8列（1/3行）
  md: 24, // 中屏：占用8列
  lg: 24, // 大屏：占用8列
  xl: 12, // 超大屏：占用8列
  xxl: 12 // 超超大屏：占用8列
}

// 搜索表单模型
interface SearchFormModel extends SkuQO {
  skuName?: string
  createTimeRange?: [Dayjs, Dayjs]
  [key: string]: any // 添加索引签名以支持动态属性访问
}

const formModel = reactive<SearchFormModel>({
  skuCode: undefined,
  skuNo: undefined,
  spuCode: undefined,
  salesCountry: undefined,
  categoryId: undefined,
  productStatus: undefined,
  shippingType: undefined,
  brandCode: undefined,
  projectGroupCode: undefined,
  chineseName: undefined,
  russianName: undefined,
  supplierCode: undefined,
  createTimeStart: undefined,
  createTimeEnd: undefined,
  packageType: undefined,
  surfaceColor: undefined,
  frameColor: undefined,
  material: undefined,
  needsPower: undefined,
  seasonal: undefined,
  hasRgbLight: undefined,
  hasGlass: undefined,
  billingWeightType: undefined,
  developerId: undefined,
  operatorId: undefined,
  qualityInspectorId: undefined,
  purchaserId: undefined,
  // 辅助字段
  skuName: undefined,
  createTimeRange: undefined
})

const { resetFields } = useForm(formModel)

// 用户数据
const { allUsers, loading: usersLoading, loadAllUsers } = useUserData()

// 切换高级搜索
const toggleAdvanced = () => {
  showAdvanced.value = !showAdvanced.value
}

// 搜索
const search = () => {
  const searchParams = { ...toRaw(formModel) }

  // 处理创建时间范围
  if (searchParams.createTimeRange && searchParams.createTimeRange.length === 2) {
    searchParams.createTimeStart = searchParams.createTimeRange[0].format('YYYY-MM-DD HH:mm:ss')
    searchParams.createTimeEnd = searchParams.createTimeRange[1].format('YYYY-MM-DD HH:mm:ss')
  }
  delete searchParams.createTimeRange

  // 产品名称模糊搜索：后端 skuName 已按「中文名 OR 俄文名」处理，直接透传，勿再拆分
  emits('search', searchParams)
}

// 重置
const reset = () => {
  resetFields()
  search()
}

// 处理产品特性状态变化（三状态：不限、包含、不含）
const handleFeatureStateChange = (featureName: string, value: any) => {
  formModel[featureName] = value
}

// 清空所有特性筛选
const clearAllFeatures = () => {
  formModel.needsPower = undefined
  formModel.seasonal = undefined
  formModel.hasRgbLight = undefined
  formModel.hasGlass = undefined
}

// 组件挂载
onMounted(() => {
  loadAllUsers()
})
</script>

<style scoped>
.sku-search-container {
  margin-bottom: 16px;
}

/* 快速搜索卡片 */
.quick-search-card {
  margin-bottom: 12px;
  border-radius: 8px;
  box-shadow:
    0 1px 2px 0 rgba(0, 0, 0, 0.03),
    0 1px 6px -1px rgba(0, 0, 0, 0.02),
    0 2px 4px 0 rgba(0, 0, 0, 0.02);
}

.search-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.search-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.search-icon {
  margin-right: 8px;
  color: #1890ff;
}

.search-actions-inline {
  display: flex;
  gap: 8px;
}

/* 搜索表单布局 - 使用Ant Design栅格系统 */
.quick-search-form {
  margin: 0;
}

.search-group {
  display: flex;
  flex-direction: column;
}

.search-label {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  white-space: nowrap;
}

/* 高级筛选卡片 - 紧凑布局 */
.advanced-filter-card {
  border-radius: 8px;
  box-shadow:
    0 1px 2px 0 rgba(0, 0, 0, 0.03),
    0 1px 6px -1px rgba(0, 0, 0, 0.02),
    0 2px 4px 0 rgba(0, 0, 0, 0.02);
}

/* Ant Design栅格系统已处理响应式布局，移除自定义网格样式 */

.filter-group {
  display: flex;
  flex-direction: column;
}

.filter-label {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  /* 确保左对齐 */
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  white-space: nowrap;
  text-align: left;
  /* 强制文本左对齐 */
}

.label-icon {
  margin-right: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

/* 响应式设计 - 小屏适配 */
@media (max-width: 768px) {
  .search-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .search-actions-inline {
    width: 100%;
    justify-content: flex-end;
  }
}

/* 紧凑模式优化 */
@media (min-width: 1400px) {
  .quick-search-card :deep(.ant-card-body) {
    padding: 20px 24px;
  }

  .advanced-filter-card :deep(.ant-card-body) {
    padding: 20px 24px;
  }

  .search-label,
  .filter-label {
    font-size: 12px;
    margin-bottom: 4px;
  }
}

/* 优化按钮样式 */
:deep(.ant-btn) {
  border-radius: 6px;
  font-weight: 500;
}

:deep(.ant-btn-primary) {
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.2);
}

/* 优化输入框样式 */
:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-picker) {
  border-radius: 6px;
  transition: all 0.2s ease;
}

:deep(.ant-input:focus),
:deep(.ant-select-focused .ant-select-selector),
:deep(.ant-picker-focused) {
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

/* 优化单选按钮组样式 */
:deep(.ant-radio-button-wrapper) {
  border-radius: 4px;
  margin-right: 4px;
  border: 1px solid #d9d9d9;
}

:deep(.ant-radio-button-wrapper:first-child) {
  border-radius: 4px;
}

:deep(.ant-radio-button-wrapper:last-child) {
  border-radius: 4px;
}

/* 产品特性区域样式 - 水平紧凑设计 */

/* 产品特性区域的特殊 label 样式 */
.features-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  white-space: nowrap;
  text-align: left;
}

.inline-clear-btn {
  padding: 0 4px !important;
  height: 16px !important;
  font-size: 10px !important;
  color: #8c8c8c !important;
  margin-left: 8px !important;
  border: none !important;
  background: none !important;
}

.inline-clear-btn:hover {
  color: #ff4d4f !important;
  background: rgba(255, 77, 79, 0.06) !important;
  border-radius: 2px !important;
}

.features-horizontal-container {
  display: flex;
  flex-wrap: nowrap;
  /* 防止换行 */
  gap: 6px;
  align-items: center;
  overflow-x: auto;
  /* 如果空间不够，允许水平滚动 */
  scrollbar-width: none;
  /* 隐藏滚动条 */
  -ms-overflow-style: none;
  /* IE隐藏滚动条 */
  padding: 2px 0;
  /* 为更大的按钮提供垂直空间 */
  min-height: 32px;
  /* 确保容器有足够高度 */
}

.features-horizontal-container::-webkit-scrollbar {
  display: none;
  /* Webkit隐藏滚动条 */
}

.feature-horizontal-item {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 2px 5px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  transition: all 0.2s ease;
  flex-shrink: 0;
  /* 防止压缩 */
  white-space: nowrap;
  /* 防止文字换行 */
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.feature-horizontal-item:hover {
  background: #f5f5f5;
  border-color: #e8e8e8;
}

.feature-name-label {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  font-weight: 500;
  color: #595959;
  user-select: none;
  white-space: nowrap;
}

.feature-icon {
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border-radius: 2px;
  transition: all 0.2s ease;
}

.feature-icon.power {
  background: rgba(250, 173, 20, 0.1);
  color: #faad14;
}

.feature-icon.seasonal {
  background: rgba(82, 196, 26, 0.1);
  color: #52c41a;
}

.feature-icon.rgb {
  background: rgba(114, 46, 209, 0.1);
  color: #722ed1;
}

.feature-icon.glass {
  background: rgba(9, 88, 217, 0.1);
  color: #0958d9;
}

.feature-state-controls {
  display: flex;
  gap: 1px;
}

.mini-state-btn {
  width: 18px !important;
  height: 18px !important;
  padding: 0 !important;
  border-radius: 3px !important;
  font-size: 10px !important;
  font-weight: 600 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  transition: all 0.2s ease !important;
  border: 1px solid #e8e8e8 !important;
  background: #ffffff !important;
  color: #8c8c8c !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05) !important;
}

.mini-state-btn:hover {
  border-color: #1890ff !important;
  color: #1890ff !important;
  background: #f0f9ff !important;
}

.mini-state-btn.ant-btn-primary {
  background: #1890ff !important;
  border-color: #1890ff !important;
  color: #ffffff !important;
  box-shadow: 0 1px 2px rgba(24, 144, 255, 0.15) !important;
}

.mini-state-btn.ant-btn-primary:hover {
  background: #40a9ff !important;
  border-color: #40a9ff !important;
  color: #ffffff !important;
}

/* 特殊状态按钮颜色 */
.mini-state-btn.include.ant-btn-primary {
  background: #52c41a !important;
  border-color: #52c41a !important;
}

.mini-state-btn.include.ant-btn-primary:hover {
  background: #73d13d !important;
  border-color: #73d13d !important;
}

.mini-state-btn.exclude.ant-btn-primary {
  background: #ff4d4f !important;
  border-color: #ff4d4f !important;
}

.mini-state-btn.exclude.ant-btn-primary:hover {
  background: #ff7875 !important;
  border-color: #ff7875 !important;
}

/* 响应式调整 - 高分辨率优化 */
@media (min-width: 1920px) {
  .features-horizontal-container {
    gap: 10px;
  }

  .feature-horizontal-item {
    gap: 5px;
    padding: 4px 8px;
  }

  .feature-name-label {
    font-size: 13px;
    gap: 4px;
  }

  .mini-state-btn {
    width: 22px !important;
    height: 22px !important;
    font-size: 11px !important;
  }

  .feature-icon {
    width: 16px;
    height: 16px;
    font-size: 13px;
  }
}

/* 中高分辨率优化 */
@media (min-width: 1600px) and (max-width: 1919px) {
  .features-horizontal-container {
    gap: 8px;
  }

  .feature-horizontal-item {
    gap: 4px;
    padding: 3px 6px;
  }

  .feature-name-label {
    font-size: 12px;
    gap: 3px;
  }

  .mini-state-btn {
    width: 20px !important;
    height: 20px !important;
    font-size: 10px !important;
  }

  .feature-icon {
    width: 15px;
    height: 15px;
    font-size: 12px;
  }
}

/* 中等分辨率 */
@media (min-width: 1200px) and (max-width: 1599px) {
  .features-horizontal-container {
    gap: 6px;
  }

  .feature-horizontal-item {
    gap: 3px;
    padding: 2px 5px;
  }

  .feature-name-label {
    font-size: 11px;
    gap: 3px;
  }

  .mini-state-btn {
    width: 18px !important;
    height: 18px !important;
    font-size: 10px !important;
  }

  .feature-icon {
    width: 14px;
    height: 14px;
    font-size: 11px;
  }
}

/* 小屏幕压缩 */
@media (max-width: 1199px) {
  .features-horizontal-container {
    gap: 4px;
  }

  .feature-horizontal-item {
    gap: 2px;
    padding: 1px 3px;
  }

  .feature-name-label {
    font-size: 10px;
    gap: 2px;
  }

  .mini-state-btn {
    width: 16px !important;
    height: 16px !important;
    font-size: 9px !important;
  }

  .feature-icon {
    width: 12px;
    height: 12px;
    font-size: 10px;
  }
}

/* 只有在非常小的屏幕上才允许换行 */
@media (max-width: 768px) {
  .features-horizontal-container {
    flex-wrap: wrap;
    /* 小屏幕允许换行 */
    gap: 3px;
  }

  .feature-horizontal-item {
    gap: 2px;
    padding: 1px 3px;
  }
}
</style>
