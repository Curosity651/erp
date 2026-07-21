<template>
  <!-- 下载失败弹窗 -->
  <a-modal
    v-model:open="errorModalVisible"
    :title="
      errorModalData.errors.length === errorModalData.total ? '文件下载失败' : '部分文件下载失败'
    "
    :width="520"
    :footer="null"
    @cancel="errorModalVisible = false"
  >
    <div class="error-modal-content">
      <p class="error-summary">
        共 {{ errorModalData.total }} 个文件，成功
        <span class="success-text">{{ errorModalData.total - errorModalData.errors.length }}</span>
        个，失败
        <span class="error-text">{{ errorModalData.errors.length }}</span>
        个。
      </p>
      <!-- 取消原因说明 -->
      <p v-if="errorModalData.cancelled && errorModalData.cancelReason" class="error-tip">
        <a-alert type="info" show-icon :message="errorModalData.cancelReason" />
      </p>
      <p v-if="errorModalData.total > 1" class="error-tip">
        <a-alert
          type="warning"
          show-icon
          message="由于存在失败文件，ZIP 压缩包未生成，请检查失败原因后重试。"
        />
      </p>
      <div class="error-list-title">失败文件列表：</div>
      <ul class="error-file-list">
        <li v-for="(e, idx) in errorModalData.errors" :key="idx">
          <span class="error-file-name">{{ e.name }}</span>
          <span class="error-file-separator">:</span>
          <span class="error-file-msg">{{ e.message }}</span>
        </li>
      </ul>
      <div class="error-modal-footer">
        <a-button type="primary" @click="errorModalVisible = false">知道了</a-button>
      </div>
    </div>
  </a-modal>

  <div v-if="loading" class="loading-container">
    <a-spin size="large" />
  </div>
  <div v-else-if="batch" class="label-batch-result">
    <!-- 批次摘要 -->
    <div class="summary-section">
      <a-descriptions :column="{ xs: 1, sm: 2, md: 3 }" size="small" bordered>
        <a-descriptions-item label="批次号">
          <span class="batch-no">{{ batch.batchNo || '-' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="平台">
          <a-tag color="blue">{{ batch.platform || '-' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="订单/文件">
          <span class="stat-text">{{ batch.totalOrders || 0 }} / {{ batch.totalFiles || 0 }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="成功">
          <span class="success-count">{{ batch.successCount || 0 }}</span>
          <span class="success-rate">({{ calculateSuccessRate() }}%)</span>
        </a-descriptions-item>
        <a-descriptions-item label="失败">
          <span class="failed-count">{{ batch.failedCount || 0 }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColor(batch.status)">{{ formatStatus(batch.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item v-if="batch.createdBy" label="创建人">
          {{ batch.createdBy }}
        </a-descriptions-item>
        <a-descriptions-item v-if="batch.createTime" label="创建时间" :span="2">
          {{ batch.createTime }}
        </a-descriptions-item>
      </a-descriptions>
    </div>

    <!-- 失败订单列表 -->
    <div v-if="(batch.failedItems || []).length" class="failed-section">
      <a-alert type="error" show-icon>
        <template #message>
          <span class="failed-title">失败订单 ({{ batch.failedItems?.length || 0 }})</span>
        </template>
        <template #description>
          <div class="failed-list">
            <div v-for="(item, idx) in batch.failedItems" :key="idx" class="failed-item">
              <a-tag color="error" size="small">{{ item.platform }}</a-tag>
              <span class="order-id">{{ item.platformOrderId || '-' }}</span>
              <span v-if="item.skuCode" class="sku">{{ item.skuCode }}</span>
              <span class="divider">|</span>
              <span class="error-msg">{{ item.errorMsg || '未知错误' }}</span>
            </div>
          </div>
        </template>
      </a-alert>
    </div>

    <!-- 文件列表 -->
    <div class="files-section">
      <div class="files-header">
        <div class="files-title">
          <span>文件列表</span>
          <a-badge :count="batch.totalFiles || 0" :number-style="{ backgroundColor: '#1890ff' }" />
        </div>
        <div class="files-actions">
          <a-button
            type="primary"
            size="small"
            :loading="allUi.downloading"
            :disabled="!anyDownloadable || allUi.downloading"
            @click="handleDownloadAllZip"
          >
            <template #icon>
              <DownloadOutlined />
            </template>
            全部打包下载
          </a-button>
          <span v-if="allUi.downloading" class="progress-text">
            {{ allUi.done }}/{{ allUi.total }}
            <span v-if="allUi.failed > 0" class="progress-failed"> (失败 {{ allUi.failed }}) </span>
          </span>
          <a-tag v-else-if="allUi.errors.length > 0" color="error"> 下载失败 </a-tag>
          <a-tag
            v-else-if="allUi.done && allUi.done === allUi.total && allUi.total > 0"
            color="success"
          >
            已完成
          </a-tag>
        </div>
      </div>

      <template v-if="(batch.files || []).length">
        <a-collapse v-model:active-key="activeGroupKeys" class="files-collapse">
          <a-collapse-panel v-for="g in groupList" :key="g.key">
            <template #header>
              <div class="group-header">
                <span class="group-name">{{ g.name }}</span>
                <div class="group-actions" @click.stop>
                  <a-button
                    type="link"
                    size="small"
                    :loading="groupUi[g.key]?.downloading"
                    :disabled="g.downloadableCount === 0 || groupUi[g.key]?.downloading"
                    @click="handleDownloadGroupZip(g)"
                  >
                    <template #icon>
                      <DownloadOutlined />
                    </template>
                    打包下载
                  </a-button>
                  <span v-if="groupUi[g.key]?.downloading" class="progress-text">
                    {{ groupUi[g.key]?.done }}/{{ groupUi[g.key]?.total }}
                    <span v-if="groupUi[g.key]?.failed > 0" class="progress-failed">
                      (失败 {{ groupUi[g.key]?.failed }})
                    </span>
                  </span>
                  <a-tag v-else-if="groupUi[g.key]?.errors.length > 0" color="error" size="small">
                    下载失败
                  </a-tag>
                  <a-tag
                    v-else-if="
                      groupUi[g.key]?.done &&
                      groupUi[g.key]?.done === groupUi[g.key]?.total &&
                      groupUi[g.key]?.total > 0
                    "
                    color="success"
                    size="small"
                  >
                    已完成
                  </a-tag>
                </div>
              </div>
            </template>

            <div class="file-list">
              <div
                v-for="(item, idx) in g.files"
                :key="idx"
                class="file-item"
                :title="item.fileName || item.objectKey"
              >
                <span class="file-icon">📄</span>
                <span class="file-name">{{ item.fileName || item.objectKey }}</span>
                <a
                  v-if="fileDownloader.canDownload(item)"
                  class="file-download"
                  @click.prevent.stop="handleDownloadSingle(item)"
                >
                  下载
                </a>
                <span v-else class="file-disabled">不可下载</span>
              </div>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </template>
      <div v-else class="empty-hint">
        <a-empty description="未生成任何文件" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
      </div>
    </div>
  </div>
  <div v-else class="empty-container">
    <a-empty description="暂无数据" />
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, watch, ref } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { Empty } from 'ant-design-vue'
import type { LabelBatchVO, LabelBatchFileVO } from '@/api/order/label-batch'
import {
  useLabelFileDownload,
  type GroupItem,
  type GroupError,
  type DownloadResult
} from '@/hooks/use-label-file-download'

// Props 定义
interface Props {
  /** 批次数据 */
  batch: LabelBatchVO | null
  /** 是否显示加载状态 */
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

// 使用文件下载组合式函数
const fileDownloader = useLabelFileDownload()

// 分组 UI 状态
interface GroupUi {
  downloading: boolean
  total: number
  done: number
  failed: number
  errors: GroupError[]
  cancelled: boolean
  cancelReason?: string
}

const groupUi = reactive<Record<string, GroupUi>>({})

// 全部下载 UI 状态
const allUi = reactive<{
  downloading: boolean
  total: number
  done: number
  failed: number
  errors: GroupError[]
  cancelled: boolean
  cancelReason?: string
}>({
  downloading: false,
  total: 0,
  done: 0,
  failed: 0,
  errors: [],
  cancelled: false
})

// 折叠面板激活的 key
const activeGroupKeys = ref<string[]>([])

// 错误弹窗状态
const errorModalVisible = ref(false)
const errorModalData = reactive<{
  total: number
  errors: GroupError[]
  cancelled: boolean
  cancelReason?: string
}>({
  total: 0,
  errors: [],
  cancelled: false
})

// 根据 destinationWarehouseId 分组文件
const groupKeyOf = (f: LabelBatchFileVO) =>
  (f.destinationWarehouseId && f.destinationWarehouseId.trim()) || '未分配仓库'

const groupList = computed<GroupItem[]>(() => {
  const files = (props.batch?.files || []) as LabelBatchFileVO[]
  const map = new Map<string, LabelBatchFileVO[]>()

  for (const f of files) {
    const k = groupKeyOf(f)
    if (!map.has(k)) map.set(k, [])
    map.get(k)!.push(f)
  }

  const list: GroupItem[] = []
  map.forEach((arr, key) => {
    list.push({
      key,
      name: arr[0].destinationWarehouseName || key,
      files: arr,
      downloadableCount: arr.filter(f => fileDownloader.canDownload(f)).length
    })
  })

  return list
})

// 是否有可下载的文件
const anyDownloadable = computed(() => groupList.value.some(g => g.downloadableCount > 0))

// 监听分组列表变化，初始化 UI 状态
watch(
  groupList,
  list => {
    const newKeys = new Set(list.map(g => g.key))
    for (const g of list) {
      if (!groupUi[g.key]) {
        groupUi[g.key] = {
          downloading: false,
          total: 0,
          done: 0,
          failed: 0,
          errors: [],
          cancelled: false
        }
      }
    }
    // 清理已不存在的 key
    Object.keys(groupUi).forEach(k => {
      if (!newKeys.has(k)) delete groupUi[k]
    })
    // 默认展开所有分组
    activeGroupKeys.value = list.map(g => g.key)
  },
  { immediate: true }
)

// 状态颜色映射
function statusColor(s?: string) {
  if (!s) return 'default'
  const k = s.toUpperCase()
  if (k === 'GENERATED') return 'success'
  if (k === 'PARTIAL') return 'warning'
  if (k === 'FAILED') return 'error'
  return 'default'
}

// 格式化状态文本
function formatStatus(s?: string) {
  if (!s) return '-'
  const k = s.toUpperCase()
  if (k === 'GENERATED') return '全部成功'
  if (k === 'PARTIAL') return '部分成功'
  if (k === 'FAILED') return '全部失败'
  return s
}

// 计算成功率
function calculateSuccessRate(): number {
  if (!props.batch) return 0
  const success = props.batch.successCount || 0
  const failed = props.batch.failedCount || 0
  const total = success + failed
  if (total === 0) return 0
  return Math.round((success / total) * 100)
}

// 下载单个文件
async function handleDownloadSingle(file: LabelBatchFileVO) {
  try {
    await fileDownloader.downloadSingle(file)
  } catch (e: any) {
    // 单文件下载失败时显示错误弹窗
    showDownloadErrorModal(
      [
        {
          name: file.fileName || file.objectKey || 'unknown',
          message: e?.message || '下载失败',
          file
        }
      ],
      1
    )
  }
}

// 显示下载失败弹窗
function showDownloadErrorModal(
  errors: GroupError[],
  total: number,
  cancelled = false,
  cancelReason?: string
) {
  errorModalData.total = total
  errorModalData.errors = errors
  errorModalData.cancelled = cancelled
  errorModalData.cancelReason = cancelReason
  errorModalVisible.value = true
}

/**
 * 创建下载回调函数（减少代码重复）
 * @param ui UI 状态对象
 */
function createDownloadCallbacks(ui: GroupUi) {
  return {
    onProgress: () => (ui.done += 1),
    onError: (f: LabelBatchFileVO, idx: number, err: any) => {
      ui.failed += 1
      ui.errors.push({
        name: f.fileName || f.objectKey || `file-${idx}.pdf`,
        message: err?.message || '下载失败',
        file: f
      })
    },
    onFinally: (result: DownloadResult) => {
      ui.downloading = false
      ui.cancelled = result.cancelled
      ui.cancelReason = result.cancelReason
      // 下载完成后，如有失败则弹窗提示
      if (ui.errors.length > 0) {
        showDownloadErrorModal(ui.errors, ui.total, result.cancelled, result.cancelReason)
      }
    }
  }
}

// 下载分组 ZIP
async function handleDownloadGroupZip(g: GroupItem) {
  const ui = groupUi[g.key]
  if (!ui || ui.downloading) return

  // 初始化 UI 状态
  ui.downloading = true
  ui.total = g.downloadableCount
  ui.done = 0
  ui.failed = 0
  ui.errors = []
  ui.cancelled = false
  ui.cancelReason = undefined

  const { onProgress, onError, onFinally } = createDownloadCallbacks(ui)

  await fileDownloader.downloadGroupZip(
    g,
    props.batch?.platform,
    props.batch?.batchNo,
    onProgress,
    onError,
    onFinally
  )
}

// 下载全部 ZIP
async function handleDownloadAllZip() {
  if (allUi.downloading) return

  // 初始化 UI 状态
  allUi.downloading = true
  allUi.total = groupList.value.reduce((sum, g) => sum + g.downloadableCount, 0)
  allUi.done = 0
  allUi.failed = 0
  allUi.errors = []
  allUi.cancelled = false
  allUi.cancelReason = undefined

  const { onProgress, onError, onFinally } = createDownloadCallbacks(allUi)

  await fileDownloader.downloadAllZip(
    groupList.value,
    props.batch?.platform,
    props.batch?.batchNo,
    onProgress,
    onError,
    onFinally
  )
}
</script>

<style scoped>
.loading-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.label-batch-result {
  width: 100%;
}

/* 批次摘要区域 */
.summary-section {
  margin-bottom: 16px;
}

.batch-no {
  font-family: 'Courier New', monospace;
  color: #1890ff;
  font-weight: 500;
}

.stat-text {
  font-weight: 600;
  font-size: 14px;
}

.success-count {
  color: #52c41a;
  font-weight: 600;
  font-size: 16px;
}

.success-rate {
  color: #52c41a;
  margin-left: 4px;
  font-size: 13px;
}

.failed-count {
  color: #ff4d4f;
  font-weight: 600;
  font-size: 16px;
}

/* 失败订单区域 */
.failed-section {
  margin-bottom: 16px;
}

.failed-title {
  font-weight: 600;
}

.failed-list {
  margin-top: 8px;
}

.failed-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #ffe7e7;
  flex-wrap: wrap;
}

.failed-item:last-child {
  border-bottom: none;
}

.order-id {
  font-weight: 500;
  color: #262626;
}

.sku {
  color: #1890ff;
  font-weight: 500;
}

.divider {
  color: #d9d9d9;
}

.error-msg {
  color: #cf1322;
  flex: 1;
}

/* 文件列表区域 */
.files-section {
  margin-top: 16px;
}

.files-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 12px;
}

.files-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.files-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-text {
  color: #666;
  font-size: 13px;
}

.progress-failed {
  color: #ff4d4f;
  margin-left: 4px;
}

.empty-hint {
  padding: 24px;
  text-align: center;
}

/* 折叠面板样式 */
.files-collapse {
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 2px;
}

.files-collapse :deep(.ant-collapse-item) {
  border-bottom: 1px solid #d9d9d9;
}

.files-collapse :deep(.ant-collapse-item:last-child) {
  border-bottom: none;
}

.files-collapse :deep(.ant-collapse-header) {
  padding: 12px 16px !important;
  background: #fafafa;
}

.files-collapse :deep(.ant-collapse-content) {
  background: #fff;
  border-top: 1px solid #d9d9d9;
}

.files-collapse :deep(.ant-collapse-content-box) {
  padding: 0;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
}

.group-name {
  font-weight: 500;
  flex: 1;
}

.group-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 文件列表样式 */
.file-list {
  background: #fff;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.3s;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item:hover {
  background-color: #f5f5f5;
}

.file-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.file-name {
  flex: 1;
  font-size: 14px;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-download {
  color: #1890ff;
  cursor: pointer;
  font-size: 14px;
  flex-shrink: 0;
}

.file-download:hover {
  color: #40a9ff;
}

.file-disabled {
  color: #bfbfbf;
  font-size: 14px;
  flex-shrink: 0;
}

/* 错误弹窗样式 */
.error-modal-content {
  padding: 8px 0;
}

.error-summary {
  margin-bottom: 12px;
  font-size: 14px;
}

.error-summary .success-text {
  color: #52c41a;
  font-weight: 600;
}

.error-summary .error-text {
  color: #ff4d4f;
  font-weight: 600;
}

.error-list-title {
  font-weight: 500;
  margin-bottom: 8px;
}

.error-file-list {
  max-height: 200px;
  overflow: auto;
  padding-left: 20px;
  margin: 0 0 16px 0;
}

.error-file-list li {
  margin-bottom: 4px;
  word-break: break-all;
}

.error-file-name {
  color: #262626;
}

.error-file-separator {
  color: #999;
  margin: 0 4px;
}

.error-file-msg {
  color: #ff4d4f;
}

.error-modal-footer {
  text-align: right;
}
</style>
