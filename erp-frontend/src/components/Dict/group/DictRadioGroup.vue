<template>
  <div v-if="props.allowClear" class="dict-radio-group-clearable">
    <template v-if="props.type.toLowerCase() === 'radio'">
      <a-radio
        v-for="dictItem in dictItems"
        :key="dictItem.id"
        :checked="props.value === dictItem.value"
        :disabled="dictItem.disabled"
        @click="handleRadioClick(dictItem.value, $event)"
      >
        {{ dictItem.name }}
      </a-radio>
    </template>
    <template v-if="props.type.toLowerCase() === 'button'">
      <a-radio-button
        v-for="dictItem in dictItems"
        :key="dictItem.id"
        :checked="props.value === dictItem.value"
        :disabled="dictItem.disabled"
        @click="handleRadioClick(dictItem.value, $event)"
      >
        {{ dictItem.name }}
      </a-radio-button>
    </template>
  </div>
  <a-radio-group v-else v-bind="props" :value="props.value" @change="onChange">
    <template v-if="props.type.toLowerCase() === 'radio'">
      <a-radio
        v-for="dictItem in dictItems"
        :key="dictItem.id"
        :value="dictItem.value"
        :disabled="dictItem.disabled"
      >
        {{ dictItem.name }}
      </a-radio>
    </template>
    <template v-if="props.type.toLowerCase() === 'button'">
      <a-radio-button
        v-for="dictItem in dictItems"
        :key="dictItem.id"
        :value="dictItem.value"
        :disabled="dictItem.disabled"
      >
        {{ dictItem.name }}
      </a-radio-button>
    </template>
  </a-radio-group>
</template>

<script setup lang="ts">
import type { DictValue } from '@/api/system/dict/types'
import { useDict } from '@/components/Dict/use-dict'
import type { RadioChangeEvent } from 'ant-design-vue/es/radio/interface'
import { type DictComponentProps, dictRadioGroupProps } from '@/components/Dict/types'

const props = defineProps(dictRadioGroupProps())

const emits = defineEmits<{
  (e: 'update:value', selectedValue: DictValue | DictValue[] | null): void
}>()

const onChange = (e: RadioChangeEvent) => {
  emits('update:value', e.target.value as DictValue | DictValue[])
}

const handleRadioClick = (value: DictValue, event: Event) => {
  event.preventDefault()
  event.stopPropagation()

  if (props.value === value) {
    // 如果点击的是当前选中的值，则清除选择
    emits('update:value', null)
  } else {
    // 否则选中新的值
    emits('update:value', value)
  }
}

const dictItems = useDict(props as DictComponentProps)
</script>

<script lang="ts">
export default {
  name: 'DictRadioGroup'
}
</script>

<style scoped></style>
