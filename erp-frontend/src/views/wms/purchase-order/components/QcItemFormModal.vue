<template>
  <a-modal
    v-model:open="visible"
    :title="modalTitle"
    :width="560"
    :mask-closable="false"
    :destroy-on-close="true"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="formModel" layout="vertical">
      <!-- SKU 信息头部 -->
      <div class="sku-header">
        <sku-brief-cell :brief="currentSkuBrief" />
      </div>

      <!-- 包装尺寸区域 -->
      <div class="form-section">
        <div class="section-title">
          <inbox-outlined />
          <span>包装尺寸</span>
          <span v-if="calculatedVolume" class="volume-display">
            体积: {{ calculatedVolume }} m³
          </span>
        </div>
        <a-row :gutter="12">
          <a-col :span="8">
            <a-form-item
              label="长(cm)"
              name="lengthCm"
              :rules="[{ required: true, message: '请输入' }]"
            >
              <a-input-number
                v-model:value="formModel.lengthCm"
                :min="0.01"
                :precision="2"
                placeholder="长度"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              label="宽(cm)"
              name="widthCm"
              :rules="[{ required: true, message: '请输入' }]"
            >
              <a-input-number
                v-model:value="formModel.widthCm"
                :min="0.01"
                :precision="2"
                placeholder="宽度"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              label="高(cm)"
              name="heightCm"
              :rules="[{ required: true, message: '请输入' }]"
            >
              <a-input-number
                v-model:value="formModel.heightCm"
                :min="0.01"
                :precision="2"
                placeholder="高度"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 重量区域 -->
      <div class="form-section">
        <div class="section-title">
          <gold-outlined />
          <span>重量信息</span>
        </div>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item
              label="毛重(kg)"
              name="grossWeightKg"
              :rules="[{ required: true, message: '请输入' }]"
            >
              <a-input-number
                v-model:value="formModel.grossWeightKg"
                :min="0.001"
                :precision="3"
                placeholder="毛重"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="净重(kg)"
              name="netWeightKg"
              :rules="[{ required: true, message: '请输入' }, { validator: validateNetWeight }]"
            >
              <a-input-number
                v-model:value="formModel.netWeightKg"
                :min="0.001"
                :precision="3"
                placeholder="净重"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <!-- 质检报告区域 -->
      <div class="form-section">
        <div class="section-title">
          <file-protect-outlined />
          <span>质检报告</span>
        </div>
        <a-form-item name="qcFileId" style="margin-bottom: 0">
          <sys-file-upload
            v-model="formModel.qcFileId"
            bucket-key="private-files"
            button-text="上传质检报告"
            :allowed-types="qcFileAllowedTypes"
          />
          <div class="upload-hint">支持 PDF、JPG、PNG 格式</div>
        </a-form-item>
      </div>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { InboxOutlined, GoldOutlined, FileProtectOutlined } from '@ant-design/icons-vue'
import { SysFileUpload } from '@/components/Upload'
import { SkuBriefCell } from '@/components/Sku'
import type { QcItemDTO, SkuBriefVO } from '@/api/wms/purchase-order/types'

interface Props {
  skuBriefMap?: Record<string, SkuBriefVO>
}

const props = withDefaults(defineProps<Props>(), {
  skuBriefMap: () => ({})
})

const emits = defineEmits<{
  (e: 'save', data: QcItemDTO): void
}>()

// 弹窗状态
const visible = ref(false)
const modalTitle = ref('编辑质检数据')
const formRef = ref<FormInstance>()

// 质检报告允许的文件类型
const qcFileAllowedTypes = ['application/pdf', 'image/jpeg', 'image/png']

// 表单模型
const formModel = reactive<QcItemDTO>({
  skuCode: '',
  lengthCm: undefined,
  widthCm: undefined,
  heightCm: undefined,
  grossWeightKg: undefined,
  netWeightKg: undefined,
  qcFileId: undefined
})

// 当前 SKU 展示信息
const currentSkuBrief = computed(() => {
  return props.skuBriefMap[formModel.skuCode]
})

// 计算体积 (cm³ -> m³)
const calculatedVolume = computed(() => {
  const { lengthCm, widthCm, heightCm } = formModel
  if (lengthCm && widthCm && heightCm) {
    const volumeM3 = (lengthCm * widthCm * heightCm) / 1000000
    return volumeM3.toFixed(6)
  }
  return null
})

// 净重校验
const validateNetWeight = async (_rule: Rule, value: number) => {
  if (value && formModel.grossWeightKg && value > formModel.grossWeightKg) {
    return Promise.reject('净重不能大于毛重')
  }
  return Promise.resolve()
}

// 打开弹窗
const open = (item: QcItemDTO) => {
  modalTitle.value = '编辑质检数据'

  // 填充表单数据
  formModel.skuCode = item.skuCode
  formModel.lengthCm = item.lengthCm
  formModel.widthCm = item.widthCm
  formModel.heightCm = item.heightCm
  formModel.grossWeightKg = item.grossWeightKg
  formModel.netWeightKg = item.netWeightKg
  formModel.qcFileId = item.qcFileId

  visible.value = true
}

// 确定
const handleOk = async () => {
  try {
    await formRef.value?.validate()

    emits('save', {
      skuCode: formModel.skuCode,
      lengthCm: formModel.lengthCm,
      widthCm: formModel.widthCm,
      heightCm: formModel.heightCm,
      grossWeightKg: formModel.grossWeightKg,
      netWeightKg: formModel.netWeightKg,
      qcFileId: formModel.qcFileId
    })

    visible.value = false
  } catch {
    // 校验失败
  }
}

// 取消
const handleCancel = () => {
  visible.value = false
}

defineExpose({ open })
</script>

<style scoped>
.sku-header {
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 16px;
}

.form-section {
  margin-bottom: 16px;
}

.form-section:last-child {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title :deep(.anticon) {
  color: #1890ff;
  font-size: 16px;
}

.volume-display {
  margin-left: auto;
  font-size: 12px;
  font-weight: normal;
  color: #1890ff;
  background: #e6f7ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.upload-hint {
  color: #8c8c8c;
  font-size: 12px;
  margin-top: 4px;
}

/* 调整 vertical 布局下的 form-item 间距 */
:deep(.ant-form-item) {
  margin-bottom: 12px;
}

:deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

:deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: #595959;
}
</style>
