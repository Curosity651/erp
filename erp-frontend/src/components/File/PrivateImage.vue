<template>
  <a-skeleton-image v-if="loading" :style="imageStyle" />
  <a-image
    v-else-if="signedUrl"
    :src="signedUrl"
    :width="width"
    :height="height"
    :preview="preview"
    :fallback="fallback"
  />
  <div v-else class="private-image-error" :style="imageStyle">
    <file-image-outlined />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { FileImageOutlined } from '@ant-design/icons-vue'
import { usePrivateFile } from '@/hooks/use-private-file'

const props = withDefaults(
  defineProps<{
    /** 文件ID */
    fileId?: number
    /** 图片宽度 */
    width?: number | string
    /** 图片高度 */
    height?: number | string
    /** 是否启用预览 */
    preview?: boolean
    /** 加载失败时显示的图片 */
    fallback?: string
  }>(),
  {
    width: 100,
    height: 100,
    preview: true,
    fallback: ''
  }
)

const { loading, getSignedUrl } = usePrivateFile()
const signedUrl = ref<string | null>(null)

const imageStyle = computed(() => ({
  width: typeof props.width === 'number' ? `${props.width}px` : props.width,
  height: typeof props.height === 'number' ? `${props.height}px` : props.height
}))

const loadImage = async () => {
  if (!props.fileId) {
    signedUrl.value = null
    return
  }
  signedUrl.value = await getSignedUrl(props.fileId)
}

watch(
  () => props.fileId,
  () => loadImage(),
  { immediate: false }
)

onMounted(() => {
  loadImage()
})
</script>

<style scoped>
.private-image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  color: #999;
  font-size: 24px;
}
</style>
