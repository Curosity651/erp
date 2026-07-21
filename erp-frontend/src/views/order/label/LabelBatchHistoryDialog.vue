<template>
  <a-modal
    v-model:open="visibleProxy"
    width="1200px"
    :footer="null"
    :body-style="modalBodyStyle"
    style="top: 40px"
    @cancel="handleCancel"
  >
    <!-- 自定义标题 -->
    <template #title>
      <div class="modal-title">
        <!-- 详情视图显示返回按钮 -->
        <a-button
          v-if="view === 'detail'"
          type="link"
          class="back-button"
          @click="handleBackToList"
        >
          <template #icon>
            <LeftOutlined />
          </template>
          返回列表
        </a-button>
        <!-- 标题文字 -->
        <span class="title-text">{{ modalTitle }}</span>
      </div>
    </template>

    <!-- 列表视图 -->
    <div v-if="view === 'list'">
      <LabelBatchList :platform="platform" :open="visibleProxy" @select="handleSelectBatch" />
    </div>

    <!-- 详情视图 -->
    <div v-else-if="view === 'detail'">
      <LabelBatchResult :batch="batchDetail" :loading="detailLoading" />
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { LeftOutlined } from '@ant-design/icons-vue'
import LabelBatchList from '@/views/order/label/LabelBatchList.vue'
import LabelBatchResult from '@/views/order/label/LabelBatchResult.vue'
import { getBatchDetail } from '@/api/order/label-batch'
import type { LabelBatchVO } from '@/api/order/label-batch'

// Props 定义
interface Props {
  open: boolean
  platform: 'Wildberries' | 'Ozon' | 'yandex'
}

const props = defineProps<Props>()

// Emits 定义
interface Emits {
  (e: 'update:open', value: boolean): void
}

const emit = defineEmits<Emits>()

// 状态管理
const visibleProxy = ref(false)
const view = ref<'list' | 'detail'>('list')
const selectedBatchId = ref<number | null>(null)
const batchDetail = ref<LabelBatchVO | null>(null)
const detailLoading = ref(false)

// 计算属性
const modalTitle = computed(() => {
  if (view.value === 'list') {
    return `${props.platform} 打印历史`
  }
  return `批次详情 - ${batchDetail.value?.batchNo || ''}`
})

// Modal body 样式
const modalBodyStyle = computed(() => {
  return { padding: '20px 24px' }
})

// 监听 visible 变化
watch(
  () => props.open,
  val => {
    visibleProxy.value = val
    if (val) {
      // 打开对话框时重置为列表视图
      view.value = 'list'
      selectedBatchId.value = null
      batchDetail.value = null
    }
  }
)

watch(visibleProxy, val => {
  emit('update:open', val)
})

// 处理选择批次
async function handleSelectBatch(batchId: number) {
  selectedBatchId.value = batchId
  detailLoading.value = true

  try {
    const resp = await getBatchDetail(batchId)
    batchDetail.value = resp.data
    view.value = 'detail'
  } catch (error: any) {
    message.error('加载批次详情失败：' + (error.message || '未知错误'))
  } finally {
    detailLoading.value = false
  }
}

// 处理返回列表
function handleBackToList() {
  view.value = 'list'
  selectedBatchId.value = null
  batchDetail.value = null
}

// 处理取消
function handleCancel() {
  visibleProxy.value = false
}
</script>

<style scoped>
.modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-button {
  padding: 0;
  height: auto;
  font-size: 14px;
}

.back-button :deep(.anticon) {
  font-size: 14px;
}

.title-text {
  font-size: 16px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}
</style>
