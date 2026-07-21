<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :body-style="{ paddingBottom: '8px' }"
    :confirm-loading="submitLoading"
    :width="640"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="labelCol" :wrapper-col="wrapperCol">
      <a-form-item v-if="isUpdateForm" style="display: none">
        <a-input v-model:value="formModel.id" />
      </a-form-item>
      <a-form-item label="平台商品ID" v-bind="validateInfos.platformItemId">
        <a-input
          v-model:value="formModel.platformItemId"
          placeholder="请输入平台商品ID(精确)"
          @blur="triggerValidateUnique"
        />
      </a-form-item>
      <a-form-item label="ERP SKU" v-bind="validateInfos.skuCode">
        <sku-select-input
          v-model="formModel.skuCode"
          placeholder="选择或输入SKU编码"
          @sku-selected="onSkuSelected"
        />
      </a-form-item>
      <a-form-item label="SKU信息">
        <div v-if="selectedSkuPreview" class="sku-inline">
          <img :src="selectedSkuPreview.image" class="sku-thumb" />
          <div class="sku-texts">
            <div class="sku-line name" :title="selectedSkuPreview.name">
              {{ selectedSkuPreview.name }}
            </div>
            <div class="sku-line cat" :title="selectedSkuPreview.categoryPath">
              {{ selectedSkuPreview.categoryPath }}
            </div>
          </div>
        </div>
        <div v-else class="sku-empty">尚未选择 SKU</div>
      </a-form-item>
      <a-alert
        v-if="uniqueError"
        type="error"
        :message="uniqueError"
        show-icon
        style="margin-bottom: 8px"
      />
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import { useModal } from '@/hooks/modal'
import { FormAction, useAdminForm, useFormAction } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import type { SkuMappingDTO, SkuMappingPageVO } from '@/api/product/sku-mapping/types'
import { createSkuMapping, updateSkuMapping } from '@/api/product/sku-mapping'
// 移除内联 SKU 选择逻辑，使用组件
import SkuSelectInput from '@/components/Sku/SkuSelectInput.vue'
import { pageSku } from '@/api/product/sku'
import { getSkuMainImage } from '@/utils/sku-utils'
import httpClient from '@/utils/axios'
import { doRequest } from '@/utils/axios/request'
import { overrideProperties } from '@/utils/bean-utils'
import type { ColProps } from 'ant-design-vue'

const labelCol: ColProps = {
  sm: { span: 24 },
  md: { span: 6 }
}

const wrapperCol: ColProps = {
  sm: { span: 24 },
  md: { span: 16 }
}

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isUpdateForm } = useFormAction()

// 表单模型
const formModel = reactive<SkuMappingDTO>({
  id: undefined,
  platformItemId: undefined,
  skuCode: undefined
})

// 表单的校验规则
const formRule = reactive({
  platformItemId: [{ required: true, message: '平台商品ID不能为空' }],
  skuCode: [{ required: true, message: 'SKU编码不能为空' }]
})

// 表单的提交请求
const formRequestMapping: FormRequestMapping<SkuMappingDTO> = {
  [FormAction.CREATE]: createSkuMapping,
  [FormAction.UPDATE]: updateSkuMapping
}

const { submitLoading, validateAndSubmit, resetFields, validateInfos } = useAdminForm(
  formAction,
  formRequestMapping,
  formModel,
  formRule
)

// 唯一性校验反馈
const uniqueError = ref('')
const triggerValidateUnique = () => {
  uniqueError.value = ''
  if (!formModel.platformItemId) return
  // GET /product/sku-mapping/validate
  doRequest(
    httpClient.get('/product/sku-mapping/validate', {
      params: { platformItemId: formModel.platformItemId, excludeId: formModel.id }
    }),
    {
      onSuccess: (r: any) => {
        if (r.code === 200 && r.data === false) {
          uniqueError.value = '该平台商品在该店铺下已存在映射'
        }
      }
    }
  )
}

// 接收来自抽取组件的 SKU 选择结果
const onSkuSelected = (row: any) => {
  if (row) {
    selectedSkuPreview.value = {
      image: getSkuMainImage(row.files, ossDomain),
      name: row.chineseName || row.skuCode,
      categoryPath: row.categoryHierarchy?.fullPathName || '-'
    }
  }
}

/* 表单提交处理 */
const handleSubmit = () => {
  if (uniqueError.value) {
    message.error('请先处理唯一性冲突')
    return
  }
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
  open(newFormAction: FormAction, record?: SkuMappingPageVO) {
    openModal()
    resetFields()
    if (newFormAction === FormAction.CREATE) {
      title.value = '新建SKU映射'
      selectedSkuPreview.value = null
    } else {
      title.value = '编辑SKU映射'
      // 不调用 resetFields() 以免覆盖现有值；直接赋值到 formModel
      overrideProperties(formModel, record)
      const promises: Promise<any>[] = []
      if (record?.skuCode) {
        promises.push(
          pageSku({ current: 1, size: 1 }, { skuCode: record.skuCode }).then((res: any) => {
            if (res.code === 200 && res.data?.records?.length) {
              const row = res.data.records[0]
              selectedSkuPreview.value = {
                image: getSkuMainImage(row.files, ossDomain),
                name: row.chineseName || row.skuCode,
                categoryPath: row.categoryHierarchy?.fullPathName || '-'
              }
            } else {
              selectedSkuPreview.value = null
            }
          })
        )
      } else {
        selectedSkuPreview.value = null
      }
      Promise.all(promises).catch(() => {})
    }
    formAction.value = newFormAction
  }
})

// 预览相关
const selectedSkuPreview = ref<{ image: string; name: string; categoryPath: string } | null>(null)
const ossDomain = import.meta.env.VITE_OSS_DOMAIN

// 若编辑模式且已有 skuCode，可在打开时尝试回填 (后续可扩展通过接口获取)
</script>

<style scoped>
.info-inline {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.info-primary {
  font-weight: 600;
  color: #262626;
}

.info-sub {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.info-placeholder {
  color: #bfbfbf;
}

.sku-inline {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fafafa;
  padding: 6px 10px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.sku-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.sku-texts {
  flex: 1;
  min-width: 0;
}

.sku-line {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.sku-line.name {
  font-size: 13px;
  font-weight: 600;
  color: #262626;
}

.sku-line.cat {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.sku-empty {
  color: #8c8c8c;
}

.sku-empty a {
  margin-left: 4px;
}
</style>
