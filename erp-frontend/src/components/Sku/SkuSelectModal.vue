<template>
  <a-modal
    :open="open"
    title="选择SKU"
    :width="900"
    :body-style="{ paddingBottom: '0px' }"
    :confirm-loading="confirmLoading"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <!-- 搜索区域 -->
    <a-form layout="inline" style="margin-bottom: 16px">
      <a-form-item label="SKU编码">
        <a-input
          v-model:value="searchForm.skuCode"
          placeholder="输入后搜索"
          allow-clear
          @blur="trim('skuCode')"
          @press-enter="doSearch"
        />
      </a-form-item>
      <a-form-item label="中文名">
        <a-input
          v-model:value="searchForm.skuName"
          placeholder="输入后搜索"
          allow-clear
          @blur="trim('skuName')"
          @press-enter="doSearch"
        />
      </a-form-item>
      <a-button type="primary" @click="doSearch">搜索</a-button>
      <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
    </a-form>

    <!-- 表格 -->
    <pro-table
      ref="tableRef"
      size="small"
      row-key="id"
      :tool-bar-render="false"
      :table-alert-render="false"
      :columns="columns"
      :request="tableRequest"
      :row-selection="rowSelection"
      :card-props="{ bodyStyle: { padding: 0 } }"
      :scroll="{ x: 700 }"
      :pagination="{ pageSize: 10 }"
    />
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick, h } from 'vue'
import ProTable from '#/table'
import type { ProColumns, ProTableInstanceExpose, TableRequest } from '#/table'
import { pageSkuForSelect, listSkuByCodes } from '@/api/product/sku'
import type { SkuSelectVO } from '@/api/product/sku/types'
import { mergePageParam } from '@/utils/page-utils'
import { isSuccess } from '@/api'
import type { SkuRow } from '@/components/Sku/types'

const props = withDefaults(
  defineProps<{
    open: boolean
    multiple?: boolean
    /** 初始选中的 SKU 编码列表（用于回显） */
    initialSelectedCodes?: string[]
    /** 所属货主ID（平台端跨货主选品时传，按该货主查其 SKU 目录） */
    erpTenantId?: number
  }>(),
  {
    multiple: false,
    initialSelectedCodes: () => []
  }
)

const emits = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'confirm', rows: SkuRow[]): void
}>()

const confirmLoading = ref(false)
const initialLoading = ref(false)
const tableRef = ref<ProTableInstanceExpose>()

const searchForm = reactive<{ skuCode?: string; skuName?: string }>({})
let searchParams: Record<string, string> = {}

// 选中的 ID 列表
const selectedKeys = ref<number[]>([])
// 缓存已选中的 SKU 数据
const selectedMap = ref<Record<number, SkuRow>>({})

const isMultiple = computed(() => !!props.multiple)

/**
 * 将 SkuSelectVO 转换为 SkuRow
 */
const convertToSkuRow = (vo: SkuSelectVO): SkuRow => ({
  id: vo.id,
  skuCode: vo.skuCode,
  chineseName: vo.chineseName,
  mainImage: vo.mainImage,
  categoryFullPath: vo.categoryFullPath,
  outerLengthMm: vo.outerLengthMm,
  outerWidthMm: vo.outerWidthMm,
  outerHeightMm: vo.outerHeightMm
})

/**
 * 加载初始选中数据
 */
const loadInitialSelection = async (codes: string[]) => {
  if (!codes || codes.length === 0) return

  initialLoading.value = true
  try {
    const result = await listSkuByCodes(codes)
    if (isSuccess(result) && result.data) {
      const rows = result.data.map(convertToSkuRow)
      // 填充选中状态
      selectedKeys.value = rows.map(r => r.id)
      rows.forEach(row => {
        selectedMap.value[row.id] = row
      })
    }
  } catch (e) {
    console.error('加载初始选中数据失败', e)
  } finally {
    initialLoading.value = false
  }
}

// 监听弹窗打开
watch(
  () => props.open,
  async val => {
    if (val) {
      // 重置选中状态
      selectedKeys.value = []
      selectedMap.value = {}
      // 重置搜索条件
      searchForm.skuCode = undefined
      searchForm.skuName = undefined
      searchParams = {}

      // 如果有初始选中值，先加载
      if (props.initialSelectedCodes && props.initialSelectedCodes.length > 0) {
        await loadInitialSelection(props.initialSelectedCodes)
      }

      // 刷新表格
      nextTick(() => {
        tableRef.value?.actionRef?.reload(true)
      })
    }
  }
)

const columns: ProColumns[] = [
  {
    title: '主图',
    dataIndex: 'mainImage',
    width: 80,
    customRender: ({ record }: { record: SkuRow }) => {
      const src = record.mainImage ? `${record.mainImage}` : ''
      if (!src) {
        return h(
          'div',
          {
            style:
              'width:48px;height:48px;background:#f5f5f5;border-radius:4px;display:flex;align-items:center;justify-content:center;color:#999;font-size:12px;'
          },
          '无图'
        )
      }
      return h('img', {
        src,
        style: 'width:48px;height:48px;object-fit:cover;border-radius:4px;border:1px solid #f0f0f0;'
      })
    }
  },
  { title: 'SKU编码', dataIndex: 'skuCode', width: 160 },
  { title: '中文名', dataIndex: 'chineseName', width: 180 },
  {
    title: '品类全路径',
    dataIndex: 'categoryFullPath',
    customRender: ({ record }: { record: SkuRow }) => record.categoryFullPath || '-'
  }
]

const tableRequest: TableRequest = async (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  const query = { ...searchParams, erpTenantId: props.erpTenantId }
  const result = await pageSkuForSelect(pageParam, query)
  // 转换数据格式
  if (isSuccess(result) && result.data) {
    const records = result.data.records.map(convertToSkuRow)
    return {
      ...result,
      data: {
        ...result.data,
        records
      }
    }
  }
  return result
}

const rowSelection = reactive({
  type: computed(() => (isMultiple.value ? 'checkbox' : 'radio')),
  selectedRowKeys: selectedKeys,
  preserveSelectedRowKeys: true,
  onChange: (keys: number[], rows: SkuRow[]) => {
    selectedKeys.value = keys
    // 更新缓存（仅更新当前页有数据的行）
    rows.forEach(row => {
      if (row) {
        selectedMap.value[row.id] = row
      }
    })
  }
})

const trim = (field: 'skuCode' | 'skuName') => {
  if (searchForm[field]) searchForm[field] = searchForm[field]!.trim()
}

const buildParams = () => {
  const p: Record<string, string> = {}
  if (searchForm.skuCode?.trim()) p.skuCode = searchForm.skuCode.trim()
  if (searchForm.skuName?.trim()) p.skuName = searchForm.skuName.trim()
  return p
}

const doSearch = () => {
  searchParams = buildParams()
  tableRef.value?.actionRef?.reload(true)
}

const resetSearch = () => {
  searchForm.skuCode = undefined
  searchForm.skuName = undefined
  doSearch()
}

const handleOk = () => {
  confirmLoading.value = true
  const ids = selectedKeys.value

  if (!ids.length) {
    confirmLoading.value = false
    emits('update:open', false)
    return
  }

  // 从缓存中取出完整行数据
  const rows = ids.map(id => selectedMap.value[id]).filter((r): r is SkuRow => !!r)

  emits('confirm', rows)
  confirmLoading.value = false
  emits('update:open', false)
}

const handleCancel = () => {
  emits('update:open', false)
}

// 暴露方法供父组件调用
defineExpose({
  /** 获取当前选中的行数据 */
  getSelectedRows: () => selectedKeys.value.map(id => selectedMap.value[id]).filter(Boolean),
  /** 刷新表格 */
  reload: () => tableRef.value?.actionRef?.reload(true)
})
</script>
