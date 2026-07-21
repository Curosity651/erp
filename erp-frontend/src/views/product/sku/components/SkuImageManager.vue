/** * SKU 图片管理组件（支持拖拽排序） * 专门处理图片文件的上传、预览、管理和排序 *
基于统一的文件管理逻辑，支持拖拽排序功能 */
<template>
  <div class="sku-image-manager">
    <!-- 图片类型并列展示 -->
    <div v-for="config in imageConfigs" :key="config.fileType" class="image-type-section">
      <!-- 类型标题栏 -->
      <div class="section-header">
        <div class="header-left">
          <div class="section-icon" :style="{ backgroundColor: config.color }">
            <component :is="config.icon" />
          </div>
          <div class="section-title">
            <h3>{{ config.title }}</h3>
            <p>{{ config.description }}</p>
          </div>
        </div>
        <div class="header-right">
          <a-badge
            :count="getImageCount(config.fileType)"
            :number-style="{ backgroundColor: config.color }"
            show-zero
          />
          <span class="count-text">{{ getImageCount(config.fileType) }}/{{ config.maxCount }}</span>
        </div>
      </div>

      <!-- 图片内容区域 -->
      <div class="image-content">
        <!-- 图片网格展示 - 支持拖拽排序 -->
        <div class="image-grid">
          <!-- 可拖拽的图片列表 -->
          <draggable
            :model-value="getImageList(config.fileType)"
            :disabled="readonly"
            item-key="uid"
            class="draggable-container"
            ghost-class="ghost-item"
            chosen-class="chosen-item"
            drag-class="drag-item"
            @update:model-value="newList => updateImageList(config.fileType, newList)"
            @end="evt => handleDragEnd(evt, config.fileType)"
          >
            <template #item="{ element: image, index }">
              <div class="image-item" :class="{ 'image-item-readonly': readonly }">
                <div class="image-preview">
                  <img :src="image.url" :alt="image.name" />

                  <!-- 排序序号显示 -->
                  <div class="sort-number">{{ index + 1 }}</div>

                  <!-- 拖拽手柄 -->
                  <div v-if="!readonly" class="drag-handle">
                    <HolderOutlined />
                  </div>

                  <!-- 图片遮罩操作层 -->
                  <div class="image-overlay">
                    <div class="image-actions">
                      <!-- 预览 -->
                      <a-button
                        type="text"
                        size="small"
                        class="action-btn preview-btn"
                        @click.stop="previewImage(image)"
                      >
                        <EyeOutlined />
                      </a-button>

                      <!-- 下载 -->
                      <a-button
                        type="text"
                        size="small"
                        class="action-btn download-btn"
                        @click.stop="downloadImage(image)"
                      >
                        <DownloadOutlined />
                      </a-button>

                      <!-- 删除 -->
                      <a-button
                        v-if="!readonly"
                        type="text"
                        size="small"
                        danger
                        class="action-btn delete-btn"
                        @click.stop="removeImage(config.fileType, image.uid)"
                      >
                        <DeleteOutlined />
                      </a-button>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </draggable>

          <!-- 上传区域 -->
          <div
            v-if="!readonly && canUploadMore(config)"
            class="upload-area"
            @click="triggerUpload(config.fileType)"
          >
            <a-upload
              :ref="(el: any) => setUploadRef(config.fileType, el)"
              :custom-request="(options: any) => handleCustomUpload(options, config)"
              :before-upload="(file: File) => handleBeforeUpload(file, config)"
              :show-upload-list="false"
              :accept="config.accept"
              :multiple="config.multiple"
              style="display: none"
            />

            <div class="upload-trigger">
              <div class="upload-icon">
                <LoadingOutlined v-if="uploadingStateMap.get(config.fileType)" />
                <PlusOutlined v-else />
              </div>
              <div class="upload-text">
                {{ uploadingStateMap.get(config.fileType) ? '上传中...' : `上传${config.title}` }}
              </div>
              <div class="upload-hint">{{ config.uploadHint }}</div>
            </div>
          </div>
        </div>

        <!-- 上传提示信息 -->
        <div class="upload-tips">
          <div class="tip-item">
            <InfoCircleOutlined />
            支持格式：{{ config.formats.join('、') }}
          </div>
          <div v-if="!readonly" class="tip-item">
            <HolderOutlined />
            拖拽图片可调整显示顺序
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览模态框 -->
    <a-modal
      v-model:open="previewVisible"
      :title="previewImageData.name"
      :footer="null"
      width="80%"
      :style="{ maxWidth: '1000px' }"
      centered
      class="image-preview-modal"
    >
      <div class="preview-container">
        <img :src="previewImageData.url" :alt="previewImageData.name" />
        <div class="preview-url">
          <div class="url-label">图片链接:</div>
          <div class="url-text">{{ previewImageData.url }}</div>
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
import {
  PlusOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EyeOutlined,
  LoadingOutlined,
  PictureOutlined,
  CameraOutlined,
  InfoCircleOutlined,
  HolderOutlined
} from '@ant-design/icons-vue'

// 图片类型配置
interface ImageConfig {
  fileType: string
  title: string
  description: string
  formats: string[]
  accept: string
  multiple: boolean
  maxCount: number
  maxSize: number // MB
  color: string
  icon: any
  uploadHint: string
}

// 图片类型配置数据
const IMAGE_CONFIGS: ImageConfig[] = [
  {
    fileType: 'actual_image',
    title: '实物图片',
    description: '产品实物拍摄图片，用于展示真实产品效果',
    formats: ['.jpg', '.jpeg', '.png', '.gif', '.webp'],
    accept: '.jpg,.jpeg,.png,.gif,.webp',
    multiple: true,
    maxCount: 999,
    maxSize: 999,
    color: '#1890ff',
    icon: CameraOutlined,
    uploadHint: '支持 JPG、PNG、GIF、WebP 格式'
  },
  {
    fileType: 'platform_image',
    title: '平台图片',
    description: '用于平台展示的产品图片，经过优化处理',
    formats: ['.jpg', '.jpeg', '.png', '.gif', '.webp'],
    accept: '.jpg,.jpeg,.png,.gif,.webp',
    multiple: true,
    maxCount: 999,
    maxSize: 999,
    color: '#52c41a',
    icon: PictureOutlined,
    uploadHint: '支持 JPG、PNG、GIF、WebP 格式'
  }
]

// 组件属性
interface Props {
  skuId?: number
  readonly?: boolean
  files?: Record<string, SkuFileVO[]>
}

// 组件事件
interface Emits {
  (e: 'files-change', files: Record<string, SkuFileDTO[]>): void
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false,
  files: () => ({})
})

const emit = defineEmits<Emits>()

// 图片配置
const imageConfigs = computed(() => IMAGE_CONFIGS)

// 使用 Map 管理图片状态
const imageListMap = reactive(new Map<string, UploadFile[]>())
const uploadingStateMap = reactive(new Map<string, boolean>())

// 初始化图片状态
imageConfigs.value.forEach(config => {
  imageListMap.set(config.fileType, [])
  uploadingStateMap.set(config.fileType, false)
})

// 标志位，避免在文件回显时触发变化事件
const isLoadingFiles = ref(false)

// 上传组件引用
const uploadRefs = ref<Record<string, any>>({})

// 图片预览相关
const previewVisible = ref(false)
const previewImageData = ref<{
  name: string
  url: string
  size?: number
  uploadTime?: string
  fileType?: string
}>({ name: '', url: '' })

// 获取指定类型的图片列表
const getImageList = (fileType: string) => {
  return imageListMap.get(fileType) || []
}

// 获取指定类型的图片数量
const getImageCount = (fileType: string) => {
  return getImageList(fileType).length
}

// 更新指定类型的图片列表
const updateImageList = (fileType: string, newList: any[]) => {
  imageListMap.set(fileType, newList)
}

// 检查是否可以上传更多图片
const canUploadMore = (config: ImageConfig) => {
  return getImageCount(config.fileType) < config.maxCount
}

// 设置上传组件引用
const setUploadRef = (fileType: string, ref: any) => {
  if (ref) {
    uploadRefs.value[fileType] = ref
  }
}

// 触发图片上传
const triggerUpload = (fileType: string) => {
  const uploadRef = uploadRefs.value[fileType]
  if (uploadRef?.$el) {
    const input = uploadRef.$el.querySelector('input[type="file"]')
    input?.click()
  }
}

// 处理拖拽结束事件
const handleDragEnd = (evt: any, fileType: string) => {
  console.log('拖拽结束:', evt, fileType)

  // 更新排序后触发文件变化事件
  nextTick(() => {
    emitFilesChange()
  })
}

// 处理自定义上传
const handleCustomUpload = async (options: any, config: ImageConfig) => {
  const { file, onProgress, onSuccess, onError } = options

  uploadingStateMap.set(config.fileType, true)

  try {
    // 使用带缓存的上传方法
    const uploadResult = await uploadToOSSWithCache(
      file,
      {
        addTimestamp: true,
        sanitizeFileName: true,
        filePrefix: `${config.fileType}`,
        folder: 'sku',
        timeFormat: 'timestamp'
      },
      percent => onProgress({ percent })
    )

    // 构建图片对象
    const uploadFile = {
      uid: file.uid,
      name: file.name,
      status: 'done' as const,
      url: uploadResult.url,
      size: file.size,
      response: {
        url: uploadResult.url,
        fileName: uploadResult.fileName,
        originalName: uploadResult.originalName,
        fileType: config.fileType,
        objectKey: uploadResult.objectKey,
        skuId: props.skuId,
        uploadTime: new Date().toLocaleString()
      }
    }

    // 添加到图片列表
    const currentList = imageListMap.get(config.fileType) || []
    currentList.push(uploadFile)
    imageListMap.set(config.fileType, [...currentList])

    onSuccess(uploadFile.response, file)
    message.success(`${config.title}上传成功`)

    // 触发文件变化事件
    emitFilesChange()
  } catch (error: any) {
    console.error(`${config.title}上传失败:`, error)
    message.error(`${config.title}上传失败: ${error.message || '未知错误'}`)
    onError(error)
  } finally {
    uploadingStateMap.set(config.fileType, false)
  }
}

// 处理上传前验证
const handleBeforeUpload = (file: File, config: ImageConfig) => {
  // 文件大小验证
  if (file.size / 1024 / 1024 > config.maxSize) {
    message.error(`${config.title}大小不能超过 ${config.maxSize}MB`)
    return false
  }

  // 文件类型验证
  const fileExtension = '.' + file.name.split('.').pop()?.toLowerCase()
  if (!config.formats.includes(fileExtension)) {
    message.error(`${config.title}格式不正确，请选择 ${config.formats.join('、')} 格式的文件`)
    return false
  }

  // 数量验证
  const currentList = imageListMap.get(config.fileType) || []
  if (currentList.length >= config.maxCount) {
    message.error(`${config.title}最多只能上传 ${config.maxCount} 张`)
    return false
  }

  return true
}

// 移除图片
const removeImage = (fileType: string, fileUid: string) => {
  const currentList = imageListMap.get(fileType) || []
  const newList = currentList.filter(f => f.uid !== fileUid)
  imageListMap.set(fileType, newList)

  // 触发文件变化事件
  emitFilesChange()

  message.success('图片删除成功')
}

// 下载图片
const downloadImage = (image: UploadFile) => {
  if (image.url) {
    const link = document.createElement('a')
    link.href = image.url
    link.download = image.name
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

// 预览图片
const previewImage = (image: UploadFile) => {
  console.log('预览图片被调用:', image)
  previewImageData.value = {
    name: image.name,
    url: image.url || '',
    size: image.size,
    uploadTime: image.response?.uploadTime,
    fileType: image.response?.fileType
  }
  console.log('预览数据设置:', previewImageData.value)
  previewVisible.value = true
  console.log('预览模态框状态:', previewVisible.value)
}

const ossDomain = import.meta.env.VITE_OSS_DOMAIN

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

// 加载图片列表
const loadFileList = () => {
  if (!props.files) return

  isLoadingFiles.value = true

  // 清空所有图片列表
  imageConfigs.value.forEach(config => {
    imageListMap.set(config.fileType, [])
  })

  // 加载图片数据
  Object.entries(props.files).forEach(([fileType, fileInfos]) => {
    const config = imageConfigs.value.find(c => c.fileType === fileType)
    if (!Array.isArray(fileInfos) || !config) {
      return
    }

    // 按排序字段排序
    const sortedFileInfos = [...fileInfos].sort((a, b) => {
      const sortOrderA = (a as any).sortOrder || 0
      const sortOrderB = (b as any).sortOrder || 0
      return sortOrderA - sortOrderB
    })

    const uploadFiles: UploadFile[] = sortedFileInfos.map((fileInfo, index) => {
      const fileUrl = (fileInfo as any).fileUrl || getFullUrl((fileInfo as any).objectKey)

      return {
        uid: `${fileType}_${(fileInfo as any).id || index}_${Date.now()}`,
        name: getDisplayName((fileInfo as any).objectKey),
        status: 'done' as const,
        url: fileUrl,
        size: (fileInfo as any).fileSize,
        response: {
          url: fileUrl,
          fileName: getDisplayName((fileInfo as any).objectKey),
          originalName: getDisplayName((fileInfo as any).objectKey),
          fileType: (fileInfo as any).fileType,
          objectKey: (fileInfo as any).objectKey,
          skuId: props.skuId,
          uploadTime: (fileInfo as any).uploadTime
        }
      }
    })

    imageListMap.set(fileType, uploadFiles)
  })

  nextTick(() => {
    isLoadingFiles.value = false
  })
}

// 获取当前所有图片数据
const getFilesData = (): Record<string, SkuFileDTO[]> => {
  const filesData: Record<string, SkuFileDTO[]> = {}

  imageConfigs.value.forEach(config => {
    const imageList = imageListMap.get(config.fileType) || []
    if (imageList.length > 0) {
      filesData[config.fileType] = imageList.map(
        image =>
          ({
            fileType: config.fileType,
            objectKey: image.response?.objectKey || extractObjectKeyFromUrl(image.url || '')
          }) as SkuFileDTO
      )
    }
  })

  return filesData
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
  clearAllFiles: () => {
    imageConfigs.value.forEach(config => {
      imageListMap.set(config.fileType, [])
    })
  }
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
.sku-image-manager {
  width: 100%;
}

/* 图片类型区域样式 */
.image-type-section {
  margin-bottom: 32px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.image-type-section:last-child {
  margin-bottom: 0;
}

/* 区域标题栏 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.section-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.section-title p {
  margin: 2px 0 0 0;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.3;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.count-text {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 500;
}

/* 图片内容区域 */
.image-content {
  padding: 20px;
  background: #fafafa;
  min-height: 200px;
}

/* 图片网格 */
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

/* 拖拽容器 */
.draggable-container {
  display: contents;
}

/* 图片项样式 */
.image-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background: white;
  border: 2px solid #e8e8e8;
  transition: all 0.3s ease;
  aspect-ratio: 1;
  min-height: 140px;
  cursor: move;
}

.image-item:hover {
  border-color: #1890ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
}

.image-item-readonly {
  cursor: default;
}

.image-item-readonly:hover {
  transform: none;
}

/* 拖拽状态样式 */
.ghost-item {
  opacity: 0.5;
  background: #f0f9ff;
  border: 2px dashed #1890ff;
}

.chosen-item {
  transform: rotate(5deg);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.3);
}

.drag-item {
  transform: rotate(5deg);
  opacity: 0.8;
}

.image-preview {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  object-position: center;
  background: #f5f5f5;
  border-radius: 4px;
}

/* 排序序号 */
.sort-number {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 24px;
  height: 24px;
  background: rgba(24, 144, 255, 0.9);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  z-index: 2;
}

/* 拖拽手柄 */
.drag-handle {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: move;
  z-index: 2;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-item:hover .drag-handle {
  opacity: 1;
}

/* 图片遮罩层 */
.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.image-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.6);
  transform: scale(1.1);
}

.delete-btn:hover {
  background: rgba(255, 77, 79, 0.8);
  border-color: #ff4d4f;
}

/* 上传区域样式 */
.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  aspect-ratio: 1;
  min-height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.upload-area:hover {
  border-color: #1890ff;
  background: #f0f9ff;
}

.upload-trigger {
  text-align: center;
  color: #666;
}

.upload-icon {
  font-size: 28px;
  margin-bottom: 10px;
  color: #1890ff;
}

.upload-text {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
}

.upload-hint {
  font-size: 11px;
  color: #8c8c8c;
  line-height: 1.4;
  max-width: 120px;
}

/* 上传提示 */
.upload-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8c8c8c;
}

/* 预览模态框样式 */
.image-preview-modal :deep(.ant-modal-body) {
  padding: 20px;
}

.preview-container img {
  width: 100%;
  max-height: 60vh;
  object-fit: contain;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.preview-url {
  margin-top: 20px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
}

.url-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
  font-weight: 500;
}

.url-text {
  font-size: 13px;
  color: #262626;
  word-break: break-all;
  background: white;
  padding: 8px 12px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  font-family: 'Courier New', monospace;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .image-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  }
}

@media (max-width: 768px) {
  .image-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 12px;
  }

  .image-item {
    min-height: 120px;
  }

  .upload-area {
    min-height: 120px;
  }

  .upload-icon {
    font-size: 24px;
    margin-bottom: 8px;
  }

  .image-content {
    padding: 16px;
  }

  .upload-tips {
    flex-direction: column;
    gap: 8px;
  }

  .section-header {
    padding: 12px 16px;
  }

  .section-icon {
    width: 32px;
    height: 32px;
    font-size: 14px;
  }

  .section-title h3 {
    font-size: 14px;
  }

  .section-title p {
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .image-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .header-left {
    gap: 8px;
  }

  .section-title h3 {
    font-size: 13px;
  }

  .section-title p {
    display: none;
  }
}
</style>
