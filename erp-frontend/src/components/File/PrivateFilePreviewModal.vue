<template>
  <a-modal
    v-model:open="internalVisible"
    :title="`预览文件 - ${fileName}`"
    :width="modalWidth"
    :footer="null"
    :destroy-on-close="true"
    centered
    class="private-file-preview-modal"
    :body-style="{ padding: 0 }"
    @cancel="handleClose"
  >
    <div class="preview-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="preview-loading">
        <a-spin size="large" />
        <p>正在加载文件...</p>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="preview-error">
        <a-result status="error" :title="error" sub-title="您可以尝试下载文件到本地查看">
          <template #extra>
            <a-button type="primary" @click="handleDownload">
              <download-outlined /> 下载文件
            </a-button>
          </template>
        </a-result>
      </div>

      <!-- 不支持预览 -->
      <div v-else-if="fileType === 'unsupported'" class="preview-unsupported">
        <a-result
          status="warning"
          title="暂不支持预览此文件类型"
          sub-title="您可以下载文件到本地查看"
        >
          <template #extra>
            <a-button type="primary" @click="handleDownload">
              <download-outlined /> 下载文件
            </a-button>
          </template>
        </a-result>
      </div>

      <!-- 预览内容 -->
      <div v-else-if="fileUrl" class="preview-content">
        <!-- 图片预览 -->
        <div v-if="fileType === 'image'" class="preview-image">
          <img :src="fileUrl" :alt="fileName" @load="handleImageLoad" @error="handleImageError" />
          <!-- 图片加载中的覆盖层 -->
          <div v-if="contentLoading" class="content-loading-overlay">
            <a-spin size="large" />
          </div>
        </div>

        <!-- PDF 预览 -->
        <div v-else-if="fileType === 'pdf'" class="preview-pdf">
          <vue-office-pdf
            :key="fileUrl"
            :src="fileUrl"
            class="office-viewer"
            @rendered="handleContentRendered"
            @error="handleOfficeError"
          />
          <!-- PDF加载中的覆盖层 -->
          <div v-if="contentLoading" class="content-loading-overlay">
            <a-spin size="large" />
            <p>正在渲染PDF...</p>
          </div>
        </div>

        <!-- Word 预览 -->
        <div v-else-if="fileType === 'word'" class="preview-word">
          <vue-office-docx
            :key="fileUrl"
            :src="fileUrl"
            class="office-viewer"
            @rendered="handleContentRendered"
            @error="handleOfficeError"
          />
          <!-- Word加载中的覆盖层 -->
          <div v-if="contentLoading" class="content-loading-overlay">
            <a-spin size="large" />
            <p>正在渲染文档...</p>
          </div>
        </div>

        <!-- Excel 预览 -->
        <div v-else-if="fileType === 'excel'" class="preview-excel">
          <vue-office-excel
            :key="fileUrl"
            :src="fileUrl"
            class="office-viewer"
            @rendered="handleContentRendered"
            @error="handleOfficeError"
          />
          <!-- Excel加载中的覆盖层 -->
          <div v-if="contentLoading" class="content-loading-overlay">
            <a-spin size="large" />
            <p>正在渲染表格...</p>
          </div>
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div v-if="!loading && !error && fileUrl" class="preview-footer">
        <a-button @click="handleDownload"> <download-outlined /> 下载 </a-button>
        <a-button @click="handleClose">关闭</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import VueOfficePdf from '@vue-office/pdf'
import VueOfficeDocx from '@vue-office/docx'
import VueOfficeExcel from '@vue-office/excel'
import '@vue-office/docx/lib/index.css'
import '@vue-office/excel/lib/index.css'
import type { FilePreviewType } from '@/hooks/use-file-preview'

defineOptions({ name: 'PrivateFilePreviewModal' })

interface Props {
  open: boolean
  fileId: number | null
  fileName: string
  fileType: FilePreviewType
  fileUrl: string | null
  loading: boolean
  error: string | null
}

const props = defineProps<Props>()

const emits = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'close'): void
  (e: 'download'): void
}>()

// 内部可见状态（用于 v-model）
const internalVisible = computed({
  get: () => props.open,
  set: (val: boolean) => emits('update:open', val)
})

// 内容加载状态（用于 Office 组件渲染）
const contentLoading = ref(false)

// 超时保护定时器
let timeoutId: ReturnType<typeof setTimeout> | null = null

// 模态框宽度根据文件类型调整
const modalWidth = computed(() => {
  switch (props.fileType) {
    case 'image':
      return 800
    case 'pdf':
    case 'word':
    case 'excel':
      return 1200
    default:
      return 500
  }
})

// 监听 fileUrl 变化，开始内容加载
watch(
  () => props.fileUrl,
  url => {
    if (url && props.fileType !== 'unsupported' && props.fileType !== 'image') {
      contentLoading.value = true
      startTimeout()
    }
  }
)

// 监听 open 变化，清理状态
watch(
  () => props.open,
  visible => {
    if (!visible) {
      contentLoading.value = false
      clearTimeout()
    }
  }
)

// 开始超时保护
const startTimeout = () => {
  clearTimeout()
  timeoutId = setTimeout(() => {
    if (contentLoading.value) {
      console.warn('文件预览加载超时')
      contentLoading.value = false
      message.warning('文件加载时间较长，如果预览失败请尝试下载查看')
    }
  }, 15000) // 15秒超时
}

// 清除超时定时器
const clearTimeout = () => {
  if (timeoutId) {
    globalThis.clearTimeout(timeoutId)
    timeoutId = null
  }
}

// 图片加载完成
const handleImageLoad = () => {
  contentLoading.value = false
}

// 图片加载错误
const handleImageError = () => {
  contentLoading.value = false
  message.error('图片加载失败，请尝试下载查看')
}

// Office 文档渲染完成
const handleContentRendered = () => {
  contentLoading.value = false
  clearTimeout()
}

// Office 文档渲染错误
const handleOfficeError = (err: Error) => {
  console.error('Office 文档渲染失败:', err)
  contentLoading.value = false
  clearTimeout()

  // 根据错误类型提供详细提示
  const errMsg = err?.message?.toLowerCase() || ''
  if (errMsg.includes('cors') || errMsg.includes('network')) {
    message.error('文档预览失败：网络或跨域问题，请尝试下载查看')
  } else if (errMsg.includes('format') || errMsg.includes('parse')) {
    message.error('文档预览失败：文件格式问题，请尝试下载查看')
  } else {
    message.error('文档预览失败，请尝试下载查看')
  }
}

// 关闭弹窗
const handleClose = () => {
  emits('close')
}

// 下载文件
const handleDownload = () => {
  emits('download')
}

// 组件卸载时清理
onUnmounted(() => {
  clearTimeout()
})
</script>

<style scoped>
.private-file-preview-modal :deep(.ant-modal-body) {
  max-height: 85vh;
  overflow: hidden;
}

.preview-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.preview-loading,
.preview-error,
.preview-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  padding: 40px;
  text-align: center;
}

.preview-loading p {
  margin-top: 16px;
  color: #8c8c8c;
}

.preview-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

/* 图片预览 */
.preview-image {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background: #f5f5f5;
  min-height: 400px;
  position: relative;
}

.preview-image img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Office 预览通用样式 */
.preview-pdf,
.preview-word,
.preview-excel {
  height: 70vh;
  overflow: hidden;
  position: relative;
  background: #f5f5f5;
}

.office-viewer {
  width: 100%;
  height: 100%;
}

/* 内容加载覆盖层 */
.content-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.9);
  z-index: 10;
}

.content-loading-overlay p {
  margin-top: 16px;
  color: #8c8c8c;
}

/* 底部操作栏 */
.preview-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

/* vue-office 样式覆盖 */
.preview-word :deep(.vue-office-docx) {
  padding: 20px;
  background: #fff;
}

.preview-excel :deep(.vue-office-excel) {
  background: #fff;
}

.preview-pdf :deep(.vue-office-pdf) {
  background: #525659;
}
</style>
