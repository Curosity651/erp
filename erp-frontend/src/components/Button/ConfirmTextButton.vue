<template>
  <a-popconfirm :title="props.title" @confirm="props.onConfirm">
    <a-button
      type="link"
      size="small"
      :danger="props.danger"
      :class="props.textClass"
      style="padding: 0"
    >
      {{ props.text }}
    </a-button>
  </a-popconfirm>
</template>

<script setup lang="ts">
defineOptions({ name: 'ConfirmTextButton' })

const props = defineProps<{
  title: string
  text: string
  danger?: boolean
  onConfirm?: (e: MouseEvent) => void
  textClass?: string | string[]
}>()
</script>
