<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :confirm-loading="submitLoading"
    :width="600"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }">
      <a-form-item v-if="isUpdateForm" style="display: none">
        <a-input v-model:value="formModel.id" />
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="仓库编码" v-bind="validateInfos.warehouseCode">
            <a-input
              v-model:value="formModel.warehouseCode"
              placeholder="请输入仓库编码"
              :disabled="isUpdateForm"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="仓库名称" v-bind="validateInfos.warehouseName">
            <a-input v-model:value="formModel.warehouseName" placeholder="请输入仓库名称" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="仓库类型" v-bind="validateInfos.warehouseType">
        <a-radio-group
          v-model:value="formModel.warehouseType"
          :disabled="isUpdateForm"
          option-type="button"
          button-style="solid"
        >
          <a-radio-button value="OWN">自有仓</a-radio-button>
          <a-radio-button value="FBO" :disabled="!isUpdateForm">FBO仓</a-radio-button>
        </a-radio-group>
        <div v-if="!isUpdateForm" class="form-hint">
          FBO仓库请使用「从平台同步仓库」功能导入
        </div>
      </a-form-item>

      <!-- 所属区域 -->
      <a-form-item label="所属区域" v-bind="validateInfos.regionId">
        <region-select v-model:value="formModel.regionId" />
      </a-form-item>

      <!-- FBO配置 -->
      <template v-if="formModel.warehouseType === 'FBO'">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="关联平台" v-bind="validateInfos.platform">
              <PlatformSelect
                v-model:value="formModel.platform"
                placeholder="请选择关联平台"
                :allow-clear="false"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="平台仓库ID" v-bind="validateInfos.platformWarehouseId">
              <a-input
                v-model:value="formModel.platformWarehouseId"
                placeholder="请输入平台仓库ID"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </template>

      <!-- 联系方式 -->
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="联系人">
            <a-input v-model:value="formModel.contactName" placeholder="请输入联系人" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系电话">
            <a-input v-model:value="formModel.contactPhone" placeholder="请输入联系电话" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="仓库地址">
            <a-textarea
              v-model:value="formModel.address"
              placeholder="请输入仓库地址"
              :rows="2"
              :maxlength="200"
              show-count
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="启用状态" v-bind="validateInfos.status">
            <a-radio-group v-model:value="formModel.status">
              <a-radio :value="1">启用</a-radio>
              <a-radio :value="0">停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注说明">
        <a-textarea
          v-model:value="formModel.remark"
          placeholder="请输入备注（可选）"
          :rows="2"
          :maxlength="500"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { useModal } from '@/hooks/modal'
import { FormAction, useAdminForm, useFormAction } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import type { WarehouseDTO, WarehousePageVO } from '@/api/wms/warehouse/types'
import { createWarehouse, updateWarehouse } from '@/api/wms/warehouse'
import { PlatformSelect } from '@/components/Platform'
import RegionSelect from '@/components/Lov/RegionSelect.vue'
import { overrideProperties } from '@/utils/bean-utils'

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isUpdateForm } = useFormAction()

// 表单模型
const formModel = reactive<WarehouseDTO>({
  id: undefined,
  warehouseCode: '',
  warehouseName: '',
  warehouseType: '',
  regionId: undefined,
  platform: undefined,
  platformWarehouseId: undefined,
  address: undefined,
  contactName: undefined,
  contactPhone: undefined,
  status: 1,
  remark: undefined
})

// 表单的校验规则
const formRule = reactive({
  warehouseCode: [
    { required: true, message: '请输入仓库编码', trigger: 'blur' },
    { max: 50, message: '仓库编码长度不能超过50个字符', trigger: 'blur' }
  ],
  warehouseName: [
    { required: true, message: '请输入仓库名称', trigger: 'blur' },
    { max: 100, message: '仓库名称长度不能超过100个字符', trigger: 'blur' }
  ],
  warehouseType: [{ required: true, message: '请选择仓库类型', trigger: 'change' }],
  regionId: [{ required: true, message: '请选择所属区域', trigger: 'change' }],
  platform: [
    {
      validator: (_rule: any, value: any) => {
        if (formModel.warehouseType === 'FBO' && !value) {
          return Promise.reject('FBO仓必须选择关联平台')
        }
        return Promise.resolve()
      },
      trigger: 'change'
    }
  ],
  platformWarehouseId: [
    {
      validator: (_rule: any, value: any) => {
        if (formModel.warehouseType === 'FBO' && !value) {
          return Promise.reject('FBO仓必须填写平台仓库ID')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

// 表单的提交请求
const formRequestMapping: FormRequestMapping<WarehouseDTO> = {
  [FormAction.CREATE]: createWarehouse,
  [FormAction.UPDATE]: updateWarehouse
}

const { submitLoading, validateAndSubmit, resetFields, validateInfos } = useAdminForm(
  formAction,
  formRequestMapping,
  formModel,
  formRule
)

/* 表单提交处理 */
const handleSubmit = () => {
  const model = { ...formModel }
  validateAndSubmit(model, {
    onSuccess: () => {
      closeModal()
      emits('submit-success')
    }
  })
}

/* 弹窗关闭方法 */
const handleClose = () => {
  closeModal()
  submitLoading.value = false
}

defineExpose({
  open(newFormAction: FormAction, record?: WarehousePageVO) {
    openModal()
    resetFields()

    if (newFormAction === FormAction.CREATE) {
      title.value = '新建仓库'
      formModel.warehouseCode = ''
      formModel.warehouseName = ''
      formModel.warehouseType = 'OWN'
      formModel.regionId = undefined
      formModel.platform = undefined
      formModel.platformWarehouseId = undefined
      formModel.address = undefined
      formModel.contactName = undefined
      formModel.contactPhone = undefined
      formModel.status = 1
      formModel.remark = undefined
    } else {
      title.value = '编辑仓库'
      overrideProperties(formModel, record)
    }
    formAction.value = newFormAction
  }
})
</script>

<style scoped>
/* 表单项样式 */
:deep(.ant-form-item) {
  margin-bottom: 16px;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}

.form-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ant-color-text-secondary);
}
</style>
