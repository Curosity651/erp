<template>
  <div class="image-upload-container">
    <a-upload
      :custom-request="handleCustomRequest"
      :before-upload="beforeUpload"
      :show-upload-list="false"
      :disabled="disabled"
      accept="image/*"
    >
      <div class="upload-area" :class="{ 'has-image': imageUrl }">
        <div v-if="!imageUrl && !uploading" class="upload-placeholder">
          <PlusOutlined />
          <div class="upload-text">{{ placeholder }}</div>
        </div>
        <div v-if="uploading" class="uploading">
          <LoadingOutlined />
          <div class="upload-text">上传中...</div>
        </div>
        <img v-if="imageUrl && !uploading" :src="imageUrl" class="uploaded-image" />
        <div v-if="imageUrl && !uploading" class="upload-mask">
          <EyeOutlined @click.stop="handlePreview" />
          <DeleteOutlined @click.stop="handleDelete" />
        </div>
      </div>
    </a-upload>

    <!-- 图片预览 -->
    <a-modal
      v-model:open="previewVisible"
      :footer="null"
      :centered="true"
      :width="800"
      title="图片预览"
    >
      <img :src="imageUrl" style="width: 100%" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, LoadingOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { uploadToOSSWithCache } from '@/hooks/use-oss-upload'

interface Props {
  modelValue?: string
  previewUrl?: string
  placeholder?: string
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '点击上传图片',
  disabled: false
})

const emits = defineEmits<{
  (e: 'update:modelValue', value: string | undefined): void
}>()

const uploading = ref(false)
const previewVisible = ref(false)

const localPreviewUrl = ref('')
const localPreviewObjectKey = ref('')

const imageUrl = computed(() => {
  if (!props.modelValue) return ''
  if (localPreviewObjectKey.value === props.modelValue && localPreviewUrl.value) {
    return localPreviewUrl.value
  }
  if (props.previewUrl) {
    return props.previewUrl
  }
  // 如果已经是完整URL，直接返回
  if (props.modelValue.startsWith('http')) {
    return props.modelValue
  }
  // 否则拼接OSS域名
  return ''
})

const clearLocalPreview = () => {
  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value)
  }
  localPreviewUrl.value = ''
  localPreviewObjectKey.value = ''
}

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }

  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    message.error('图片大小不能超过 5MB!')
    return false
  }

  return true
}

const handleCustomRequest = async (options: any) => {
  const { file } = options

  if (!beforeUpload(file)) {
    return
  }

  uploading.value = true

  try {
    const result = await uploadToOSSWithCache(
      file,
      {
        folder: 'supplier/business-license',
        addTimestamp: true
      },
      (progress: number) => {
        console.log('上传进度:', progress)
      }
    )

    clearLocalPreview()
    localPreviewUrl.value = URL.createObjectURL(file)
    localPreviewObjectKey.value = result.objectKey
    emits('update:modelValue', result.objectKey)
    message.success('上传成功!')
  } catch (error) {
    console.error('上传失败:', error)
    message.error('上传失败!')
  } finally {
    uploading.value = false
  }
}

const handlePreview = () => {
  previewVisible.value = true
}

const handleDelete = () => {
  clearLocalPreview()
  emits('update:modelValue', undefined)
}

onBeforeUnmount(clearLocalPreview)
</script>

<style scoped>
.image-upload-container {
  display: inline-block;
}

.upload-area {
  width: 120px;
  height: 120px;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.upload-area:hover {
  border-color: #1890ff;
}

.upload-area.has-image {
  border-color: transparent;
}

.upload-placeholder,
.uploading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.upload-text {
  font-size: 12px;
  margin-top: 8px;
  text-align: center;
}

.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 0.3s;
}

.upload-area:hover .upload-mask {
  opacity: 1;
}

.upload-mask .anticon {
  color: white;
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
}

.upload-mask .anticon:hover {
  color: #1890ff;
}
</style>
