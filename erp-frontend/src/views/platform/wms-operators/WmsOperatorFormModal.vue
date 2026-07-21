<template>
  <a-modal
    v-model:open="open"
    title="开通 WMS 服务商"
    :confirm-loading="submitting"
    :width="560"
    @ok="submit"
    @cancel="open = false"
  >
    <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="服务商名称" required>
        <a-input v-model:value="form.tenantName" placeholder="公司名称" />
      </a-form-item>
      <a-form-item label="服务商编码" required>
        <a-input v-model:value="form.tenantCode" placeholder="英文短码，全局唯一" />
      </a-form-item>
      <a-form-item label="管理员账号" required>
        <a-input v-model:value="form.adminUsername" placeholder="登录用户名，全局唯一" />
      </a-form-item>
      <a-form-item label="初始密码" required>
        <a-input-password v-model:value="form.adminPassword" placeholder="管理员初始密码" />
      </a-form-item>
      <a-form-item label="管理员昵称">
        <a-input v-model:value="form.adminNickname" placeholder="默认取服务商名称" />
      </a-form-item>
      <a-form-item label="联系人">
        <a-input v-model:value="form.contactName" />
      </a-form-item>
      <a-form-item label="联系电话">
        <a-input v-model:value="form.contactPhone" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="form.remark" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'
import { openWmsOperator } from '@/api/tenant'
import type { OpenTenantParam } from '@/api/tenant/types'

const emits = defineEmits<{ (e: 'success'): void }>()

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
    message.warning('服务商名称/编码、管理员账号/密码不能为空')
    return
  }
  submitting.value = true
  try {
    const res = await openWmsOperator(form)
    if (isSuccess(res)) {
      message.success('开通成功')
      open.value = false
      emits('success')
    } else {
      message.error(res.message || '开通失败')
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
