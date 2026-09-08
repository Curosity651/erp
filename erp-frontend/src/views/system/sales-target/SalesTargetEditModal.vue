<template>
  <a-modal
    v-model:open="visible"
    :title="modalTitle"
    :confirm-loading="loading"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-item :label="t('system.salesTarget.year')">
        <span>{{ year }}</span>
      </a-form-item>

      <a-form-item
        v-if="mode === 'editMonthly' || mode === 'createMonthly'"
        :label="t('system.salesTarget.month')"
      >
        <span>{{ monthName }}</span>
      </a-form-item>

      <a-form-item
        :label="
          mode === 'editAnnual' || mode === 'createAnnual'
            ? t('system.salesTarget.annualTarget')
            : t('system.salesTarget.targetAmount')
        "
        name="targetAmount"
        :rules="[
          { required: true, message: t('system.salesTarget.validation.targetAmount') },
          { type: 'number', min: 0.01, message: t('system.salesTarget.validation.amountPositive') }
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

      <a-form-item
        v-if="mode === 'editAnnual' || mode === 'createAnnual'"
        :label="t('system.salesTarget.currency')"
      >
        <a-input value="RUB" disabled />
      </a-form-item>

      <a-form-item :label="t('common.remarks')" name="remark">
        <a-textarea
          v-model:value="formData.remark"
          :rows="3"
          :placeholder="t('system.salesTarget.optional')"
        />
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
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

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
  if (mode.value === 'createAnnual') return t('system.salesTarget.createAnnual')
  if (mode.value === 'editAnnual') return t('system.salesTarget.editAnnual')
  if (mode.value === 'editMonthly') return t('system.salesTarget.editMonthly')
  return t('system.salesTarget.createMonthly')
})

// 月份名称
const monthName = computed(() => {
  return month.value
    ? t('system.salesTarget.monthName', {
        month: month.value,
        name: t(`system.salesTarget.months.${month.value}`)
      })
    : ''
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
        successMessage: t('system.salesTarget.createSuccess'),
        onSuccess: () => {
          visible.value = false
          emit('submit-success')
        }
      })
    } else if (mode.value === 'editAnnual') {
      // 更新年度目标
      if (!targetId.value) {
        console.error(t('system.salesTarget.targetIdMissing'))
        return
      }

      await doRequest(
        updateAnnualTarget(targetId.value, {
          id: targetId.value,
          targetAmount: formData.targetAmount!,
          remark: formData.remark
        }),
        {
          successMessage: t('system.salesTarget.updateSuccess'),
          onSuccess: () => {
            visible.value = false
            emit('submit-success')
          }
        }
      )
    } else if (mode.value === 'editMonthly') {
      // 更新月度目标
      if (!targetId.value) {
        console.error(t('system.salesTarget.targetIdMissing'))
        return
      }

      await doRequest(
        updateSingleMonthly(targetId.value, {
          id: targetId.value,
          targetAmount: formData.targetAmount!,
          remark: formData.remark
        }),
        {
          successMessage: t('system.salesTarget.updateSuccess'),
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
        successMessage: t('system.salesTarget.createSuccess'),
        onSuccess: () => {
          visible.value = false
          emit('submit-success')
        }
      })
    }
  } catch (error) {
    console.error(t('system.salesTarget.submitFailed'), error)
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
