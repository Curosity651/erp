<template>
  <a-form :model="formModel">
    <a-row :gutter="16">
      <a-col :md="6" :sm="24">
        <a-form-item :label="t('system.role.userId')">
          <a-input-number
            v-model:value="formModel.userId"
            type="number"
            :placeholder="t('message.pleaseEnter')"
            style="width: 100%"
          />
        </a-form-item>
      </a-col>
      <a-col :md="6" :sm="24">
        <a-form-item :label="t('system.user.username')">
          <a-input v-model:value="formModel.username" :placeholder="t('message.pleaseEnter')" />
        </a-form-item>
      </a-col>
      <a-col :md="6" :sm="24">
        <a-form-item :label="t('system.user.organization')">
          <sys-organization-tree-select
            v-model:value="formModel.organizationId"
            :allow-clear="true"
            :placeholder="t('common.select')"
          />
        </a-form-item>
      </a-col>
      <a-col :md="6" :sm="24">
        <search-actions :loading="props.loading" @search="search" @reset="reset" />
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup lang="ts">
import SysOrganizationTreeSelect from '../organization/SysOrganizationTreeSelect.vue'
import type { SysRoleUserQO } from '@/api/system/role/types'
import { Form } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    loading?: boolean
    roleCode: string
  }>(),
  { loading: false }
)

const emits = defineEmits<{
  (e: 'search', params: SysRoleUserQO): void
}>()

const formModel = reactive<SysRoleUserQO>({
  roleCode: props.roleCode,
  userId: undefined,
  username: undefined,
  organizationId: undefined
})

const { resetFields } = Form.useForm(formModel)

const search = () => {
  emits('search', toRaw(formModel))
}

const reset = () => {
  resetFields()
  search()
}
</script>
