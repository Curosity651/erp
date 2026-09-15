<template>
  <a-modal
    v-model:open="open"
    :title="t('platform.operator.openTitle')"
    :confirm-loading="submitting"
    :width="560"
    @ok="submit"
    @cancel="open = false"
  >
    <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item :label="t('platform.operator.name')" required>
        <a-input v-model:value="form.tenantName" :placeholder="t('platform.operator.companyPlaceholder')" />
      </a-form-item>
      <a-form-item :label="t('platform.operator.code')" required>
        <a-input v-model:value="form.tenantCode" :placeholder="t('platform.operator.uniqueCodePlaceholder')" />
      </a-form-item>
      <a-form-item :label="t('platform.operator.adminAccount')" required>
        <a-input v-model:value="form.adminUsername" :placeholder="t('platform.operator.adminAccountPlaceholder')" />
      </a-form-item>
      <a-form-item :label="t('platform.operator.initialPassword')" required>
        <a-input-password v-model:value="form.adminPassword" :placeholder="t('platform.operator.initialPasswordPlaceholder')" />
      </a-form-item>
      <a-form-item :label="t('platform.operator.adminNickname')">
        <a-input v-model:value="form.adminNickname" :placeholder="t('platform.operator.adminNicknamePlaceholder')" />
      </a-form-item>
      <a-form-item :label="t('platform.operator.contactName')">
        <a-input v-model:value="form.contactName" />
      </a-form-item>
      <a-form-item :label="t('platform.operator.contactPhone')">
        <a-input v-model:value="form.contactPhone" />
      </a-form-item>
      <a-form-item :label="t('platform.operator.remark')">
        <a-textarea v-model:value="form.remark" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { openWmsOperator } from '@/api/tenant'
import type { OpenTenantParam } from '@/api/tenant/types'

const emits = defineEmits<{ (e: 'success'): void }>()
const { t } = useI18n()

const open = ref(false)
const submitting = ref(false)

function emptyForm(): OpenTenantParam {
  return {
    tenantCode: '',
    tenantName: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    remark: '',
    adminUsername: '',
    adminPassword: '',
    adminNickname: ''
  }
}
const form = reactive<OpenTenantParam>(emptyForm())

function openCreate() {
  Object.assign(form, emptyForm())
  open.value = true
}

async function submit() {
  if (!form.tenantName || !form.tenantCode || !form.adminUsername || !form.adminPassword) {
    message.warning(t('platform.operator.required'))
    return
  }
  submitting.value = true
  try {
    const res = await openWmsOperator(form)
    if (isSuccess(res)) {
      message.success(t('platform.operator.opened'))
      open.value = false
      emits('success')
    } else {
      message.error(res.message || t('platform.operator.openFailed'))
    }
  } finally {
    submitting.value = false
  }
}

defineExpose({ openCreate })
</script>

<script lang="ts">
export default {
  name: 'WmsOperatorFormModal'
}
</script>
