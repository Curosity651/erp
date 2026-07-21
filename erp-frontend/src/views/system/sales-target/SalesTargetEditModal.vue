<template>
  <a-modal
    v-model:open="visible"
    :title="modalTitle"
    :confirm-loading="loading"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-item label="年份">
        <span>{{ year }}</span>
      </a-form-item>

      <a-form-item v-if="mode === 'editMonthly' || mode === 'createMonthly'" label="月份">
        <span>{{ monthName }}</span>
      </a-form-item>

      <a-form-item
        :label="mode === 'editAnnual' || mode === 'createAnnual' ? '年度目标' : '目标金额'"
        name="targetAmount"
        :rules="[
          { required: true, message: '请输入目标金额' },
          { type: 'number', min: 0.01, message: '金额必须大于0' }
        ]"
      >
        <a-input-number
          v-model:value="formData.targetAmount"
          :precision="2"
          :min="0.01"
          style="width: 100%"
          addon-before="₽"
        />
      </a-form-item>

      <a-form-item v-if="mode === 'editAnnual' || mode === 'createAnnual'" label="货币">
        <a-input value="RUB" disabled />
      </a-form-item>

      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" :rows="3" placeholder="可选" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { doRequest } from '@/utils/axios/request'
import {
  createAnnualTarget,
  updateAnnualTarget,
  updateSingleMonthly,
  createSingleMonthly
} from '@/api/system/sales-target'

const emit = defineEmits(['submit-success'])

const visible = ref(false)
const loading = ref(false)
const formRef = ref()
const mode = ref<'createAnnual' | 'editAnnual' | 'editMonthly' | 'createMonthly'>('editMonthly')

const year = ref<number>()
const month = ref<number>()
const targetId = ref<number>()

const formData = reactive({
  targetAmount: undefined as number | undefined,
  remark: ''
})

// 模态框标题
const modalTitle = computed(() => {
  if (mode.value === 'createAnnual') return '创建年度目标'
  if (mode.value === 'editAnnual') return '编辑年度目标'
  if (mode.value === 'editMonthly') return '编辑月度目标'
  return '创建月度目标'
})

// 月份名称
const monthNames = [
  '一月',
  '二月',
  '三月',
  '四月',
  '五月',
  '六月',
  '七月',
  '八月',
  '九月',
  '十月',
  '十一月',
  '十二月'
]
const monthName = computed(() => {
  return month.value ? `${month.value}月 (${monthNames[month.value - 1]})` : ''
})

// 打开模态框
const open = (
  openMode: 'createAnnual' | 'editAnnual' | 'editMonthly' | 'createMonthly',
  data: any
) => {
  mode.value = openMode

  if (openMode === 'createAnnual') {
    // 创建年度目标
    year.value = data.year
    targetId.value = undefined
    formData.targetAmount = undefined
    formData.remark = ''
  } else if (openMode === 'editAnnual') {
    // 编辑年度目标
    year.value = data.year
    targetId.value = data.annualTarget.id
    formData.targetAmount = parseFloat(data.annualTarget.targetAmount)
    formData.remark = data.annualTarget.remark || ''
  } else if (openMode === 'editMonthly') {
    // 编辑月度目标
    year.value = data.year
    month.value = data.record.month
    targetId.value = data.record.id
    formData.targetAmount = data.record.targetAmount
      ? parseFloat(data.record.targetAmount)
      : undefined
    formData.remark = data.record.remark === '-' ? '' : data.record.remark
  } else {
    // 创建月度目标
    year.value = data.year
    month.value = data.record.month
    targetId.value = undefined
    formData.targetAmount = undefined
    formData.remark = ''
  }

  visible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    if (mode.value === 'createAnnual') {
      // 创建年度目标
      const dto = {
        year: year.value!,
        amount: formData.targetAmount!,
        remark: formData.remark
      }

      await doRequest(createAnnualTarget(dto), {
        successMessage: '创建成功!',
        onSuccess: () => {
          visible.value = false
          emit('submit-success')
        }
      })
    } else if (mode.value === 'editAnnual') {
      // 更新年度目标
      if (!targetId.value) {
        console.error('目标ID不存在')
        return
      }

      await doRequest(
        updateAnnualTarget(targetId.value, {
          id: targetId.value,
          targetAmount: formData.targetAmount!,
          remark: formData.remark
        }),
        {
          successMessage: '更新成功!',
          onSuccess: () => {
            visible.value = false
            emit('submit-success')
          }
        }
      )
    } else if (mode.value === 'editMonthly') {
      // 更新月度目标
      if (!targetId.value) {
        console.error('目标ID不存在')
        return
      }

      await doRequest(
        updateSingleMonthly(targetId.value, {
          id: targetId.value,
          targetAmount: formData.targetAmount!,
          remark: formData.remark
        }),
        {
          successMessage: '更新成功!',
          onSuccess: () => {
            visible.value = false
            emit('submit-success')
          }
        }
      )
    } else {
      // 创建月度目标
      const dto = {
        year: year.value!,
        month: month.value!,
        amount: formData.targetAmount!,
        remark: formData.remark
      }

      await doRequest(createSingleMonthly(dto), {
        successMessage: '创建成功!',
        onSuccess: () => {
          visible.value = false
          emit('submit-success')
        }
      })
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    loading.value = false
  }
}

// 取消
const handleCancel = () => {
  visible.value = false
  formRef.value?.resetFields()
}

defineExpose({ open })
</script>
