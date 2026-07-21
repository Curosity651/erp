<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :body-style="{ paddingBottom: '8px' }"
    :confirm-loading="submitLoading"
    :width="500"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="labelCol" :wrapper-col="wrapperCol">
      <a-form-item v-if="isUpdateForm" style="display: none">
        <a-input v-model:value="formModel.id" />
      </a-form-item>
      <a-form-item label="品牌名称" v-bind="validateInfos.name">
        <a-input v-model:value="formModel.name" placeholder="请输入品牌名称" />
      </a-form-item>
      <a-form-item label="品牌编码" v-bind="validateInfos.code">
        <a-input
          v-model:value="formModel.code"
          placeholder="请输入品牌编码"
          :disabled="isUpdateForm"
        />
      </a-form-item>
      <a-form-item label="品牌LOGO">
        <div class="logo-upload-container">
          <!-- 文件上传组件 -->
          <a-upload
            :custom-request="handleLogoUpload"
            :before-upload="beforeLogoUpload"
            :show-upload-list="false"
            accept=".jpg,.jpeg,.png,.gif,.webp"
            class="logo-uploader"
          >
            <div class="upload-area" :class="{ 'has-logo': formModel.logoUrl }">
              <div v-if="formModel.logoUrl" class="logo-preview">
                <a-image
                  :src="getLogoDisplayUrl()"
                  :width="80"
                  :height="80"
                  style="border-radius: 4px; object-fit: cover"
                  :preview="{ mask: '预览' }"
                />
                <div class="logo-overlay">
                  <div class="overlay-actions">
                    <EyeOutlined @click.stop="previewLogo" />
                    <DeleteOutlined @click.stop="removeLogo" />
                  </div>
                </div>
              </div>
              <div v-else class="upload-placeholder">
                <LoadingOutlined v-if="logoUploading" />
                <PlusOutlined v-else />
                <div class="upload-text">
                  {{ logoUploading ? '上传中...' : '上传LOGO' }}
                </div>
              </div>
            </div>
          </a-upload>

          <!-- 上传提示 -->
          <div class="upload-tips">
            <div class="tip-item">
              <InfoCircleOutlined />
              支持 JPG、PNG、GIF、WebP 格式，建议尺寸 200x200px，大小不超过 2MB
            </div>
          </div>
        </div>
      </a-form-item>
      <a-form-item label="原产国家/地区">
        <a-input v-model:value="formModel.originCountry" placeholder="请输入原产国家/地区" />
      </a-form-item>
      <a-form-item label="品牌介绍">
        <a-textarea
          v-model:value="formModel.description"
          placeholder="请输入品牌介绍"
          :rows="3"
          :maxlength="500"
          show-count
        />
      </a-form-item>
      <a-form-item label="状态">
        <dict-radio-group v-model:value="formModel.status" dict-code="enable_status" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { useModal } from '@/hooks/modal'
import { FormAction, useAdminForm, useFormAction } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import type { BrandDTO, BrandPageVO } from '@/api/product/brand/types'
import { createBrand, updateBrand } from '@/api/product/brand'
import { overrideProperties } from '@/utils/bean-utils'
import type { ColProps } from 'ant-design-vue'
import { DictRadioGroup } from '@/components/Dict'
import {
  PlusOutlined,
  LoadingOutlined,
  EyeOutlined,
  DeleteOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'
import { uploadToOSSWithCache } from '@/hooks/use-oss-upload'
import { fileAbsoluteUrl } from '@/utils/file-utils'
import { message } from 'ant-design-vue'
import { isSuccess } from '@/api'

const labelCol: ColProps = {
  sm: { span: 24 },
  md: { span: 5 }
}

const wrapperCol: ColProps = {
  sm: { span: 24 },
  md: { span: 19 }
}

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isUpdateForm } = useFormAction()

// 表单模型
const formModel = reactive<BrandDTO>({
  // 品牌ID
  id: undefined,
  // 品牌名称
  name: undefined,
  // 品牌编码，唯一
  code: undefined,
  // 品牌LOGO URL
  logoUrl: undefined,
  // 品牌原产国家/地区
  originCountry: undefined,
  // 品牌介绍
  description: undefined,
  // 状态（1-启用，0-停用）
  status: 1
})

// 表单的校验规则
const formRule = reactive({
  name: [
    { required: true, message: '请输入品牌名称', trigger: 'blur' },
    { max: 100, message: '品牌名称长度不能超过100个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入品牌编码', trigger: 'blur' },
    { max: 30, message: '品牌编码长度不能超过30个字符', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_-]+$/,
      message: '品牌编码只能包含字母、数字、下划线和横线',
      trigger: 'blur'
    }
  ]
})

// 表单的提交请求
const formRequestMapping: FormRequestMapping<BrandDTO> = {
  [FormAction.CREATE]: createBrand,
  [FormAction.UPDATE]: updateBrand
}

const { submitLoading, validateAndSubmit, resetFields, validateInfos } = useAdminForm(
  formAction,
  formRequestMapping,
  formModel,
  formRule
)

// LOGO 上传相关状态
const logoUploading = ref(false)

// 获取 LOGO 显示 URL
const getLogoDisplayUrl = () => {
  if (!formModel.logoUrl) return ''
  return fileAbsoluteUrl(formModel.logoUrl)
}

// LOGO 上传前验证
const beforeLogoUpload = (file: File) => {
  // 文件类型验证
  const isValidType = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'].includes(
    file.type
  )
  if (!isValidType) {
    message.error('只能上传 JPG、PNG、GIF、WebP 格式的图片!')
    return false
  }

  // 文件大小验证 (2MB)
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB!')
    return false
  }

  return true
}

// 处理 LOGO 上传
const handleLogoUpload = async (options: any) => {
  const { file, onProgress, onSuccess, onError } = options

  logoUploading.value = true

  try {
    // 使用带缓存的上传方法
    const uploadResult = await uploadToOSSWithCache(
      file,
      {
        addTimestamp: true,
        sanitizeFileName: true,
        filePrefix: 'logo',
        folder: 'brands/',
        timeFormat: 'timestamp'
      },
      percent => onProgress({ percent })
    )

    // 更新表单数据 - 保存 objectKey
    formModel.logoUrl = uploadResult.objectKey

    onSuccess(uploadResult, file)
    message.success('LOGO 上传成功')
  } catch (error: any) {
    console.error('LOGO 上传失败:', error)
    message.error(`LOGO 上传失败: ${error.message || '未知错误'}`)
    onError(error)
  } finally {
    logoUploading.value = false
  }
}

// 预览 LOGO
const previewLogo = (event: Event) => {
  event.stopPropagation()
  // 触发 Image 组件的预览功能
  const imageElement = event.target as HTMLElement
  const imageComponent = imageElement.closest('.logo-preview')?.querySelector('.ant-image')
  if (imageComponent) {
    // 通过点击图片来触发预览
    const img = imageComponent.querySelector('img') as HTMLImageElement
    if (img) {
      img.click()
    }
  }
}

// 移除 LOGO
const removeLogo = (event: Event) => {
  event.stopPropagation()

  // 简单确认
  if (confirm('确定要删除当前 LOGO 吗？')) {
    formModel.logoUrl = ''
    message.success('LOGO 已删除')
  }
}

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
  open(newFormAction: FormAction, record?: BrandPageVO) {
    openModal()
    resetFields()

    // 重置 LOGO 上传状态
    logoUploading.value = false

    if (newFormAction === FormAction.CREATE) {
      title.value = '新建品牌'
      // 新建时清空 LOGO
      formModel.logoUrl = ''
    } else {
      title.value = '编辑品牌'
      overrideProperties(formModel, record)
    }
    formAction.value = newFormAction
  }
})
</script>

<style scoped>
.logo-upload-container {
  .logo-uploader {
    .upload-area {
      width: 100px;
      height: 100px;
      border: 2px dashed #d9d9d9;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;

      &:hover {
        border-color: #1890ff;
        background-color: #f0f9ff;
      }

      &.has-logo {
        border: 2px solid #e8e8e8;
        padding: 0;

        &:hover {
          border-color: #1890ff;
          background-color: transparent;
        }
      }

      .logo-preview {
        position: relative;
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;

        .logo-overlay {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.6);
          display: flex;
          align-items: center;
          justify-content: center;
          opacity: 0;
          transition: opacity 0.3s ease;

          .overlay-actions {
            display: flex;
            gap: 12px;

            .anticon {
              color: white;
              font-size: 16px;
              cursor: pointer;
              padding: 4px;
              border-radius: 4px;
              transition: background-color 0.3s ease;

              &:hover {
                background-color: rgba(255, 255, 255, 0.2);
              }
            }
          }
        }

        &:hover .logo-overlay {
          opacity: 1;
        }
      }

      .upload-placeholder {
        text-align: center;
        color: #666;

        .anticon {
          font-size: 24px;
          margin-bottom: 8px;
          color: #1890ff;
        }

        .upload-text {
          font-size: 12px;
          color: #666;
        }
      }
    }
  }

  .upload-tips {
    margin-top: 8px;

    .tip-item {
      display: flex;
      align-items: flex-start;
      gap: 6px;
      font-size: 12px;
      color: #8c8c8c;
      line-height: 1.4;

      .anticon {
        margin-top: 2px;
        flex-shrink: 0;
      }
    }
  }
}

:deep(.ant-form-item-label) {
  font-weight: 500;
}

:deep(.ant-form-item) {
  margin-bottom: 16px;
}
</style>
