<template>
  <a-button type="link" :loading="loading" size="small" @click="handleClick">
    <template #icon><download-outlined /></template>
    {{ displayName }}
  </a-button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { usePrivateFile } from '@/hooks/use-private-file'

const props = withDefaults(
  defineProps<{
    /** 文件ID */
    fileId?: number
    /** 显示的文件名 */
    fileName?: string
    /** 最大显示长度 */
    maxLength?: number
  }>(),
  {
    maxLength: 20
  }
)

const { loading, getSignedUrl } = usePrivateFile()

const displayName = computed(() => {
  if (!props.fileName) return '下载'
  if (props.fileName.length <= props.maxLength) return props.fileName
  const ext = props.fileName.substring(props.fileName.lastIndexOf('.'))
  const name = props.fileName.substring(0, props.maxLength - ext.length - 3)
  return `${name}...${ext}`
})

const handleClick = async () => {
  if (!props.fileId) return

  const url = await getSignedUrl(props.fileId)
  if (url) {
    window.open(url, '_blank')
  }
}
</script>
