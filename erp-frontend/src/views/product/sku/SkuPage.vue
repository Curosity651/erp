<template>
  <div class="sku-page">
    <!-- 查询表单 -->
    <sku-page-search :loading="tableRef?.loading" @search="searchTable" />

    <pro-table
      ref="tableRef"
      :header-title="t('product.sku.pageTitle')"
      row-key="id"
      :request="tableRequest"
      :columns="columns"
      :scroll="{ x: 1370 }"
    >
      <!-- 操作按钮区域 -->
      <template #toolBarRender>
        <a-space>
          <export-confirm-button
            :title="t('product.sku.confirmExport')"
            :loading="exportLoading"
            :on-export="handleExport"
          />
          <new-button v-if="hasPermission('product:sku:add')" @click="handleNew" />
        </a-space>
      </template>
    </pro-table>

    <!-- SKU 映射详情弹窗（M6：由业务视图层持有，公共 Badge 仅抛 open 事件，避免 components→views 反向依赖） -->
    <SkuMappingDetailModal
      v-if="mappingModal.skuCode"
      v-model:open="mappingModal.open"
      :sku-code="mappingModal.skuCode"
      :sku-name="mappingModal.skuName || mappingModal.skuCode"
      :sku-files="mappingModal.skuFiles"
      @success="onMappingChanged"
    />
  </div>
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import SkuPageSearch from './SkuPageSearch.vue'
import SkuMappingBadge from '@/components/Sku/SkuMappingBadge.vue'
import SkuMappingDetailModal from './components/SkuMappingDetailModal.vue'
import { NewButton, ExportConfirmButton } from '@/components/Button'
import { h, ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import DictTag from '@/components/Dict/display/DictTag.vue'
import { getCountryByCode } from '@/utils/countries'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { useSkuApi } from '@/hooks/use-sku-api'
import { calculateSkuProperties } from '@/hooks/use-sku-calculator'
import { pageSku, exportSkusExcelByPost } from '@/api/product/sku'
import type { SkuPageVO, SkuQO } from '@/api/product/sku/types'
import { useRouter } from 'vue-router'
import { emitter } from '@/hooks/mitt'

import { Modal, message } from 'ant-design-vue'
import { EditOutlined, CopyOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { remoteFileDownload } from '@/utils/file-utils'
import { getSkuMainImage } from '@/utils/sku-utils'
import { useI18n } from 'vue-i18n'
import { formatLocaleDate, formatLocaleDateTime } from '@/utils/locale-format'
import type { SupportedLocale } from '@/locales/locale-contract'

defineOptions({ name: 'SkuPage' })

// 路由实例
const router = useRouter()

// 鉴权方法
const { hasPermission } = useAuthorize()
const { t, locale } = useI18n()

// 使用SKU API钩子
const { deleteSku } = useSkuApi()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
// 导出按钮加载状态
const exportLoading = ref(false)

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: SkuQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageSku(pageParam, searchParams)
}

/* 查询SKU */
const searchTable = (params: SkuQO) => {
  searchParams = params
  reloadTable(true)
}

/* 导出SKU（POST，body传参，返回文件流） */
const handleExport = async () => {
  if (exportLoading.value) return
  exportLoading.value = true
  try {
    const response = await exportSkusExcelByPost(searchParams || {})
    remoteFileDownload(response)
    message.success(t('product.sku.exportSuccess'))
  } catch (error) {
    console.error(t('product.sku.exportFailed'), error)
    message.error(t('product.sku.exportRetry'))
  } finally {
    exportLoading.value = false
  }
}

/* 新建SKU */
const handleNew = () => {
  router.push({
    path: '/product/sku/form/create',
    query: { _multiTab: t('product.sku.createTitle') }
  })
}

/* 编辑SKU */
const handleEdit = (record: SkuPageVO) => {
  router.push({
    path: `/product/sku/form/edit/${record.id}`,
    query: { _multiTab: t('product.sku.editTab', { sku: record.skuCode }) }
  })
}

/* 复制创建SKU */
const handleCopy = (record: SkuPageVO) => {
  // 准备复制数据 - 使用深拷贝避免引用问题
  const copyData = JSON.parse(JSON.stringify(record))

  // 删除不需要的属性
  delete copyData.id
  delete copyData.createTime
  delete copyData.updateTime

  // 清空SKU编码和序号，让用户重新输入
  copyData.skuCode = ''
  copyData.skuNo = undefined

  // 将复制数据存储到 sessionStorage，避免 URL 过长
  const copyKey = `sku_copy_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`

  try {
    // 先检查 sessionStorage 是否可用
    if (typeof Storage === 'undefined') {
      throw new Error(t('product.sku.storageUnsupported'))
    }

    // 安全序列化，处理可能的循环引用
    const jsonString = JSON.stringify(copyData, (key, value) => {
      // 过滤掉可能的函数或循环引用
      if (typeof value === 'function') {
        return undefined
      }
      return value
    })

    // 检查数据大小，sessionStorage 通常限制为 5-10MB
    if (jsonString.length > 5 * 1024 * 1024) {
      throw new Error(t('product.sku.storageTooLarge'))
    }

    sessionStorage.setItem(copyKey, jsonString)

    // 验证存储是否成功
    const stored = sessionStorage.getItem(copyKey)
    if (!stored) {
      throw new Error(t('product.sku.storageVerificationFailed'))
    }

    // 显示提示信息，提醒用户需要填写必要字段
    message.info(t('product.sku.copyReminder'), 3)

    // 立即跳转，避免页面刷新导致数据丢失
    router.push({
      path: '/product/sku/form/copy',
      query: {
        copyKey,
        _multiTab: t('product.sku.copyTitle')
      }
    })
  } catch (error) {
    console.error(t('product.sku.copySaveFailed'), error)
    const errorMessage = error instanceof Error ? error.message : t('product.sku.unknownError')
    message.error(t('product.sku.copySaveFailedWithReason', { reason: errorMessage }))
  }
}

/* 删除SKU */
const handleDelete = (record: SkuPageVO) => {
  Modal.confirm({
    title: t('product.sku.confirmDelete'),
    content: t('product.sku.confirmDeleteContent', { sku: record.skuCode }),
    onOk: async () => {
      try {
        await deleteSku(record.id)
        message.success(t('message.removeSuccess'))
        reloadTable()
      } catch (error) {
        console.error(t('product.sku.deleteFailed'), error)
      }
    }
  })
}

/* 映射操作成功后刷新表格 */
const onMappingChanged = () => {
  reloadTable(false) // 刷新当前页，不重置页码
}

/* SKU 映射详情弹窗状态（M6：弹窗归业务视图层持有；每行 Badge 点击时用闭包捕获该行上下文打开） */
const mappingModal = reactive<{ open: boolean; skuCode: string; skuName?: string; skuFiles?: any }>(
  {
    open: false,
    skuCode: '',
    skuName: undefined,
    skuFiles: undefined
  }
)
const openMappingModal = (record: any) => {
  mappingModal.skuCode = record.skuCode
  mappingModal.skuName = record.chineseName
  mappingModal.skuFiles = record.files
  mappingModal.open = true
}

const ossDomain = import.meta.env.VITE_OSS_DOMAIN

const columns = computed<ProColumns[]>(() => [
  {
    title: t('product.sku.productInfo'),
    key: 'productInfo',
    width: 260,
    fixed: 'left',
    customRender: ({ record }) => {
      // 获取产品图片URL - 优先实物图片，其次平台图片
      const getProductImageUrl = () => {
        return getSkuMainImage(record.files, ossDomain)
      }

      return h('div', { class: 'product-info-cell' }, [
        // 上半部分：头像、中文品名、品牌、序号
        h('div', { class: 'product-upper-section' }, [
          // 产品图片（头像）
          h('div', { class: 'product-image' }, [
            h('img', {
              src: getProductImageUrl(),
              alt: record.chineseName || t('product.sku.productImage'),
              class: 'product-img',
              onClick: () => {
                // 点击图片预览
                const imageUrl = getProductImageUrl()
                if (imageUrl && imageUrl !== '/placeholder-product.svg') {
                  // 创建预览模态框
                  Modal.info({
                    title: record.chineseName || t('product.sku.productImage'),
                    content: h('div', { class: 'image-preview-modal' }, [
                      h('img', {
                        src: imageUrl,
                        alt: record.chineseName || t('product.sku.productImage'),
                        style: {
                          width: '100%',
                          height: 'auto',
                          maxHeight: '70vh',
                          objectFit: 'contain'
                        }
                      })
                    ]),
                    width: 800,
                    centered: true,
                    maskClosable: true,
                    okText: t('product.sku.close')
                  })
                }
              }
            }),
            // 产品状态标识（右上角）
            h('div', { class: 'status-badge' }, [
              h(DictTag, {
                dictCode: 'product_status',
                value: record.productStatus,
                size: 'small'
              })
            ])
          ]),
          // 右侧：产品名称、品牌、序号
          h('div', { class: 'product-basic-info' }, [
            // 产品名称
            h('div', { class: 'product-name' }, [
              h(
                'span',
                {
                  class: 'name-text',
                  title: record.chineseName || record.russianName || t('product.sku.unnamedProduct')
                },
                record.chineseName || record.russianName || t('product.sku.unnamedProduct')
              )
            ]),
            // 品牌和序号同行
            h('div', { class: 'brand-sequence-row' }, [
              h('div', { class: 'brand-info' }, [
                h('span', { class: 'brand-tag' }, record.brandName || t('product.sku.noBrand'))
              ]),
              h('div', { class: 'sequence-info' }, [
                h('span', { class: 'sequence-badge' }, `#${record.skuNo || '-'}`)
              ])
            ])
          ])
        ]),

        // 下半部分：SKU code、SPU code、项目组、品类
        h('div', { class: 'product-lower-section' }, [
          // 编码信息
          h('div', { class: 'code-section' }, [
            h('div', { class: 'code-row primary' }, [
              h('span', { class: 'code-label' }, 'SKU'),
              h('span', { class: 'code-value sku-code' }, record.skuCode || '-'),
              // 映射徽章 - 紧跟在 SKU code 后面
              h(SkuMappingBadge, {
                count: record.mappingCount || 0,
                skuCode: record.skuCode,
                skuName: record.chineseName,
                skuFiles: record.files,
                size: 'sm',
                onOpen: () => openMappingModal(record)
              })
            ]),
            h('div', { class: 'code-row secondary' }, [
              h('span', { class: 'code-label' }, 'SPU'),
              h('span', { class: 'code-value spu-code' }, record.spuCode || '-')
            ])
          ]),
          // 项目组和品类信息
          h('div', { class: 'category-section' }, [
            // 项目组信息
            h('div', { class: 'project-item' }, [
              h('span', { class: 'project-icon' }, '👥'),
              h('span', { class: 'project-text' }, record.projectGroupName || '-')
            ]),
            // 品类信息
            h('div', { class: 'category-item' }, [
              h('span', { class: 'category-icon' }, '📁'),
              h(
                'span',
                {
                  class: 'category-text',
                  title: record.categoryHierarchy?.fullPathName || record.categoryName || '-'
                },
                record.categoryHierarchy?.fullPathName || record.categoryName || '-'
              )
            ])
          ])
        ])
      ])
    }
  },
  {
    title: t('product.sku.description'),
    key: 'productDescription',
    width: 200,
    customRender: ({ record }) => {
      const description = record.description || t('product.sku.noDescription')
      const isLong = description.length > 120

      return h('div', { class: 'description-cell table-cell' }, [
        h(
          'p',
          {
            class: 'description-text',
            title: description
          },
          isLong ? description.substring(0, 120) + '...' : description
        )
      ])
    }
  },

  {
    title: t('product.sku.purchaseInfo'),
    key: 'purchaseInfo',
    width: 200,
    customRender: ({ record }) => {
      const purchasePrice = record.purchasePrice ? parseFloat(record.purchasePrice) : 0

      return h('div', { class: 'purchase-info-cell table-cell' }, [
        // 采购价格信息（最重要）
        h('div', { class: 'price-section table-section' }, [
          h('div', { class: 'price-header table-row' }, [
            h('span', { class: 'price-icon table-icon' }, '💰'),
            h('span', { class: 'price-label table-label' }, t('product.sku.purchasePrice'))
          ]),
          h('div', { class: 'price-main' }, [
            h('span', { class: 'currency' }, '¥'),
            h('span', { class: 'price-amount table-price-value' }, purchasePrice.toFixed(2) || '-')
          ]),
          // 税务信息紧跟价格
          h(
            'div',
            { class: 'tax-info' },
            [
              record.includeTax !== undefined &&
                h('div', { class: 'tax-item' }, [
                  h('span', { class: 'tax-label table-small-label' }, t('product.sku.taxIncluded')),
                  h(
                    'span',
                    { class: 'tax-value table-small-value' },
                    record.includeTax ? t('product.sku.yes') : t('product.sku.no')
                  )
                ]),
              record.taxRate &&
                h('div', { class: 'tax-item' }, [
                  h('span', { class: 'tax-label table-small-label' }, t('product.sku.taxRate')),
                  h('span', { class: 'tax-value table-small-value' }, `${record.taxRate}%`)
                ])
            ].filter(Boolean)
          )
        ]),

        // 供应商信息
        h('div', { class: 'supplier-section table-section' }, [
          h('div', { class: 'supplier-row table-row' }, [
            h('span', { class: 'supplier-icon table-icon' }, '🏭'),
            h('span', { class: 'supplier-label table-small-label' }, t('product.sku.supplier')),
            h('span', { class: 'supplier-code' }, record.supplierCode || '-')
          ])
        ]),

        // 采购条件
        h(
          'div',
          { class: 'purchase-conditions' },
          [
            record.minimumOrderQuantity &&
              h('div', { class: 'condition-item table-row' }, [
                h('span', { class: 'condition-icon table-icon' }, '📦'),
                h('div', { class: 'condition-info table-info-row' }, [
                  h(
                    'span',
                    { class: 'condition-label table-small-label' },
                    t('product.sku.minimumOrder')
                  ),
                  h(
                    'span',
                    { class: 'condition-value table-small-value' },
                    t('product.sku.pieces', { count: record.minimumOrderQuantity })
                  )
                ])
              ]),
            record.productionCycle &&
              h('div', { class: 'condition-item table-row' }, [
                h('span', { class: 'condition-icon table-icon' }, '⏱️'),
                h('div', { class: 'condition-info table-info-row' }, [
                  h(
                    'span',
                    { class: 'condition-label table-small-label' },
                    t('product.sku.productionCycle')
                  ),
                  h(
                    'span',
                    { class: 'condition-value table-small-value' },
                    t('product.sku.days', { count: record.productionCycle })
                  )
                ])
              ])
          ].filter(Boolean)
        )
      ])
    }
  },
  {
    title: t('product.sku.specsLogistics'),
    key: 'specsLogistics',
    width: 200,
    customRender: ({ record }) => {
      const { packageLength, packageWidth, packageHeight, packageUnit, weight, weightUnit } = record
      const dimensions =
        packageLength && packageWidth && packageHeight
          ? `${packageLength}×${packageWidth}×${packageHeight}${packageUnit || ''}`
          : '-'

      // 使用统一的计算方法
      const calculationResult = calculateSkuProperties(
        packageLength,
        packageWidth,
        packageHeight,
        packageUnit,
        weight,
        weightUnit
      )

      const { packageVolume, densityKgM3, containerCapacity } = calculationResult

      return h('div', { class: 'specs-logistics-cell table-cell' }, [
        // 包装规格
        h('div', { class: 'package-specs table-section' }, [
          h('div', { class: 'spec-row table-row' }, [
            h('span', { class: 'spec-icon table-icon' }, '📦'),
            h('span', { class: 'spec-title table-label' }, t('product.sku.packageSpecs')),
            h('span', { class: 'spec-value table-value' }, dimensions)
          ])
        ]),

        // 重量信息
        weight &&
          h('div', { class: 'weight-section table-section' }, [
            h('div', { class: 'weight-row table-row' }, [
              h('span', { class: 'weight-icon table-icon' }, '⚖️'),
              h('span', { class: 'weight-title table-label' }, t('product.sku.weight')),
              h('span', { class: 'weight-value table-value' }, `${weight}${weightUnit || 'g'}`)
            ])
          ]),

        // 体积信息
        packageVolume > 0 &&
          h('div', { class: 'volume-section table-section' }, [
            h('div', { class: 'volume-row table-row' }, [
              h('span', { class: 'volume-icon table-icon' }, '📐'),
              h('span', { class: 'volume-title table-label' }, t('product.sku.volume')),
              h('span', { class: 'volume-value table-value' }, `${packageVolume.toFixed(3)}m³`)
            ])
          ]),

        // 密度信息 - kg/m³
        densityKgM3 > 0 &&
          h('div', { class: 'density-section table-section' }, [
            h('div', { class: 'density-row table-row' }, [
              h('span', { class: 'density-icon table-icon' }, '📏'),
              h('span', { class: 'density-title table-label' }, t('product.sku.density')),
              h('span', { class: 'density-value table-value' }, `${densityKgM3.toFixed(3)}`)
            ])
          ]),

        // 每托数量信息 - 始终显示
        h('div', { class: 'pallet-section table-section' }, [
          h('div', { class: 'pallet-row table-row' }, [
            h('span', { class: 'pallet-icon table-icon' }, '🏗️'),
            h('span', { class: 'pallet-title table-label' }, t('product.sku.quantityPerPallet')),
            h(
              'span',
              { class: 'pallet-value table-value' },
              record.quantityPerPallet
                ? t('product.sku.pieces', { count: record.quantityPerPallet })
                : '-'
            )
          ])
        ]),

        // 装柜量信息
        containerCapacity > 0 &&
          h('div', { class: 'container-section table-section' }, [
            h('div', { class: 'container-row table-row' }, [
              h('span', { class: 'container-icon table-icon' }, '📦'),
              h(
                'span',
                { class: 'container-title table-label' },
                t('product.sku.containerCapacity')
              ),
              h(
                'span',
                { class: 'container-value table-value' },
                t('product.sku.units', { count: containerCapacity.toFixed(3) })
              )
            ])
          ]),

        // 包装信息
        h(
          'div',
          { class: 'package-info' },
          [
            record.packageType &&
              h('div', { class: 'logistics-row package table-row' }, [
                h('span', { class: 'logistics-icon table-icon-fixed' }, '📦'),
                h(
                  'span',
                  { class: 'package-label table-small-label' },
                  t('product.sku.packageType')
                ),
                h(
                  'span',
                  { class: 'package-value table-small-value' },
                  record.packageType === 'normal'
                    ? t('product.sku.normalPackage')
                    : record.packageType === 'magnetic'
                      ? t('product.sku.magneticPackage')
                      : record.packageType
                )
              ]),
            record.packaging &&
              h('div', { class: 'logistics-row packaging table-row' }, [
                h('span', { class: 'logistics-icon table-icon-fixed' }, '📋'),
                h(
                  'span',
                  { class: 'packaging-label table-small-label' },
                  t('product.sku.splitBoxes')
                ),
                h('span', { class: 'packaging-value table-small-value' }, record.packaging)
              ])
          ].filter(Boolean)
        )
      ])
    }
  },
  {
    title: t('product.sku.attributes'),
    key: 'attributesFeatures',
    width: 200,
    customRender: ({ record }) => {
      return h('div', { class: 'attributes-features-cell table-cell' }, [
        // 产品特性标签
        h(
          'div',
          { class: 'product-features table-section' },
          [
            record.needsPower &&
              h('span', { class: 'feature-tag power' }, t('product.sku.featurePower')),
            record.seasonal &&
              h('span', { class: 'feature-tag seasonal' }, t('product.sku.featureSeasonal')),
            record.hasRgbLight &&
              h('span', { class: 'feature-tag rgb-light' }, t('product.sku.featureRgb')),
            record.hasGlass &&
              h('span', { class: 'feature-tag glass' }, t('product.sku.featureGlass')),
            record.packageType === 'magnetic' &&
              h('span', { class: 'feature-tag magnetic' }, t('product.sku.featureMagnetic'))
          ].filter(Boolean)
        ),

        // 产品属性
        h('div', { class: 'attributes-section table-section' }, [
          h('div', { class: 'attr-item' }, [
            h('span', { class: 'attr-label table-small-label' }, t('product.sku.surfaceColor')),
            h('span', { class: 'attr-value table-small-value' }, record.surfaceColor || '-')
          ]),
          h('div', { class: 'attr-item' }, [
            h('span', { class: 'attr-label table-small-label' }, t('product.sku.frameColor')),
            h('span', { class: 'attr-value table-small-value' }, record.frameColor || '-')
          ])
        ]),

        // 销售国家
        h('div', { class: 'country-section table-section' }, [
          h('div', { class: 'attr-item' }, [
            h('span', { class: 'attr-label table-small-label' }, t('product.sku.salesCountry')),
            h(
              'span',
              { class: 'attr-value table-small-value' },
              (() => {
                if (!record.salesCountry) return '-'
                const country = getCountryByCode(record.salesCountry)
                return country ? `${country.name} (${country.code})` : record.salesCountry
              })()
            )
          ])
        ]),

        // 功能性能要求
        record.functionalRequirements &&
          h('div', { class: 'functional-requirements' }, [
            h('div', { class: 'functional-header table-row' }, [
              h('span', { class: 'functional-icon table-icon' }, '⚙️'),
              h(
                'span',
                { class: 'functional-title table-label' },
                t('product.sku.functionalRequirements')
              )
            ]),
            h(
              'p',
              {
                class: 'functional-text',
                title: record.functionalRequirements
              },
              record.functionalRequirements.length > 60
                ? record.functionalRequirements.substring(0, 60) + '...'
                : record.functionalRequirements
            )
          ])
      ])
    }
  },
  {
    title: t('product.sku.team'),
    key: 'teamInfo',
    width: 170,
    customRender: ({ record }) => {
      const teamMembers = [
        {
          role: t('product.sku.roleDeveloper'),
          name: record.developerName,
          icon: '👨‍💻',
          color: '#1890ff'
        },
        {
          role: t('product.sku.roleOperator'),
          name: record.operatorName,
          icon: '👨‍💼',
          color: '#52c41a'
        },
        { role: t('product.sku.roleQc'), name: record.qcName, icon: '🔍', color: '#faad14' },
        {
          role: t('product.sku.rolePurchaser'),
          name: record.purchaserName,
          icon: '🛒',
          color: '#722ed1'
        }
      ]

      return h('div', { class: 'team-cell table-cell' }, [
        // 团队成员列表
        h(
          'div',
          { class: 'team-members' },
          teamMembers.map(member =>
            h(
              'div',
              {
                class: `team-member ${member.name ? 'assigned' : 'unassigned'}`,
                key: member.role
              },
              [
                h('div', { class: 'member-avatar' }, [
                  h(
                    'span',
                    {
                      class: 'member-icon',
                      style: { color: member.color }
                    },
                    member.icon
                  )
                ]),
                h('div', { class: 'member-info' }, [
                  h('div', { class: 'member-role' }, member.role),
                  h(
                    'div',
                    {
                      class: `member-name ${member.name ? 'has-name' : 'no-name'}`
                    },
                    member.name || t('product.sku.unassigned')
                  )
                ])
              ]
            )
          )
        )
      ])
    }
  },
  {
    title: t('product.sku.timeRecords'),
    key: 'timeRecord',
    width: 140,
    customRender: ({ record }) => {
      const formatTime = (timeStr: string) => {
        if (!timeStr) return '-'
        return formatLocaleDate(timeStr, locale.value as SupportedLocale, {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit'
        })
      }

      const formatDateTime = (timeStr: string) => {
        if (!timeStr) return '-'
        return formatLocaleDateTime(timeStr, locale.value as SupportedLocale, {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        })
      }

      return h('div', { class: 'time-record-cell table-cell' }, [
        h('div', { class: 'time-rows' }, [
          h('div', { class: 'time-row table-row' }, [
            h('span', { class: 'time-icon table-icon-fixed' }, '📅'),
            h('div', { class: 'time-info' }, [
              h('div', { class: 'time-label table-small-label' }, t('common.createTime')),
              h(
                'div',
                {
                  class: 'time-value table-small-value',
                  title: formatDateTime(record.createTime)
                },
                formatTime(record.createTime)
              )
            ])
          ]),
          h('div', { class: 'time-row table-row' }, [
            h('span', { class: 'time-icon table-icon-fixed' }, '🔄'),
            h('div', { class: 'time-info' }, [
              h('div', { class: 'time-label table-small-label' }, t('common.updateTime')),
              h(
                'div',
                {
                  class: 'time-value table-small-value',
                  title: formatDateTime(record.updateTime)
                },
                formatTime(record.updateTime)
              )
            ])
          ])
        ])
      ])
    }
  },
  {
    key: 'operate',
    title: t('common.operation'),
    align: 'center',
    width: 100,
    fixed: 'right',
    customRender: ({ record }) => {
      return h('div', { class: 'operate-cell' }, [
        h(
          'div',
          { class: 'operate-buttons-vertical' },
          [
            // 编辑按钮
            hasPermission('product:sku:edit') &&
              h(
                'button',
                {
                  class: 'operate-btn edit',
                  onClick: () => handleEdit(record)
                },
                [
                  h(EditOutlined, { class: 'btn-icon' }),
                  h('span', { class: 'btn-text' }, t('action.edit'))
                ]
              ),

            // 复制按钮
            h(
              'button',
              {
                class: 'operate-btn copy',
                onClick: () => handleCopy(record)
              },
              [
                h(CopyOutlined, { class: 'btn-icon' }),
                h('span', { class: 'btn-text' }, t('product.sku.copy'))
              ]
            ),

            // 删除按钮
            hasPermission('product:sku:del') &&
              h(
                'button',
                {
                  class: 'operate-btn delete',
                  onClick: () => handleDelete(record)
                },
                [
                  h(DeleteOutlined, { class: 'btn-icon' }),
                  h('span', { class: 'btn-text' }, t('action.delete'))
                ]
              )
          ].filter(Boolean)
        )
      ])
    }
  }
])

// 事件监听处理
onMounted(() => {
  // 监听SKU列表刷新事件
  emitter.on('refresh-sku-list', () => {
    console.log('收到刷新SKU列表事件，正在刷新表格...')
    reloadTable()
  })
})

onUnmounted(() => {
  // 清理事件监听，避免内存泄漏
  emitter.off('refresh-sku-list')
})
</script>

<style scoped>
/* ==============================================
   通用基础样式类 - 用于减少重复代码
   ============================================== */

/* 通用图标样式 */
:deep(.table-icon) {
  font-size: 12px;
  color: #8c8c8c;
}

/* 固定宽度图标样式（用于对齐） */
:deep(.table-icon-fixed) {
  font-size: 12px;
  color: #8c8c8c;
  width: 16px;
  text-align: center;
}

/* 通用标题样式 */
:deep(.table-label) {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 600;
}

/* 通用数值样式 */
:deep(.table-value) {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  margin-left: auto;
}

/* 通用行布局 */
:deep(.table-row) {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 通用节间距 */
:deep(.table-section) {
  margin-bottom: 12px;
}

/* 表格单元格通用样式 */
:deep(.table-cell) {
  padding: 16px 12px;
}

/* 小标签样式 */
:deep(.table-small-label) {
  font-size: 11px;
  color: #8c8c8c;
}

/* 小值样式 */
:deep(.table-small-value) {
  font-size: 12px;
  color: #262626;
  font-weight: 500;
}

/* 价格数值样式 */
:deep(.table-price-value) {
  font-size: 20px;
  color: #262626;
  font-weight: 700;
}

/* 条件/信息行样式 */
:deep(.table-info-row) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
}

/* ==============================================
   表格基础样式 - 专业UI设计
   ============================================== */

:deep(.ant-table) {
  font-size: 14px;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8e8e8;
}

:deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 600;
  color: #262626;
  padding: 16px 12px;
  font-size: 13px;
}

/* 表格行样式优化 */
:deep(.ant-table-tbody > tr) {
  transition: background-color 0.2s ease;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.ant-table-tbody > tr:hover) {
  background: #fafafa;
}

:deep(.ant-table-tbody > tr:last-child) {
  border-bottom: none;
}

/* 产品信息列样式 - 简洁上下分层布局设计 */
:deep(.product-info-cell) {
  padding: 14px 10px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 上半部分：头像、中文品名、品牌、序号 */
:deep(.product-upper-section) {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

/* 产品图片优化 */
:deep(.product-image) {
  position: relative;
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f0 100%);
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

:deep(.product-img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

:deep(.product-img:hover) {
  transform: scale(1.08);
}

:deep(.product-image:hover) {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
  transform: translateY(-1px);
}

:deep(.status-badge) {
  position: absolute;
  top: -6px;
  right: -6px;
  z-index: 2;
  transform: scale(0.85);
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

/* 产品基本信息区域 */
:deep(.product-basic-info) {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

:deep(.product-name) {
  display: flex;
  align-items: center;
  margin-bottom: 2px;
}

:deep(.name-text) {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  letter-spacing: 0.2px;
}

/* 品牌和序号同行 - 修复对齐问题 */
:deep(.brand-sequence-row) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  height: 24px;
  /* 固定高度确保对齐 */
}

:deep(.brand-info) {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  /* 确保垂直居中 */
}

:deep(.brand-tag) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  color: #495057;
  padding: 5px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border: 1px solid #dee2e6;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  min-height: 22px;
}

:deep(.brand-tag:hover) {
  background: linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%);
  transform: translateY(-0.5px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

:deep(.sequence-info) {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  /* 确保垂直居中 */
}

:deep(.sequence-badge) {
  background: linear-gradient(135deg, #1890ff 0%, #0958d9 100%);
  color: #ffffff;
  padding: 5px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  box-shadow: 0 2px 6px rgba(24, 144, 255, 0.3);
  transition: all 0.2s ease;
  letter-spacing: 0.4px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  min-height: 22px;
  justify-content: center;
}

:deep(.sequence-badge:hover) {
  background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
  transform: translateY(-0.5px);
  box-shadow: 0 3px 8px rgba(24, 144, 255, 0.4);
  border-color: rgba(255, 255, 255, 0.3);
}

/* 下半部分：SKU code、SPU code、项目组、品类 */
:deep(.product-lower-section) {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  position: relative;
}

:deep(.product-lower-section::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #d9d9d9, transparent);
}

/* 编码信息美化 */
:deep(.code-section) {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

:deep(.code-row) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.code-label) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  color: #6c757d;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
  min-width: 32px;
  text-align: center;
  flex-shrink: 0;
  border: 1px solid #dee2e6;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  letter-spacing: 0.5px;
}

:deep(.sku-code) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-weight: 600;
  color: #262626;
  font-size: 13px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.spu-code) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-weight: 500;
  color: #8c8c8c;
  font-size: 12px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 项目组和品类信息美化 */
:deep(.category-section) {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

:deep(.category-item),
:deep(.project-item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.category-icon),
:deep(.project-icon) {
  font-size: 12px;
  flex-shrink: 0;
}

:deep(.category-text),
:deep(.project-text) {
  font-size: 12px;
  color: #595959;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

/* 产品描述列样式 - 使用通用样式类 */
:deep(.description-text) {
  margin: 0;
  line-height: 1.6;
  color: #262626;
  font-size: 13px;
  word-break: break-word;
}

/* 属性特性列样式 - 使用通用样式类，移除重复定义 */
:deep(.product-features) {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

:deep(.feature-tag) {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #389e0d;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

:deep(.feature-tag.power) {
  background: #fff7e6;
  border-color: #ffd591;
  color: #d46b08;
}

:deep(.feature-tag.seasonal) {
  background: #f0f9ff;
  border-color: #91d5ff;
  color: #0958d9;
}

:deep(.feature-tag.fragile) {
  background: #fff2e8;
  border-color: #ffbb96;
  color: #d4380d;
}

:deep(.feature-tag.rgb-light) {
  background: #f9f0ff;
  border-color: #d3adf7;
  color: #722ed1;
}

:deep(.feature-tag.glass) {
  background: #e6f7ff;
  border-color: #91d5ff;
  color: #0958d9;
}

:deep(.attr-item) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  margin-bottom: 6px;
}

:deep(.attr-item:last-child) {
  margin-bottom: 0;
}

:deep(.functional-requirements) {
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

:deep(.functional-text) {
  margin: 0;
  line-height: 1.5;
  color: #595959;
  font-size: 12px;
  word-break: break-word;
}

/* 采购信息列样式 - 使用通用样式类，移除重复定义 */
:deep(.price-main) {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}

:deep(.currency) {
  font-size: 14px;
  color: #262626;
  font-weight: 600;
}

:deep(.tax-info) {
  display: flex;
  gap: 8px;
}

:deep(.tax-item) {
  display: flex;
  align-items: center;
  gap: 4px;
}

:deep(.tax-value) {
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 8px;
}

:deep(.supplier-section) {
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.supplier-code) {
  font-size: 13px;
  color: #262626;
  font-weight: 600;
  margin-left: auto;
}

:deep(.purchase-conditions) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 规格物流列样式 - 使用通用样式类，移除重复定义 */

/* 包装信息样式 - 使用通用样式类，移除重复定义 */
:deep(.package-info) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

:deep(.logistics-info) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

:deep(.logistics-row:last-child) {
  margin-bottom: 0;
}

:deep(.country-info),
:deep(.shipping-info) {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

:deep(.shipping-value),
:deep(.package-value),
:deep(.packaging-value) {
  margin-left: auto;
}

/* 重复的feature-tag样式已移除，使用上面统一的定义 */

/* 产品属性列重复样式已移除，使用通用样式类 */

/* 团队协作列样式 - 使用通用样式类，保留特有样式 */
:deep(.team-members) {
  margin-bottom: 16px;
}

:deep(.team-member) {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 4px 0;
}

:deep(.team-member:last-child) {
  margin-bottom: 0;
}

:deep(.member-avatar) {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.member-icon) {
  font-size: 10px;
  color: #8c8c8c;
}

:deep(.member-info) {
  flex: 1;
  min-width: 0;
}

:deep(.member-role) {
  font-size: 10px;
  color: #8c8c8c;
  line-height: 1.2;
  margin-bottom: 2px;
}

:deep(.member-name.has-name) {
  font-size: 12px;
  color: #262626;
  font-weight: 500;
  line-height: 1.2;
}

:deep(.member-name.no-name) {
  font-size: 11px;
  color: #bfbfbf;
  font-weight: 400;
  font-style: italic;
  line-height: 1.2;
}

/* 时间记录列样式 - 使用通用样式类，保留特有样式 */
:deep(.time-rows) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.time-info) {
  flex: 1;
}

:deep(.time-label) {
  line-height: 1.2;
  margin-bottom: 2px;
}

:deep(.time-value) {
  line-height: 1.2;
  cursor: help;
}

/* 操作列样式 - 简洁竖向设计 */
:deep(.operate-cell) {
  padding: 10px 6px;
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.operate-buttons-vertical) {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  width: 100%;
}

:deep(.operate-btn) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 28px;
  padding: 0 6px;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 12px;
  font-weight: 400;
  color: #666666;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

:deep(.operate-btn:hover) {
  border-color: #d9d9d9;
  background: #fafafa;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

:deep(.operate-btn .btn-icon) {
  font-size: 12px;
  flex-shrink: 0;
}

:deep(.operate-btn .btn-text) {
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

/* 编辑按钮 - 蓝色主题 */
:deep(.operate-btn.edit) {
  color: #1677ff;
  border-color: #d6e4ff;
  background: #f0f5ff;
}

:deep(.operate-btn.edit .btn-icon) {
  color: #1677ff;
}

:deep(.operate-btn.edit:hover) {
  color: #0958d9;
  border-color: #adc6ff;
  background: #e6f4ff;
  box-shadow: 0 2px 4px rgba(22, 119, 255, 0.12);
}

:deep(.operate-btn.edit:hover .btn-icon) {
  color: #0958d9;
}

/* 复制按钮 - 绿色主题 */
:deep(.operate-btn.copy) {
  color: #389e0d;
  border-color: #d9f7be;
  background: #f6ffed;
}

:deep(.operate-btn.copy .btn-icon) {
  color: #389e0d;
}

:deep(.operate-btn.copy:hover) {
  color: #237804;
  border-color: #b7eb8f;
  background: #efffb7;
  box-shadow: 0 2px 4px rgba(56, 158, 13, 0.12);
}

:deep(.operate-btn.copy:hover .btn-icon) {
  color: #237804;
}

/* 删除按钮 - 红色主题 */
:deep(.operate-btn.delete) {
  color: #cf1322;
  border-color: #ffccc7;
  background: #fff2f0;
}

:deep(.operate-btn.delete .btn-icon) {
  color: #cf1322;
}

:deep(.operate-btn.delete:hover) {
  color: #a8071a;
  border-color: #ffa39e;
  background: #ffebe8;
  box-shadow: 0 2px 4px rgba(207, 19, 34, 0.12);
}

:deep(.operate-btn.delete:hover .btn-icon) {
  color: #a8071a;
}

/* 按钮激活状态 */
:deep(.operate-btn:active) {
  transform: translateY(1px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

/* 按钮激活状态 */
:deep(.operate-btn:active) {
  transform: translateY(1px);
}

/* 按钮禁用状态 */
:deep(.operate-btn:disabled) {
  opacity: 0.5;
  cursor: not-allowed;
  color: #bfbfbf;
  border-color: #f0f0f0;
  background: #fafafa;
}

:deep(.operate-btn:disabled:hover) {
  color: #bfbfbf;
  border-color: #f0f0f0;
  background: #fafafa;
  transform: none;
}

/* 表格行高调整 */
:deep(.ant-table-tbody > tr > td) {
  padding: 12px 8px;
  vertical-align: top;
}

/* 固定列阴影优化 */
:deep(.ant-table-fixed-left) {
  box-shadow: 6px 0 6px -4px rgba(0, 0, 0, 0.15);
}

:deep(.ant-table-fixed-right) {
  box-shadow: -6px 0 6px -4px rgba(0, 0, 0, 0.15);
}

/* 图片预览模态框样式 */
:deep(.image-preview-modal) {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

:deep(.image-preview-modal img) {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>
