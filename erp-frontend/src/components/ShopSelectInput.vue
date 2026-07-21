<template>
  <div class="shop-select-input">
    <a-input-group compact>
      <!-- 单选回显：文本输入框 -->
      <a-input
        v-if="!isMultiple"
        :value="displayText"
        :placeholder="placeholder"
        readonly
        style="width: calc(100% - 63px)"
      >
        <template #suffix>
          <close-circle-filled v-if="hasValue" class="clear-icon" @click="clearSelection" />
        </template>
      </a-input>

      <!-- 多选回显：使用 a-select 多选模式，支持单个删除 -->
      <a-select
        v-else
        :value="multipleValueForView"
        mode="multiple"
        :open="false"
        :placeholder="placeholder"
        style="width: calc(100% - 63px)"
        allow-clear
        @change="handleViewChange"
      >
        <a-select-option v-for="opt in multipleOptionsForView" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </a-select-option>
      </a-select>

      <a-button type="default" @click="openModal">选择</a-button>
    </a-input-group>

    <a-modal :open="visible" title="选择店铺" :width="950" @ok="confirm" @cancel="close">
      <a-form layout="inline" style="margin-bottom: 8px">
        <a-form-item label="关键字">
          <a-input
            v-model:value="search.keyword"
            placeholder="名称/店铺ID"
            allow-clear
            @blur="trim('keyword')"
            @press-enter="doSearch"
          />
        </a-form-item>
        <a-form-item v-if="!hidePlatformFilter" label="平台">
          <PlatformSelect
            v-model:value="search.platform"
            width="160px"
            placeholder="平台"
            allow-clear
          />
        </a-form-item>
        <a-button type="primary" @click="doSearch">搜索</a-button>
        <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
      </a-form>
      <pro-table
        ref="tableRef"
        size="small"
        row-key="id"
        :columns="columns"
        :request="tableRequest"
        :row-selection="rowSelection"
        :scroll="{ x: 700 }"
        :pagination="{ pageSize: 10 }"
        :card-props="{ bodyStyle: { padding: 0 } }"
        :table-alert-render="false"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import { PlatformSelect } from '@/components/Platform'
import { listShops, getShop } from '@/api/shop'
import type { ShopVO } from '@/api/shop/types'
import { mergePageParam } from '@/utils/page-utils'

const props = withDefaults(
  defineProps<{
    /**
     * 当前选中店铺：
     * - 单选模式下：number | undefined
     * - 多选模式下：number[]
     */
    modelValue?: number | number[] | undefined
    /** 是否多选，默认为 false（单选） */
    multiple?: boolean
    placeholder?: string
    /** 默认平台筛选（如 'ozon'），设置后自动应用 */
    defaultPlatform?: string
    /** 是否隐藏平台筛选器 */
    hidePlatformFilter?: boolean
  }>(),
  {
    placeholder: '请选择店铺',
    modelValue: undefined,
    multiple: false,
    defaultPlatform: undefined,
    hidePlatformFilter: false
  }
)

const emits = defineEmits<{
  (e: 'update:modelValue', v: number | number[] | undefined): void
  (e: 'shop-selected', shop: ShopVO | ShopVO[]): void
}>()

// state
const visible = ref(false)
// 统一使用数组维护选中的 ID，支持跨页多选
const selectedKeys = ref<number[]>([])
const search = reactive<{ keyword?: string; platform?: string }>({})
const tableRef = ref<ProTableInstanceExpose>()
let searchParams: any = {}

// cache: id -> erpShopName
const labelMap = ref<Record<number, string>>({})
// 全量缓存已选中的店铺数据，避免仅依赖当前页数据
const selectedMap = ref<Record<number, ShopVO>>({})
const loadingIds = new Set<number>()

const isMultiple = computed(() => !!props.multiple)

const hasValue = computed(() => {
  if (isMultiple.value) {
    return Array.isArray(props.modelValue) && props.modelValue.length > 0
  }
  return typeof props.modelValue === 'number'
})

// 单选展示文本
const displayText = computed(() => {
  if (!hasValue.value || isMultiple.value) return ''
  const id = props.modelValue as number
  return labelMap.value[id] ?? ''
})

// 多选模式用于回显的 value/option 列表（用 a-select 渲染）
const multipleValueForView = computed(() => {
  if (!isMultiple.value || !Array.isArray(props.modelValue)) return []
  return props.modelValue as number[]
})

const multipleOptionsForView = computed(() => {
  if (!isMultiple.value || !Array.isArray(props.modelValue)) return []
  const ids = props.modelValue as number[]
  return ids.map(id => ({ value: id, label: labelMap.value[id] ?? String(id) }))
})

// 根据外部 v-model 同步内部选中 keys，并按需加载名称
watch(
  () => props.modelValue,
  async val => {
    if (!val || (Array.isArray(val) && val.length === 0)) {
      selectedKeys.value = []
      return
    }

    const ids = Array.isArray(val) ? val : [val]
    selectedKeys.value = [...new Set(ids)]

    for (const id of ids) {
      if (!(id in labelMap.value)) {
        await fetchLabelById(id)
      }
    }
  },
  { immediate: true }
)

const openModal = () => {
  visible.value = true
  // 应用默认平台筛选
  if (props.defaultPlatform) {
    search.platform = props.defaultPlatform
    searchParams = buildParams()
  }
  nextTick(() => tableRef.value?.actionRef?.reload(true))
}
const close = () => {
  visible.value = false
}
const clearSelection = () => {
  selectedKeys.value = []
  selectedMap.value = {}
  emits('update:modelValue', isMultiple.value ? [] : undefined)
}

// 仅用于多选时在 a-select 上删除单个 tag 时同步到内部选中
const handleViewChange = (values: number[]) => {
  if (!isMultiple.value) return
  const uniqueIds = [...new Set(values || [])]
  selectedKeys.value = uniqueIds
  emits('update:modelValue', uniqueIds)
}
const trim = (field: 'keyword') => {
  if (search[field]) search[field] = search[field]!.trim()
}

const rowSelection = reactive({
  type: computed(() => (isMultiple.value ? 'checkbox' : 'radio')),
  selectedRowKeys: selectedKeys,
  preserveSelectedRowKeys: true, // 添加这一行！
  onChange: (k: any[], rows: ShopVO[]) => {
    const ids = k as number[]
    selectedKeys.value = ids

    // 更新当前页勾选到 selectedMap 中，支持跨页保留完整数据
    const currentRows: ShopVO[] = rows || []
    currentRows.forEach(row => {
      selectedMap.value[row.id] = row
      if (row.erpShopName) {
        labelMap.value[row.id] = row.erpShopName
      }
    })
  }
})
const buildParams = () => {
  const p: any = {}
  if (search.keyword?.trim()) p.keyword = search.keyword.trim()
  if (search.platform) p.platform = search.platform
  return p
}
const doSearch = () => {
  searchParams = buildParams()
  tableRef.value?.actionRef?.reload(true)
}
const resetSearch = () => {
  search.keyword = undefined
  search.platform = props.defaultPlatform || undefined
  doSearch()
}
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return listShops({ ...pageParam, ...searchParams })
}
const columns: ProColumns[] = [
  { title: 'ID', dataIndex: 'id', width: 50 },
  { title: '平台', dataIndex: 'platform', width: 80 },
  { title: 'Erp店铺名称', dataIndex: 'erpShopName', width: 120 },
  { title: '平台店铺名称', dataIndex: 'name', width: 120 },
  { title: '店铺ID', dataIndex: 'platformShopId' },
  { title: '状态', dataIndex: 'status', width: 50 }
]
const confirm = () => {
  const ids = selectedKeys.value

  if (!ids.length) {
    // 未选择时不做任何修改，直接关闭
    close()
    return
  }

  // 从缓存中取出完整行数据，可能部分为 undefined
  const rows = ids.map(id => selectedMap.value[id]).filter((r): r is ShopVO => !!r)

  if (isMultiple.value) {
    emits('update:modelValue', ids)
    emits('shop-selected', rows)
  } else {
    emits('update:modelValue', ids[0])
    emits('shop-selected', rows[0] ?? ({} as ShopVO))
  }

  close()
}

async function fetchLabelById(id: number) {
  if (loadingIds.has(id)) return
  loadingIds.add(id)
  try {
    const res = await getShop(id)
    // 兼容不同响应包装：ApiResult -> { data: { ... } } 或直接对象
    const payload = res as any
    const data = payload?.data?.data ?? payload?.data ?? payload
    if (data && typeof data.erpShopName === 'string') {
      labelMap.value[id] = data.erpShopName
    }
  } catch (e) {
    // 静默失败，避免影响用户流程
    // 可根据需要加入全局提示
  } finally {
    loadingIds.delete(id)
  }
}
</script>

<style scoped>
.shop-select-input {
  width: 100%;
}

.clear-icon {
  color: rgba(0, 0, 0, 0.25);
  font-size: 12px;
  cursor: pointer;
  transition: color 0.3s;
}

.clear-icon:hover {
  color: rgba(0, 0, 0, 0.45);
}
</style>
