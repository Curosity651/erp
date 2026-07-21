<template>
  <div class="engineering-file-manager">
    <!-- 工程文件表格 -->
    <a-table
      :columns="engineeringFileColumns"
      :data-source="engineeringFileTableData"
      :pagination="false"
      size="middle"
      class="engineering-files-table"
    >
      <!-- 文件类型列 -->
      <template #fileTypeIcon="{ record }">
        <div class="file-type-display">
          <div class="file-type-icon" :style="{ '--icon-color': record.color }">
            <div class="icon-wrapper">
              <component :is="record.icon" />
            </div>
            <div v-if="record.fileCount > 0" class="file-count-badge">
              {{ record.fileCount }}
            </div>
          </div>
          <div class="file-type-info">
            <div class="file-type-name">{{ record.typeName }}</div>
            <div class="file-type-formats">
              <a-tag
                v-for="format in record.formats"
                :key="format"
                :color="record.tagColor"
                size="small"
              >
                {{ format }}
              </a-tag>
            </div>
          </div>
        </div>
      </template>

      <!-- 文件列表列 -->
      <template #fileList="{ record }">
        <div class="file-list-cell">
          <div v-if="(fileListMap.get(record.fileType) || []).length > 0" class="uploaded-files">
            <draggable
              :model-value="fileListMap.get(record.fileType) || []"
              item-key="uid"
              :disabled="readonly"
              class="draggable-list"
              :data-file-type="record.fileType"
              ghost-class="ghost-item"
              chosen-class="chosen-item"
              drag-class="drag-item"
              @update:model-value="
                (newList: UploadFile[]) => handleFileDragEnd(newList, record.fileType)
              "
            >
              <template #item="{ element: file }">
                <div class="file-item">
                  <div v-if="!readonly" class="drag-handle">
                    <HolderOutlined />
                  </div>
                  <span class="file-name">{{ file.name }}</span>
                  <div class="file-actions">
                    <!-- 预览按钮 - 根据文件名动态判断是否显示 -->
                    <a-button
                      v-if="isFilePreviewable(file.name)"
                      type="link"
                      size="small"
                      title="预览文件"
                      @click="() => handlePreviewFile(file)"
                    >
                      <EyeOutlined />
                    </a-button>

                    <!-- 下载按钮 -->
                    <a-button type="link" size="small" @click="() => handleDownloadFile(file)">
                      <DownloadOutlined />
                    </a-button>

                    <!-- 删除按钮 -->
                    <a-button
                      v-if="!readonly"
                      type="link"
                      size="small"
                      danger
                      @click="() => handleRemoveFile(file, record.fileType)"
                    >
                      <DeleteOutlined />
                    </a-button>
                  </div>
                </div>
              </template>
            </draggable>
          </div>
          <div v-else class="no-files">
            <span style="color: #8c8c8c">暂无文件</span>
          </div>
        </div>
      </template>

      <!-- 操作列 -->
      <template #actions="{ record }">
        <div class="action-cell">
          <!-- 隐藏的上传组件 -->
          <a-upload
            :ref="(el: any) => setUploadRef(record.fileType, el)"
            :custom-request="(options: any) => handleCustomUpload(options, record.fileType)"
            :before-upload="(file: File) => handleBeforeUpload(file, record.fileType)"
            :show-upload-list="false"
            :accept="record.accept"
            :multiple="record.multiple"
            style="display: none"
          />

          <!-- 上传按钮 -->
          <a-button
            v-if="!readonly"
            type="primary"
            size="small"
            :loading="uploadingStateMap.get(record.fileType) || false"
            class="upload-btn"
            @click="() => triggerUpload(record.fileType)"
          >
            <template #icon>
              <UploadOutlined v-if="!uploadingStateMap.get(record.fileType)" />
            </template>
            {{
              uploadingStateMap.get(record.fileType)
                ? '上传中...'
                : (fileListMap.get(record.fileType)?.length || 0) > 0
                  ? '添加文件'
                  : '上传文件'
            }}
          </a-button>

          <!-- 查看模式提示 -->
          <span v-else style="color: #8c8c8c; font-size: 12px">仅查看</span>
        </div>
      </template>
    </a-table>

    <!-- 文件预览模态框 -->
    <a-modal
      v-model:open="previewVisible"
      :title="`预览文件 - ${previewFile?.name || ''}`"
      :width="1200"
      :footer="null"
      centered
      class="file-preview-modal"
      :body-style="{ padding: 0 }"
      @cancel="handleClosePreview"
    >
      <div v-if="previewFile" class="preview-container">
        <!-- 预览内容 + 覆盖式加载态（始终渲染内容，避免事件丢失） -->
        <div class="preview-content">
          <!-- 图片预览 -->
          <div v-if="previewFile.type === 'image'" class="image-preview">
            <img
              :src="previewFile.url"
              :alt="previewFile.name"
              style="max-width: 100%; max-height: 70vh; object-fit: contain"
              @load="handleImageLoad"
              @error="handleImageError"
            />
          </div>

          <!-- PDF预览 -->
          <div v-else-if="previewFile.type === 'pdf'" class="pdf-preview">
            <VueOfficePdf
              :key="previewFile.url"
              :src="previewFile.url"
              style="width: 100%; height: 70vh"
              :options="{
                enableHandToolOnLoad: false,
                pdfOpenParams: { scrollbars: 1, toolbar: 1, statusbar: 1 }
              }"
              @rendered="handlePreviewSuccess"
              @success="handlePreviewSuccess"
              @error="handleOfficePreviewError"
            />
          </div>

          <!-- 视频预览 -->
          <div v-else-if="previewFile.type === 'video'" class="video-preview">
            <video
              :key="previewFile.url"
              :src="previewFile.url"
              controls
              style="max-width: 100%; max-height: 70vh"
              @loadstart="handleVideoLoadStart"
              @canplay="handleVideoCanPlay"
              @error="handleVideoError"
            >
              您的浏览器不支持视频播放
            </video>
          </div>

          <!-- Office文档预览 -->
          <div
            v-else-if="previewFile.type === 'docx' || previewFile.type === 'excel'"
            class="office-preview no-padding"
          >
            <!-- Word 文档预览 -->
            <div v-if="previewFile.type === 'docx'" class="docx-preview">
              <VueOfficeDocx
                :key="previewFile.url"
                :src="previewFile.url"
                style="height: 75vh; width: 100%"
                @rendered="handlePreviewSuccess"
                @success="handlePreviewSuccess"
                @error="handleOfficePreviewError"
              />
            </div>

            <!-- Excel 文档预览 -->
            <div v-else-if="previewFile.type === 'excel'" class="excel-preview">
              <VueOfficeExcel
                :key="previewFile.url"
                :src="previewFile.url"
                style="height: 75vh; width: 100%"
                @rendered="handlePreviewSuccess"
                @success="handlePreviewSuccess"
                @error="handleOfficePreviewError"
              />
            </div>
          </div>

          <!-- 不支持预览 -->
          <div v-else class="unsupported-preview">
            <a-result
              status="warning"
              title="暂不支持预览此文件类型"
              sub-title="您可以下载文件到本地查看"
            >
              <template #extra>
                <a-button type="primary" @click="handleDownloadCurrentFile"> 下载文件 </a-button>
              </template>
            </a-result>
          </div>

          <!-- 覆盖式加载层 -->
          <div v-show="previewLoading" class="preview-loading">
            <a-spin size="large" />
            <p>正在加载预览...</p>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, reactive } from 'vue'
import { message } from 'ant-design-vue'
import type { UploadFile } from 'ant-design-vue/lib/upload/interface'
import { uploadToOSSWithCache } from '@/hooks/use-oss-upload'
import type { SkuFileDTO, SkuFileVO } from '@/api/product/sku/types'
import draggable from 'vuedraggable'
// 导入 vue-office 组件
import VueOfficeDocx from '@vue-office/docx'
import VueOfficeExcel from '@vue-office/excel'
import VueOfficePdf from '@vue-office/pdf'
// 引入 vue-office 官方样式，避免样式缺失影响渲染和事件
import '@vue-office/docx/lib/index.css'
import '@vue-office/excel/lib/index.css'
import {
  UploadOutlined,
  DownloadOutlined,
  DeleteOutlined,
  HolderOutlined,
  FileTextOutlined,
  FilePdfOutlined,
  PlayCircleOutlined,
  SafetyCertificateOutlined,
  VideoCameraOutlined,
  BlockOutlined,
  FileImageOutlined,
  TableOutlined,
  ExperimentOutlined,
  FileProtectOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'

// 文件类型配置
interface FileTypeConfig {
  key: string
  fileType: string
  typeName: string
  description: string
  color: string
  tagColor: string
  icon: any
  formats: string[]
  accept: string
  multiple: boolean
  maxSize: number // 文件大小限制，单位：MB
}

// 文件类型配置数据 - 所有类型都支持多文件
const FILE_TYPE_CONFIGS: FileTypeConfig[] = [
  {
    key: 'manual',
    fileType: 'manual',
    typeName: '说明书',
    description: '产品使用说明或技术文档',
    color: '#1890ff',
    tagColor: 'blue',
    icon: FileTextOutlined,
    formats: ['.pdf', '.doc', '.docx'],
    accept: '.pdf,.doc,.docx',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'box_mark',
    fileType: 'box_mark',
    typeName: '箱唛',
    description: '产品包装箱标识文件，PDF格式',
    color: '#ff4d4f',
    tagColor: 'red',
    icon: FilePdfOutlined,
    formats: ['.pdf'],
    accept: '.pdf',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'install_video',
    fileType: 'install_video',
    typeName: '安装视频',
    description: '产品安装或使用演示视频',
    color: '#722ed1',
    tagColor: 'purple',
    icon: PlayCircleOutlined,
    formats: ['.mp4', '.avi', '.mov'],
    accept: '.mp4,.avi,.mov',
    multiple: true,
    maxSize: 2048
  },
  {
    key: 'quality_report',
    fileType: 'quality_report',
    typeName: '质检报告',
    description: '产品质量检测报告文档',
    color: '#52c41a',
    tagColor: 'green',
    icon: SafetyCertificateOutlined,
    formats: ['.pdf', '.doc', '.docx', '.xls', '.xlsx'],
    accept: '.pdf,.doc,.docx,.xls,.xlsx',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'platform_render_video',
    fileType: 'platform_render_video',
    typeName: '平台渲染视频',
    description: '用于平台展示的产品渲染动画视频',
    color: '#eb2f96',
    tagColor: 'magenta',
    icon: VideoCameraOutlined,
    formats: ['.mp4', '.avi', '.mov'],
    accept: '.mp4,.avi,.mov',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'model_3d',
    fileType: 'model_3d',
    typeName: '3D模型',
    description: '三维建模软件导出的模型文件',
    color: '#13c2c2',
    tagColor: 'cyan',
    icon: BlockOutlined,
    formats: ['.obj', '.fbx', '.3ds', '.dae'],
    accept: '.obj,.fbx,.3ds,.dae',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'engineering_drawing',
    fileType: 'engineering_drawing',
    typeName: '工程图',
    description: '产品工程图纸和技术图纸',
    color: '#faad14',
    tagColor: 'orange',
    icon: FileImageOutlined,
    formats: ['.pdf', '.dwg', '.dxf', '.jpg', '.png'],
    accept: '.pdf,.dwg,.dxf,.jpg,.png',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'bom',
    fileType: 'bom',
    typeName: '物料清单',
    description: 'Excel表格或CSV格式的物料清单',
    color: '#389e0d',
    tagColor: 'green',
    icon: TableOutlined,
    formats: ['.xls', '.xlsx', '.csv'],
    accept: '.xls,.xlsx,.csv',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'drop_test_video',
    fileType: 'drop_test_video',
    typeName: '摔箱视频',
    description: '产品包装摔箱测试视频',
    color: '#d4380d',
    tagColor: 'volcano',
    icon: ExperimentOutlined,
    formats: ['.mp4', '.avi', '.mov'],
    accept: '.mp4,.avi,.mov',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'package_manual',
    fileType: 'package_manual',
    typeName: '包装说明书',
    description: '产品包装说明文档，支持Excel、Word、PDF、ZIP格式',
    color: '#531dab',
    tagColor: 'purple',
    icon: FileProtectOutlined,
    formats: ['.xls', '.xlsx', '.doc', '.docx', '.pdf', '.zip'],
    accept: '.xls,.xlsx,.doc,.docx,.pdf,.zip',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'quotation',
    fileType: 'quotation',
    typeName: '报价单',
    description: '产品报价单文件，PDF格式',
    color: '#fa8c16',
    tagColor: 'gold',
    icon: DollarOutlined,
    formats: ['.pdf'],
    accept: '.pdf',
    multiple: true,
    maxSize: 300
  },
  {
    key: 'sample_confirmation',
    fileType: 'sample_confirmation',
    typeName: '样品确认单',
    description: '样品确认单文档，支持Excel、Word、PDF格式',
    color: '#7cb305',
    tagColor: 'lime',
    icon: CheckCircleOutlined,
    formats: ['.xls', '.xlsx', '.doc', '.docx', '.pdf'],
    accept: '.xls,.xlsx,.doc,.docx,.pdf',
    multiple: true,
    maxSize: 300
  }
]

// 文件类型名称映射
const FILE_TYPE_NAME_MAP = Object.fromEntries(
  FILE_TYPE_CONFIGS.map(config => [config.fileType, config.typeName])
)

// 预览文件类型
type PreviewType = 'image' | 'pdf' | 'video' | 'docx' | 'excel' | 'unsupported'

// 预览文件信息
interface PreviewFileInfo {
  name: string
  url: string
  type: PreviewType
}

// 根据文件扩展名判断预览类型
const getPreviewType = (fileName: string): PreviewType => {
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))

  const previewTypeMap: Record<string, PreviewType> = {
    // 图片文件
    '.jpg': 'image',
    '.jpeg': 'image',
    '.png': 'image',
    '.gif': 'image',
    '.bmp': 'image',
    '.webp': 'image',

    // PDF文件
    '.pdf': 'pdf',

    // 视频文件
    '.mp4': 'video',
    '.avi': 'video',
    '.mov': 'video',
    '.wmv': 'video',
    '.flv': 'video',
    '.webm': 'video',

    // Word文档
    '.doc': 'docx',
    '.docx': 'docx',

    // Excel表格
    '.xls': 'excel',
    '.xlsx': 'excel'
  }

  return previewTypeMap[ext] || 'unsupported'
}

// 判断文件是否支持预览
const isFilePreviewable = (fileName: string): boolean => {
  return getPreviewType(fileName) !== 'unsupported'
}

// 接口定义
type SkuFiles = Record<string, SkuFileDTO[]>
type SkuFilesVO = Record<string, SkuFileVO[]>

interface Props {
  skuId?: number
  readonly?: boolean
  files?: SkuFilesVO
}

interface Emits {
  (e: 'files-change', files: SkuFiles): void
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false,
  files: () => ({})
})

const emit = defineEmits<Emits>()

// 使用 Map 管理文件列表和上传状态
const fileListMap = reactive(new Map<string, UploadFile[]>())
const uploadingStateMap = reactive(new Map<string, boolean>())

// 初始化所有文件类型的状态
FILE_TYPE_CONFIGS.forEach(config => {
  fileListMap.set(config.fileType, [])
  uploadingStateMap.set(config.fileType, false)
})

// 标志位，用于避免在文件回显时触发变化事件
const isLoadingFiles = ref(false)

// 上传组件引用
const uploadRefs = ref<Record<string, any>>({})

// 预览相关状态
const previewVisible = ref(false)
const previewFile = ref<PreviewFileInfo | null>(null)
const previewLoading = ref(false)

// 工程文件表格列配置
const engineeringFileColumns = [
  {
    title: '文件类型',
    dataIndex: 'typeName',
    key: 'typeName',
    width: 280,
    slots: { customRender: 'fileTypeIcon' }
  },
  {
    title: '说明',
    dataIndex: 'description',
    key: 'description',
    width: 300
  },
  {
    title: '文件列表',
    dataIndex: 'files',
    key: 'files',
    slots: { customRender: 'fileList' }
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    slots: { customRender: 'actions' }
  }
]

// 工程文件表格数据
const engineeringFileTableData = computed(() =>
  FILE_TYPE_CONFIGS.map(config => ({
    ...config,
    files: fileListMap.get(config.fileType) || [],
    fileCount: fileListMap.get(config.fileType)?.length || 0,
    uploading: uploadingStateMap.get(config.fileType) || false
  }))
)

// 设置上传组件引用
const setUploadRef = (fileType: string, ref: any) => {
  if (ref) {
    uploadRefs.value[fileType] = ref
  }
}

// 触发文件上传
const triggerUpload = (fileType: string) => {
  const uploadRef = uploadRefs.value[fileType]
  if (uploadRef?.$el) {
    const input = uploadRef.$el.querySelector('input[type="file"]')
    input?.click()
  }
}

// 处理文件下载
const handleDownloadFile = (file: UploadFile) => {
  const url = file.url || file.response?.fileUrl
  if (url) {
    window.open(url, '_blank')
  }
}

// 处理文件预览
const handlePreviewFile = (file: UploadFile) => {
  const previewType = getPreviewType(file.name)
  const fileUrl = file.url || file.response?.fileUrl

  if (!fileUrl) {
    message.error('文件地址不存在')
    return
  }

  // 调试信息
  console.log('预览文件:', {
    fileName: file.name,
    fileUrl: fileUrl,
    previewType: previewType
  })

  // 设置预览数据
  previewFile.value = {
    name: file.name,
    url: fileUrl,
    type: previewType
  }

  // 打开加载态（先渲染内容，再由各自的 onload/事件关闭）
  previewLoading.value = true
  previewVisible.value = true

  // 不支持的文件类型无需等待
  if (previewType === 'unsupported') {
    nextTick(() => (previewLoading.value = false))
  }

  // 视频和PDF、Office文档通过事件控制加载状态，但添加超时保护
  if (previewType === 'pdf' || previewType === 'docx' || previewType === 'excel') {
    setTimeout(() => {
      if (previewLoading.value) {
        console.warn('文档预览超时，强制停止加载状态')
        previewLoading.value = false
        message.warning('文档加载时间较长，如果预览失败请尝试下载文件查看')
      }
    }, 15000) // 15秒超时
  } else if (previewType === 'video') {
    // 视频添加较短的超时保护（5秒）
    setTimeout(() => {
      if (previewLoading.value) {
        console.warn('视频预览超时，强制停止加载状态')
        previewLoading.value = false
      }
    }, 5000) // 5秒超时
  }
}

// 关闭预览
const handleClosePreview = () => {
  previewVisible.value = false
  previewFile.value = null
  previewLoading.value = false
}

// 下载当前预览的文件
const handleDownloadCurrentFile = () => {
  if (previewFile.value?.url) {
    window.open(previewFile.value.url, '_blank')
  }
}

// 处理预览成功事件
const handlePreviewSuccess = () => {
  console.log('文档预览成功')
  previewLoading.value = false
}

// 处理 Office 文档预览错误
const handleOfficePreviewError = (error: any) => {
  console.error('Office文档预览失败:', error)
  previewLoading.value = false

  // 检查是否是CORS或网络问题
  if (error?.message?.includes('CORS') || error?.message?.includes('network')) {
    message.error('文档预览失败：网络或跨域问题，请尝试下载文件查看')
  } else if (error?.message?.includes('format') || error?.message?.includes('parse')) {
    message.error('文档预览失败：文件格式问题，请尝试下载文件查看')
  } else {
    message.error('文档预览加载失败，请尝试下载文件查看')
  }
}

// 处理视频开始加载
const handleVideoLoadStart = () => {
  console.log('视频开始加载')
  // 开始加载时不改变 loading 状态，保持加载状态
}

// 处理视频可以播放
const handleVideoCanPlay = () => {
  console.log('视频可以播放')
  previewLoading.value = false
}

// 处理视频加载错误
const handleVideoError = (error: any) => {
  console.error('视频预览失败:', error)
  previewLoading.value = false
  message.error('视频预览失败，请检查视频文件是否损坏或尝试下载查看')
}

// 图片加载事件处理
const handleImageLoad = () => {
  console.log('图片加载完成')
  previewLoading.value = false
}

const handleImageError = (error: any) => {
  console.error('图片预览失败:', error)
  previewLoading.value = false
  message.error('图片预览失败，请尝试下载查看')
}

const ossDomain = import.meta.env.VITE_OSS_DOMAIN

// 处理自定义上传
const handleCustomUpload = async (options: any, fileType: string) => {
  const { file, onProgress, onSuccess } = options

  uploadingStateMap.set(fileType, true)

  try {
    // 使用带缓存的上传方法
    const uploadResult = await uploadToOSSWithCache(
      file,
      {
        addTimestamp: true,
        sanitizeFileName: true,
        filePrefix: '',
        folder: `sku/${fileType}`,
        timeFormat: 'timestamp'
      },
      percent => onProgress({ percent })
    )

    // 构建上传文件对象
    const uploadFile = {
      uid: file.uid,
      name: file.name,
      status: 'done' as const,
      url: uploadResult.url,
      response: {
        url: uploadResult.url,
        fileName: uploadResult.fileName,
        originalName: uploadResult.originalName,
        fileType: fileType,
        objectKey: uploadResult.objectKey,
        skuId: props.skuId
      }
    }

    // 添加到文件列表
    const currentList = fileListMap.get(fileType) || []

    // 检查是否单文件类型，需要替换
    const config = FILE_TYPE_CONFIGS.find(c => c.fileType === fileType)
    if (!config?.multiple && currentList.length > 0) {
      fileListMap.set(fileType, [uploadFile])
    } else {
      currentList.push(uploadFile)
      fileListMap.set(fileType, [...currentList])
    }

    onSuccess(uploadFile.response, file)
    message.success(`${FILE_TYPE_NAME_MAP[fileType]}上传成功`)

    // 添加上传成功的视觉反馈
    nextTick(() => {
      const fileItems = document.querySelectorAll(`[data-file-type="${fileType}"] .file-item`)
      const lastItem = fileItems[fileItems.length - 1]
      if (lastItem) {
        lastItem.classList.add('upload-success')
        setTimeout(() => {
          lastItem.classList.remove('upload-success')
        }, 600)
      }
    })

    // 触发文件变化事件
    emitFilesChange()
  } catch (error: any) {
    console.error('工程文件上传失败:', error)
    message.error(`${FILE_TYPE_NAME_MAP[fileType]}上传失败: ${error.message || '未知错误'}`)
  } finally {
    uploadingStateMap.set(fileType, false)
  }
}

// 处理上传前验证
const handleBeforeUpload = (file: File, fileType: string) => {
  // 根据文件类型获取对应的大小限制配置
  const config = FILE_TYPE_CONFIGS.find(c => c.fileType === fileType)
  const maxSize = config?.maxSize || 300 // 默认300MB

  if (file.size / 1024 / 1024 > maxSize) {
    const sizeStr = maxSize >= 1024 ? `${maxSize / 1024}GB` : `${maxSize}MB`
    message.error(`文件大小不能超过 ${sizeStr}`)
    return false
  }
  return true
}

// 处理文件删除
const handleRemoveFile = async (file: UploadFile, fileType: string) => {
  try {
    const currentList = fileListMap.get(fileType) || []
    const newList = currentList.filter(f => f.uid !== file.uid)
    fileListMap.set(fileType, newList)

    // 触发文件变化事件
    emitFilesChange()
  } catch (error) {
    console.error('删除文件失败:', error)
  }
}

// 处理文件拖拽排序
const handleFileDragEnd = (newList: UploadFile[], fileType: string) => {
  fileListMap.set(fileType, [...newList])
  // 触发文件变化事件
  emitFilesChange()
}

// 根据 objectKey 生成完整的 URL
const getFullUrl = (objectKey: string) => {
  if (!objectKey || objectKey.startsWith('http')) return objectKey
  return `${ossDomain}/${objectKey}`
}

// 从文件名中提取显示名称
const getDisplayName = (objectKey: string) => {
  if (!objectKey) return 'unknown'
  return objectKey.split('/').pop() || objectKey
}

// 从完整URL中提取 objectKey
const extractObjectKeyFromUrl = (url: string): string => {
  if (!url) return ''
  if (url.startsWith(ossDomain)) {
    return url.replace(`${ossDomain}/`, '')
  }
  return url
}

// 加载文件列表
const loadFileList = () => {
  if (!props.files) return

  console.log(props.files)

  isLoadingFiles.value = true

  // 清空所有文件列表
  FILE_TYPE_CONFIGS.forEach(config => {
    fileListMap.set(config.fileType, [])
  })

  // 加载文件数据
  Object.entries(props.files).forEach(([fileType, fileInfos]) => {
    if (!Array.isArray(fileInfos)) return

    const uploadFiles: UploadFile[] = fileInfos.map((fileInfo, index) => {
      // 兼容 SkuFileVO 和 SkuFileDTO 两种类型
      const fileUrl = (fileInfo as any).fileUrl || getFullUrl((fileInfo as any).objectKey)
      console.log(fileUrl)

      return {
        uid: `${fileType}_${(fileInfo as any).id || index}_${Date.now()}`,
        name: getDisplayName((fileInfo as any).objectKey),
        status: 'done' as const,
        url: fileUrl,
        response: {
          url: fileUrl,
          fileName: getDisplayName((fileInfo as any).objectKey),
          originalName: getDisplayName((fileInfo as any).objectKey),
          fileType: (fileInfo as any).fileType,
          objectKey: (fileInfo as any).objectKey,
          skuId: props.skuId
        }
      }
    })

    fileListMap.set(fileType, uploadFiles)
  })

  nextTick(() => {
    isLoadingFiles.value = false
  })
}

// 获取当前所有文件数据
const getFilesData = (): SkuFiles => {
  const filesData: SkuFiles = {}

  FILE_TYPE_CONFIGS.forEach(config => {
    const fileList = fileListMap.get(config.fileType) || []
    if (fileList.length > 0) {
      filesData[config.fileType] = fileList.map(
        file =>
          ({
            fileType: config.fileType,
            objectKey: file.response?.objectKey || extractObjectKeyFromUrl(file.url || '')
          }) as SkuFileDTO
      )
    }
  })

  return filesData
}

// 清空所有文件列表
const clearAllFiles = () => {
  FILE_TYPE_CONFIGS.forEach(config => {
    fileListMap.set(config.fileType, [])
  })
}

// 触发文件变化事件
const emitFilesChange = () => {
  if (isLoadingFiles.value) return

  const filesData = getFilesData()
  emit('files-change', filesData)
}

// 暴露方法给父组件
defineExpose({
  getFilesData,
  clearAllFiles,
  emitFilesChange
})

// 监听 files 属性变化
watch(
  () => props.files,
  () => loadFileList(),
  { immediate: true, deep: true }
)

// 监听 skuId 变化
watch(
  () => props.skuId,
  newSkuId => {
    if (newSkuId) {
      loadFileList()
    }
  }
)
</script>

<style scoped>
.engineering-file-manager {
  width: 100%;
}

.engineering-files-table {
  .file-type-display {
    display: flex;
    align-items: flex-start;
    gap: 12px;

    .file-type-icon {
      position: relative;

      .icon-wrapper {
        width: 44px;
        height: 44px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        color: white;
        background: linear-gradient(
          135deg,
          var(--icon-color) 0%,
          color-mix(in srgb, var(--icon-color) 85%, #000 15%) 100%
        );
        border-radius: 12px;
        box-shadow:
          0 2px 8px color-mix(in srgb, var(--icon-color) 30%, transparent 70%),
          0 1px 3px rgba(0, 0, 0, 0.1);
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        position: relative;
        overflow: hidden;

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, transparent 50%);
          border-radius: 12px;
        }

        &:hover {
          transform: scale(1.08) translateY(-1px);
          box-shadow:
            0 4px 16px color-mix(in srgb, var(--icon-color) 40%, transparent 60%),
            0 2px 8px rgba(0, 0, 0, 0.15);
        }

        .anticon {
          z-index: 1;
          font-size: 18px;
        }
      }

      .file-count-badge {
        position: absolute;
        top: -8px;
        right: -8px;
        background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);
        color: white;
        font-size: 11px;
        font-weight: 700;
        min-width: 20px;
        height: 20px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow:
          0 2px 6px rgba(255, 77, 79, 0.4),
          0 0 0 2px white;
        animation: badgePulse 2s infinite;
        border: 2px solid white;
        z-index: 2;
      }
    }

    .file-type-info {
      flex: 1;

      .file-type-name {
        font-weight: 600;
        color: #262626;
        margin-bottom: 6px;
        font-size: 14px;
      }

      .file-type-formats {
        .ant-tag {
          margin: 2px 4px 2px 0;
          font-size: 11px;
          line-height: 16px;
          padding: 0 6px;
          border-radius: 10px;
          border: none;
          font-weight: 500;
        }
      }
    }
  }

  .file-list-cell {
    .uploaded-files {
      .draggable-list {
        .file-item {
          display: flex;
          align-items: center;
          padding: 10px 12px;
          background: linear-gradient(135deg, #f8f9fa 0%, #f1f3f4 100%);
          border: 1px solid #e8eaed;
          border-radius: 8px;
          margin-bottom: 8px;
          cursor: move;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

          &:last-child {
            margin-bottom: 0;
          }

          &:hover {
            background: linear-gradient(135deg, #e6f7ff 0%, #d6f4ff 100%);
            border-color: #40a9ff;
            box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
            transform: translateY(-1px);
          }

          .drag-handle {
            color: #8c8c8c;
            margin-right: 12px;
            cursor: grab;
            padding: 4px;
            border-radius: 4px;
            transition: all 0.2s ease;

            &:hover {
              color: #1890ff;
              background-color: rgba(24, 144, 255, 0.1);
            }

            &:active {
              cursor: grabbing;
            }
          }

          .file-name {
            flex: 1;
            font-size: 13px;
            color: #262626;
            font-weight: 500;
            margin-right: 12px;
            word-break: break-all;
            line-height: 1.4;
          }

          .file-actions {
            display: flex;
            gap: 4px;

            .ant-btn {
              border: none;
              box-shadow: none;
              padding: 4px 8px;
              height: auto;
              border-radius: 6px;
              transition: all 0.2s ease;

              &:hover {
                transform: scale(1.05);
              }

              &.ant-btn-link {
                color: #1890ff;

                &:hover {
                  background-color: rgba(24, 144, 255, 0.1);
                }
              }

              &.ant-btn-dangerous {
                color: #ff4d4f;

                &:hover {
                  background-color: rgba(255, 77, 79, 0.1);
                }
              }
            }
          }
        }

        .ghost-item {
          opacity: 0.4;
          background: linear-gradient(135deg, #bae7ff 0%, #91d5ff 100%);
          border: 2px dashed #40a9ff;
          transform: scale(0.98);
        }

        .chosen-item {
          background: linear-gradient(135deg, #e6f7ff 0%, #d6f4ff 100%);
          border: 2px solid #40a9ff;
          box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
          transform: scale(1.02);
        }

        .drag-item {
          transform: rotate(3deg) scale(1.05);
          opacity: 0.9;
          box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
          z-index: 1000;
        }
      }
    }

    .no-files {
      text-align: center;
      padding: 20px;
      color: #8c8c8c;
      font-style: italic;
      background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
      border: 2px dashed #d9d9d9;
      border-radius: 8px;
      transition: all 0.3s ease;

      &:hover {
        border-color: #40a9ff;
        background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
      }
    }
  }

  .action-cell {
    .ant-btn {
      font-size: 12px;
      height: 32px;
      border-radius: 6px;
      font-weight: 500;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

      &.upload-btn {
        background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
        border: none;
        box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
        position: relative;
        overflow: hidden;

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: -100%;
          width: 100%;
          height: 100%;
          background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
          transition: left 0.5s;
        }

        &:hover {
          background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
          box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
          transform: translateY(-1px);

          &::before {
            left: 100%;
          }
        }

        &:active {
          transform: translateY(0);
        }

        &.ant-btn-loading {
          background: linear-gradient(135deg, #91d5ff 0%, #69c0ff 100%);

          .ant-btn-loading-icon {
            color: #fff;
          }
        }
      }
    }
  }

  :deep(.ant-table-tbody > tr > td) {
    padding: 16px 12px;
    vertical-align: top;
  }

  :deep(.ant-table-thead > tr > th) {
    background: linear-gradient(135deg, #fafafa 0%, #f0f0f0 100%);
    border-bottom: 2px solid #e8eaed;
    font-weight: 600;
    color: #262626;
  }

  :deep(.ant-table-tbody > tr) {
    transition: all 0.2s ease;

    &:hover {
      background-color: rgba(24, 144, 255, 0.02);
    }
  }
}

/* 预览区域样式 */
.preview-container {
  position: relative;
}

.preview-content {
  position: relative;
  min-height: 200px;
}

.preview-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: saturate(150%) blur(1px);
  z-index: 10;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .engineering-files-table {
    .file-list-cell {
      .uploaded-files {
        .draggable-list {
          .file-item {
            padding: 8px 10px;

            .file-name {
              font-size: 12px;
              margin-right: 8px;
            }

            .drag-handle {
              margin-right: 8px;
            }
          }
        }
      }
    }

    .action-cell {
      .ant-btn {
        font-size: 11px;
        height: 28px;
        padding: 0 8px;
      }
    }
  }
}

/* 动画效果 */
@keyframes fileItemEnter {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes badgePulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

@keyframes uploadSuccess {
  0% {
    transform: scale(1);
    background-color: #52c41a;
  }
  50% {
    transform: scale(1.1);
    background-color: #73d13d;
  }
  100% {
    transform: scale(1);
    background-color: #52c41a;
  }
}

.file-item {
  animation: fileItemEnter 0.3s ease-out;
}

/* 上传成功动画 */
.upload-success {
  animation: uploadSuccess 0.6s ease-out;
}

/* 拖拽时的全局样式 */
.sortable-ghost {
  opacity: 0.4;
}

.sortable-chosen {
  transform: scale(1.02);
}

.sortable-drag {
  transform: rotate(3deg);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

/* 文件预览模态框样式 */
.file-preview-modal {
  :deep(.ant-modal-content) {
    border-radius: 12px;
    overflow: hidden;
  }

  :deep(.ant-modal-header) {
    background: linear-gradient(135deg, #f6f9fc 0%, #f1f5f9 100%);
    border-bottom: 1px solid #e8eaed;
    padding: 16px 24px;

    .ant-modal-title {
      font-weight: 600;
      color: #262626;
    }
  }

  :deep(.ant-modal-body) {
    max-height: 80vh;
    overflow: hidden;
  }
}

.preview-container {
  width: 100%;
  height: 100%;

  .preview-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    color: #8c8c8c;

    p {
      margin-top: 16px;
      font-size: 14px;
    }
  }

  .preview-content {
    .image-preview {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 20px;
      background: #fafafa;

      img {
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }
    }

    .pdf-preview {
      background: #f5f5f5;
      border-radius: 8px;
      overflow: hidden;
      padding: 20px;

      :deep(.vue-office-pdf) {
        background: #fff;
        border-radius: 8px;
      }
    }

    .video-preview {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 20px;
      background: #000;

      video {
        border-radius: 8px;
      }
    }

    .office-preview,
    .unsupported-preview {
      padding: 20px;

      .office-preview-placeholder {
        text-align: center;
      }

      .docx-preview,
      .excel-preview {
        border: 1px solid #e8eaed;
        border-radius: 8px;
        overflow: hidden;
        background: #fff;

        :deep(.vue-office-docx),
        :deep(.vue-office-excel) {
          border: none;
          box-shadow: none;
        }
      }
    }

    /* 当模态框body没有padding时，Office预览需要特殊处理 */
    .office-preview.no-padding {
      padding: 0;

      .docx-preview,
      .excel-preview {
        border: none;
        border-radius: 0;
        height: 100%;
      }
    }
  }
}
</style>
