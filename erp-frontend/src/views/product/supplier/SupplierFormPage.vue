<template>
  <div class="supplier-form-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-left">
          <a-button type="text" class="back-btn" @click="handleBack">
            <arrow-left-outlined />
            返回列表
          </a-button>
          <div class="title-section">
            <h1 class="page-title">{{ title }}</h1>
            <p class="page-subtitle">请填写完整的供应商信息</p>
          </div>
        </div>
        <div class="header-right">
          <a-button style="margin-right: 12px" @click="handleBack">取消</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isUpdateForm ? '更新' : '保存' }}
          </a-button>
        </div>
      </div>
    </div>

    <!-- 表单内容 -->
    <div class="page-content">
      <a-form
        :model="formModel"
        :label-col="labelCol"
        :wrapper-col="wrapperCol"
        class="supplier-form"
        layout="horizontal"
      >
        <a-form-item v-if="isUpdateForm" style="display: none">
          <a-input v-model:value="formModel.id" />
        </a-form-item>

        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-header">
            <shop-outlined class="section-icon" />
            <span class="section-title">基本信息</span>
          </div>
          <div class="section-content">
            <a-row :gutter="32">
              <a-col :span="12">
                <a-form-item label="供应商编码" v-bind="validateInfos.supplierCode">
                  <a-input
                    v-model:value="formModel.supplierCode"
                    placeholder="请输入供应商编码"
                    :disabled="isUpdateForm"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="供应商名称" v-bind="validateInfos.name">
                  <a-input
                    v-model:value="formModel.name"
                    placeholder="请输入供应商名称"
                    size="large"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="32">
              <a-col :span="12">
                <a-form-item label="所在城市">
                  <a-input v-model:value="formModel.city" placeholder="请输入城市" size="large" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="税号">
                  <a-input
                    v-model:value="formModel.taxNumber"
                    placeholder="请输入税号"
                    size="large"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="32">
              <a-col :span="24">
                <a-form-item
                  label="详细地址"
                  :label-col="{ sm: { span: 24 }, md: { span: 3 } }"
                  :wrapper-col="{ sm: { span: 24 }, md: { span: 21 } }"
                >
                  <a-textarea
                    v-model:value="formModel.address"
                    placeholder="请输入详细地址"
                    :rows="3"
                    size="large"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </div>

        <!-- 法人信息 -->
        <div class="form-section">
          <div class="section-header">
            <user-outlined class="section-icon" />
            <span class="section-title">法人信息</span>
            <span class="section-subtitle">可选</span>
          </div>
          <div class="section-content">
            <a-row :gutter="32">
              <a-col :span="12">
                <a-form-item label="法人姓名">
                  <a-input
                    v-model:value="formModel.legalPersonName"
                    placeholder="请输入法人姓名"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="法人电话">
                  <a-input
                    v-model:value="formModel.legalPersonPhone"
                    placeholder="请输入法人电话"
                    size="large"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </div>

        <!-- 业务联系人信息 -->
        <div class="form-section required-section">
          <div class="section-header">
            <contacts-outlined class="section-icon" />
            <span class="section-title">业务联系人信息</span>
            <span class="required-badge">必填</span>
          </div>
          <div class="section-content">
            <a-row :gutter="32">
              <a-col :span="8">
                <a-form-item label="联系人" v-bind="validateInfos.businessContactName">
                  <a-input
                    v-model:value="formModel.businessContactName"
                    placeholder="请输入业务联系人"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="联系电话" v-bind="validateInfos.businessContactPhone">
                  <a-input
                    v-model:value="formModel.businessContactPhone"
                    placeholder="请输入联系电话"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="联系邮箱" v-bind="validateInfos.businessContactEmail">
                  <a-input
                    v-model:value="formModel.businessContactEmail"
                    placeholder="请输入联系邮箱"
                    size="large"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </div>

        <!-- 公账信息 -->
        <div class="form-section">
          <div class="section-header">
            <bank-outlined class="section-icon" />
            <span class="section-title">公账信息</span>
            <span class="section-subtitle">可选</span>
          </div>
          <div class="section-content">
            <a-row :gutter="32">
              <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :xxl="12">
                <a-form-item label="账户名称">
                  <a-input
                    v-model:value="formModel.publicAccountName"
                    placeholder="请输入公账账户名称"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :xxl="12">
                <a-form-item label="银行名称">
                  <a-input
                    v-model:value="formModel.publicBankName"
                    placeholder="请输入公账银行"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :xxl="12">
                <a-form-item label="开户行地址">
                  <a-input
                    v-model:value="formModel.publicBankAddress"
                    placeholder="请输入公账开户行地址"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :xxl="12">
                <a-form-item label="账户卡号">
                  <a-input
                    v-model:value="formModel.publicAccountNo"
                    placeholder="请输入公账账户卡号（可选）"
                    size="large"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </div>

        <!-- 私账信息 -->
        <div class="form-section required-section">
          <div class="section-header">
            <credit-card-outlined class="section-icon" />
            <span class="section-title">私账信息</span>
            <span class="required-badge">必填</span>
          </div>
          <div class="section-content">
            <a-row :gutter="32">
              <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :xxl="12">
                <a-form-item label="账户名称" v-bind="validateInfos.privateAccountName">
                  <a-input
                    v-model:value="formModel.privateAccountName"
                    placeholder="请输入私账账户名称"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :xxl="12">
                <a-form-item label="银行名称" v-bind="validateInfos.privateBankName">
                  <a-input
                    v-model:value="formModel.privateBankName"
                    placeholder="请输入私账银行"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :xxl="12">
                <a-form-item label="开户行地址" v-bind="validateInfos.privateBankAddress">
                  <a-input
                    v-model:value="formModel.privateBankAddress"
                    placeholder="请输入私账开户行地址"
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12" :xxl="12">
                <a-form-item label="账户卡号" v-bind="validateInfos.privateAccountNo">
                  <a-input
                    v-model:value="formModel.privateAccountNo"
                    placeholder="请输入私账账户卡号"
                    size="large"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </div>

        <!-- 营业执照 -->
        <div class="form-section required-section">
          <div class="section-header">
            <file-image-outlined class="section-icon" />
            <span class="section-title">营业执照</span>
            <span class="required-badge">必填</span>
          </div>
          <div class="section-content">
            <a-form-item label="营业执照照片" v-bind="validateInfos.businessLicensePhoto">
              <image-upload
                v-model="formModel.businessLicensePhoto"
                :preview-url="businessLicensePreviewUrl"
                placeholder="请上传营业执照照片"
              />
            </a-form-item>
          </div>
        </div>

        <!-- 其他信息 -->
        <div class="form-section">
          <div class="section-header">
            <setting-outlined class="section-icon" />
            <span class="section-title">其他信息</span>
          </div>
          <div class="section-content">
            <a-row :gutter="32">
              <a-col :span="12">
                <a-form-item label="状态">
                  <dict-radio-group v-model:value="formModel.status" dict-code="enable_status" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="32">
              <a-col :span="24">
                <a-form-item
                  label="备注"
                  :label-col="{ sm: { span: 24 }, md: { span: 3 } }"
                  :wrapper-col="{ sm: { span: 24 }, md: { span: 21 } }"
                >
                  <a-textarea
                    v-model:value="formModel.remarks"
                    placeholder="请输入备注"
                    :rows="4"
                    size="large"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { FormAction, useAdminForm, useFormAction } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import type { SupplierDTO, SupplierPageVO } from '@/api/product/supplier/types'
import {
  createSupplier,
  getBusinessLicenseUrl,
  updateSupplier
} from '@/api/product/supplier'
import { isSuccess } from '@/api'
import { overrideProperties } from '@/utils/bean-utils'
import type { ColProps } from 'ant-design-vue'
import { DictRadioGroup } from '@/components/Dict'
import ImageUpload from '@/components/ImageUpload/index.vue'
import {
  ArrowLeftOutlined,
  ShopOutlined,
  UserOutlined,
  ContactsOutlined,
  BankOutlined,
  CreditCardOutlined,
  FileImageOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'

const labelCol: ColProps = {
  sm: { span: 24 },
  md: { span: 6 }
}

const wrapperCol: ColProps = {
  sm: { span: 24 },
  md: { span: 18 }
}

const emits = defineEmits<{
  (e: 'submit-success'): void
  (e: 'cancel'): void
}>()

const { formAction, isUpdateForm } = useFormAction()
const businessLicensePreviewUrl = ref('')

const title = computed(() => {
  return isUpdateForm.value ? '编辑供应商' : '新建供应商'
})

// 表单模型
const formModel = reactive<SupplierDTO>({
  // 供应商ID
  id: 0,
  // 供应商编码，唯一标识
  supplierCode: '',
  // 供应商名称
  name: '',
  // 供应商所在城市
  city: '',
  // 详细地址
  address: '',
  // 税号
  taxNumber: '',
  // 法人姓名
  legalPersonName: '',
  // 法人电话
  legalPersonPhone: '',
  // 业务联系人
  businessContactName: '',
  // 业务联系人电话
  businessContactPhone: '',
  // 业务联系人邮箱
  businessContactEmail: '',
  // 公账账户名称
  publicAccountName: '',
  // 公账银行
  publicBankName: '',
  // 公账开户行地址
  publicBankAddress: '',
  // 公账账户卡号（非必填）
  publicAccountNo: '',
  // 私账账户名称
  privateAccountName: '',
  // 私账银行
  privateBankName: '',
  // 私账开户行地址
  privateBankAddress: '',
  // 私账账户卡号（必填）
  privateAccountNo: '',
  // 营业执照照片OSS key
  businessLicensePhoto: '',
  // 状态（1-启用，0-停用）
  status: 1,
  // 备注
  remarks: ''
})

// 表单的校验规则
const formRule = reactive({
  supplierCode: [
    { required: true, message: '请输入供应商编码', trigger: 'blur' },
    { max: 30, message: '供应商编码长度不能超过30个字符', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_-]+$/,
      message: '供应商编码只能包含字母、数字、下划线和横线',
      trigger: 'blur'
    }
  ],
  name: [
    { required: true, message: '请输入供应商名称', trigger: 'blur' },
    { max: 100, message: '供应商名称长度不能超过100个字符', trigger: 'blur' }
  ],
  businessContactName: [
    { required: true, message: '请输入业务联系人', trigger: 'blur' },
    { max: 100, message: '业务联系人长度不能超过100个字符', trigger: 'blur' }
  ],
  businessContactPhone: [
    { required: true, message: '请输入业务联系人电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  businessContactEmail: [
    { required: true, message: '请输入业务联系人邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  privateAccountName: [
    { required: true, message: '请输入私账账户名称', trigger: 'blur' },
    { max: 100, message: '私账账户名称长度不能超过100个字符', trigger: 'blur' }
  ],
  privateBankName: [
    { required: true, message: '请输入私账银行', trigger: 'blur' },
    { max: 100, message: '私账银行长度不能超过100个字符', trigger: 'blur' }
  ],
  privateBankAddress: [
    { required: true, message: '请输入私账开户行地址', trigger: 'blur' },
    { max: 255, message: '私账开户行地址长度不能超过255个字符', trigger: 'blur' }
  ],
  privateAccountNo: [
    { required: true, message: '请输入私账账户卡号', trigger: 'blur' },
    { max: 50, message: '账户卡号长度不能超过50个字符', trigger: 'blur' }
  ],
  businessLicensePhoto: [{ required: true, message: '请上传营业执照照片', trigger: 'change' }]
})

// 表单的提交请求
const formRequestMapping: FormRequestMapping<SupplierDTO> = {
  [FormAction.CREATE]: createSupplier,
  [FormAction.UPDATE]: updateSupplier
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
      emits('submit-success')
    }
  })
}

/* 返回列表 */
const handleBack = () => {
  emits('cancel')
}

/* 初始化表单 */
const initForm = (action: FormAction, record?: SupplierPageVO) => {
  resetFields()
  businessLicensePreviewUrl.value = ''
  formAction.value = action

  if (action === FormAction.CREATE) {
    // 重置表单数据到初始状态
    Object.assign(formModel, {
      id: 0,
      supplierCode: '',
      name: '',
      city: '',
      address: '',
      taxNumber: '',
      legalPersonName: '',
      legalPersonPhone: '',
      businessContactName: '',
      businessContactPhone: '',
      businessContactEmail: '',
      publicAccountName: '',
      publicBankName: '',
      publicBankAddress: '',
      publicAccountNo: '',
      privateAccountName: '',
      privateBankName: '',
      privateBankAddress: '',
      privateAccountNo: '',
      businessLicensePhoto: '',
      status: 1,
      remarks: ''
    })
  } else {
    overrideProperties(formModel, record)
    if (record?.id && record.businessLicensePhoto) {
      void getBusinessLicenseUrl(record.id)
        .then(res => {
          if (isSuccess(res)) {
            businessLicensePreviewUrl.value = res.data || ''
          }
        })
        .catch(error => {
          console.error('Failed to load business license preview URL:', error)
        })
    }
  }
}

defineExpose({
  initForm
})
</script>

<style scoped>
.supplier-form-page {
  margin: -16px;
  min-height: 100vh;
}

/* 页面头部 */
.page-header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 32px;
  max-width: 1200px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
}

.back-btn {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #595959;
  margin-right: 20px;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.3s;
}

.back-btn:hover {
  color: #1890ff;
  background: #f0f8ff;
}

.title-section {
  margin-left: 8px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  margin: 0;
  line-height: 1.2;
}

.page-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 4px 0 0 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 页面内容 */
.page-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

.supplier-form {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* 分组样式 - 现代化简洁设计 */
.form-section {
  border-bottom: 1px solid #f5f5f5;
}

.form-section:last-child {
  border-bottom: none;
}

.section-header {
  display: flex;
  align-items: center;
  padding: 24px 32px 16px;
  background: #fff;
}

.section-icon {
  font-size: 20px;
  color: #1890ff;
  margin-right: 12px;
  opacity: 0.8;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin-right: 12px;
}

.section-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  font-weight: 400;
  margin-left: auto;
  background: #f5f5f5;
  padding: 4px 12px;
  border-radius: 12px;
}

.required-badge {
  font-size: 12px;
  color: #ff4d4f;
  background: #fff2f0;
  padding: 4px 12px;
  border-radius: 12px;
  border: 1px solid #ffccc7;
  font-weight: 500;
  margin-left: auto;
}

.section-content {
  padding: 0 32px 32px;
}

/* 表单项样式 */
:deep(.ant-form-item) {
  margin-bottom: 24px;
}

:deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: #262626;
  font-size: 14px;
}

:deep(.ant-form-item-required:not(.ant-form-item-required-mark-optional)::before) {
  color: #ff4d4f;
}

/* 简洁输入框样式 */
:deep(.ant-input-affix-wrapper),
:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-picker) {
  border-radius: 6px;
  transition: all 0.3s;
}

:deep(.ant-input-affix-wrapper:hover),
:deep(.ant-input:hover),
:deep(.ant-select-selector:hover),
:deep(.ant-picker:hover) {
  border-color: #40a9ff;
}

:deep(.ant-input-affix-wrapper-focused),
:deep(.ant-input-focused),
:deep(.ant-select-focused .ant-select-selector),
:deep(.ant-picker-focused) {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 文本域样式 */
:deep(.ant-input) {
  line-height: 1.6;
}

/* 简洁单选按钮组样式 */
:deep(.ant-radio-group) {
  margin-top: 4px;
}

:deep(.ant-radio-wrapper) {
  margin-right: 24px;
  font-size: 14px;
}

/* 移除表单验证错误时的样式简化 */
:deep(.ant-form-item-has-error .ant-input),
:deep(.ant-form-item-has-error .ant-input-affix-wrapper),
:deep(.ant-form-item-has-error .ant-select-selector) {
  border-color: #ff4d4f;
}

:deep(.ant-form-item-has-error .ant-input:hover),
:deep(.ant-form-item-has-error .ant-input-affix-wrapper:hover),
:deep(.ant-form-item-has-error .ant-select-selector:hover) {
  border-color: #ff7875;
}

/* 简洁操作按钮样式 */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px 0;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  margin: 24px -32px -32px;
}

.action-buttons .ant-btn {
  height: 40px;
  padding: 0 24px;
  font-size: 14px;
  border-radius: 6px;
  min-width: 100px;
}

.action-buttons .ant-btn-primary {
  background: #1890ff;
  border-color: #1890ff;
}

.action-buttons .ant-btn-primary:hover {
  background: #40a9ff;
  border-color: #40a9ff;
}

.action-buttons .ant-btn-default {
  background: #fff;
  border-color: #d9d9d9;
  color: #595959;
}

.action-buttons .ant-btn-default:hover {
  border-color: #40a9ff;
  color: #40a9ff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .supplier-form-page {
    margin: 16px;
  }

  .form-container {
    padding: 20px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 12px;
    padding: 20px;
    margin: 20px -20px -20px;
  }

  .action-buttons .ant-btn {
    width: 100%;
  }
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .page-content {
    padding: 16px;
  }

  .header-content {
    padding: 12px 16px;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-right {
    align-self: stretch;
    justify-content: flex-end;
  }

  .section-content {
    padding: 24px 16px;
  }
}
</style>
