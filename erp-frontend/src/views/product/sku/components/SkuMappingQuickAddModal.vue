<template>
  <a-modal
    :open="open"
    title="添加平台映射"
    :width="600"
    :confirm-loading="submitLoading"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <!-- SKU 信息展示 -->
      <a-form-item label="SKU 信息">
        <div class="sku-info-display">
          <div class="sku-image-wrapper">
            <img :src="getProductImageUrl()" alt="产品图片" class="sku-image" />
          </div>
          <div class="sku-text-info">
            <span class="sku-code">{{ skuCode }}</span>
            <span class="sku-name">{{ skuName }}</span>
          </div>
        </div>
      </a-form-item>

      <!-- 平台商品 ID 输入 -->
      <a-form-item label="平台商品 ID" v-bind="validateInfos.platformItemId" required>
        <a-input
          v-model:value="formModel.platformItemId"
          placeholder="请输入平台商品ID"
          @blur="handleBlur"
        />
        <div class="form-tip">请输入平台商品的唯一标识符</div>
      </a-form-item>

      <!-- 唯一性错误提示 -->
      <a-alert
        v-if="uniqueError"
        type="error"
        :message="uniqueError"
        show-icon
        style="margin-bottom: 16px"
      />
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { Form } from 'ant-design-vue'
import { quickCreateSkuMapping } from '@/api/product/sku-mapping'
import httpClient from '@/utils/axios'

interface Props {
  open: boolean
  skuCode: string
  skuName?: string
  skuFiles?: any
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const useForm = Form.useForm

import { getSkuMainImage } from '@/utils/sku-utils'

const submitLoading = ref(false)
const uniqueError = ref('')

const ossDomain = import.meta.env.VITE_OSS_DOMAIN

// 获取产品图片URL
const getProductImageUrl = () => {
  return getSkuMainImage(props.skuFiles, ossDomain)
}

// 表单模型
const formModel = reactive({
  platformItemId: ''
})

// 表单验证规则
const formRules = reactive({
  platformItemId: [{ required: true, message: '平台商品ID不能为空', trigger: 'blur' }]
})

const { validateInfos, validate, resetFields } = useForm(formModel, formRules)

// 监听 visible 变化，重置表单
watch(
  () => props.open,
  newVal => {
    if (newVal) {
      resetFields()
      uniqueError.value = ''
    }
  }
)

// 失焦时验证唯一性
const handleBlur = async () => {
  uniqueError.value = ''
  if (!formModel.platformItemId) return

  try {
    const res = await httpClient.get('/product/sku-mapping/validate', {
      params: {
        platformItemId: formModel.platformItemId
      }
    })
    if (res.code === 200 && res.data === false) {
      uniqueError.value = '该平台商品已存在映射'
    }
  } catch (error) {
    console.error('验证唯一性失败:', error)
  }
}

// 提交表单
const handleSubmit = async () => {
  if (uniqueError.value) {
    message.error('请先处理唯一性冲突')
    return
  }

  try {
    await validate()
  } catch (error) {
    return
  }

  submitLoading.value = true
  try {
    const res = await quickCreateSkuMapping({
      skuCode: props.skuCode,
      platformItemId: formModel.platformItemId
    })

    if (res.code === 200) {
      message.success('添加映射成功')
      emit('success')
      handleClose()
    }
  } catch (error: any) {
    console.error('添加映射失败:', error)
    const errorMsg = error?.response?.data?.message || error?.message || '添加映射失败'
    message.error(errorMsg)
  } finally {
    submitLoading.value = false
  }
}

// 关闭模态框
const handleClose = () => {
  emit('update:open', false)
}
</script>

<style scoped>
.sku-info-display {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.sku-image-wrapper {
  flex-shrink: 0;
  width: 50px;
  height: 50px;
  border: 2px solid #e8e8e8;
  border-radius: 6px;
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f0 100%);
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}

.sku-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sku-text-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sku-code {
  font-size: 13px;
  font-weight: 600;
  color: #262626;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.sku-name {
  font-size: 12px;
  color: #595959;
}

.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}
</style>
