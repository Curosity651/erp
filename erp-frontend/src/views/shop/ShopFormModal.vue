<template>
  <a-modal
    :title="modalTitle"
    :open="visible"
    :mask-closable="false"
    :body-style="{ paddingBottom: '8px' }"
    width="600px"
    :ok-button-props="{ disabled: !canSave, loading: saving }"
    :ok-text="saving ? '保存中...' : '保存'"
    @ok="handleSave"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="ERP店铺名称" required>
        <a-input v-model:value="formModel.erpShopName" maxlength="100" />
      </a-form-item>

      <a-form-item label="平台" required>
        <PlatformRadioGroup
          v-model:value="formModel.platform"
          :disabled="!!formModel.id"
          @change="handlePlatformChange"
        />
      </a-form-item>

      <template v-if="formModel.platform">
        <template v-for="field in currentPlatformConfig.credentialFields" :key="field.key">
          <a-form-item :label="field.label" :required="field.required">
            <a-textarea
              v-if="field.inputType === 'textarea'"
              v-model:value="formModel.credential[field.key]"
              :rows="field.rows || 3"
              :auto-size="{ minRows: field.rows || 3, maxRows: 6 }"
              :maxlength="field.maxLength || 1024"
              show-count
              :placeholder="field.placeholder"
              :disabled="!editingCredential"
              @input="handleCredentialInput"
            />
            <a-input
              v-else
              v-model:value="formModel.credential[field.key]"
              :maxlength="field.maxLength || 128"
              :placeholder="field.placeholder"
              :disabled="!editingCredential"
              @input="handleCredentialInput"
            />

            <template v-if="field.key === 'api_key' && action === FormAction.UPDATE">
              <a-button type="link" size="small" @click="toggleEditingCredential(!editingCredential)">
                {{ editingCredential ? '取消修改凭证' : '修改凭证' }}
              </a-button>
            </template>
          </a-form-item>
        </template>
      </template>

      <a-form-item label="店铺名称" :required="requiresManualShopName">
        <template v-if="requiresManualShopName">
          <a-input v-model:value="formModel.shopName" :placeholder="currentPlatformConfig.shopNamePlaceholder" />
        </template>
        <template v-else>
          <a-input :value="formModel.shopName" disabled placeholder="测试成功后自动填充" />
        </template>
      </a-form-item>

      <a-form-item label="店铺ID" required>
        <a-input
          :value="displayPlatformShopId"
          disabled
          :placeholder="currentPlatformConfig.platformShopIdPlaceholder"
        />
      </a-form-item>

      <a-form-item label="默认WMS仓库" required>
        <a-select
          v-model:value="formModel.defaultWmsWarehouseId"
          :options="warehouseOptions"
          placeholder="请选择订单默认提交的海外仓"
        />
      </a-form-item>

      <a-form-item label="测试">
        <a-space>
          <a-button :loading="testing" :disabled="!canTest" @click="testCred">{{ testBtnText }}</a-button>
          <span v-if="state === 'TEST_SUCCESS'" style="color: #52c41a">测试成功</span>
          <span v-else-if="state === 'TEST_FAIL'" style="color: #ff4d4f">{{ testError }}</span>
        </a-space>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { createShop, updateShop, testCredential, getShop } from '@/api/shop'
import type {
  TestCredentialResponse,
  ShopDetailVO,
  CreateOrUpdateShopRequest,
  ShopVO
} from '@/api/shop/types'
import { PLATFORMS, type PlatformType } from '@/constants/platform'
import { FormAction } from '@/hooks/form'
import { PlatformRadioGroup } from '@/components/Platform'
import { message } from 'ant-design-vue'
import { getWarehouseOptions } from '@/api/wms/warehouse'

defineOptions({ name: 'ShopFormModal' })

type CredentialFieldConfig = {
  key: string
  label: string
  required: boolean
  inputType: 'input' | 'textarea'
  placeholder?: string
  maxLength?: number
  rows?: number
}

type PlatformFormConfig = {
  credentialFields: CredentialFieldConfig[]
  shopNameRequired: boolean
  shopNamePlaceholder?: string
  platformShopIdPlaceholder: string
  platformIdCredentialKey?: string
}

const PLATFORM_FORM_CONFIG: Record<PlatformType, PlatformFormConfig> = {
  [PLATFORMS.WILDBERRIES]: {
    credentialFields: [
      {
        key: 'api_key',
        label: 'API Key',
        required: true,
        inputType: 'textarea',
        placeholder: '请输入 API Key/JWT',
        maxLength: 1024,
        rows: 3
      }
    ],
    shopNameRequired: false,
    platformShopIdPlaceholder: '测试成功后自动填充'
  },
  [PLATFORMS.OZON]: {
    credentialFields: [
      {
        key: 'client_id',
        label: 'Client ID',
        required: true,
        inputType: 'input',
        placeholder: '请输入 Client ID',
        maxLength: 128
      },
      {
        key: 'api_key',
        label: 'API Key',
        required: true,
        inputType: 'textarea',
        placeholder: '请输入 API Key',
        maxLength: 1024,
        rows: 3
      }
    ],
    shopNameRequired: true,
    shopNamePlaceholder: '请手动填写 Ozon 店铺名称',
    platformShopIdPlaceholder: '自动使用 Client ID（测试后确认）',
    platformIdCredentialKey: 'client_id'
  },
  [PLATFORMS.YANDEX]: {
    credentialFields: [
      {
        key: 'business_id',
        label: 'Business ID',
        required: true,
        inputType: 'input',
        placeholder: '请输入 Business ID',
        maxLength: 64
      },
      {
        key: 'campaign_id',
        label: 'Campaign ID',
        required: true,
        inputType: 'input',
        placeholder: '请输入 Campaign ID',
        maxLength: 64
      },
      {
        key: 'api_key',
        label: 'API Key',
        required: true,
        inputType: 'textarea',
        placeholder: '请输入 API Key',
        maxLength: 1024,
        rows: 3
      }
    ],
    shopNameRequired: true,
    shopNamePlaceholder: '请手动填写 Yandex 店铺名称',
    platformShopIdPlaceholder: '自动使用 Campaign ID（测试后确认）',
    platformIdCredentialKey: 'campaign_id'
  }
}

const emits = defineEmits<{ (e: 'submit-success'): void }>()

const visible = ref(false)
const modalTitle = ref('')
const action = ref<FormAction>(FormAction.CREATE)

const formModel = reactive<{
  id?: number
  platform: PlatformType
  credential: Record<string, string>
  shopName?: string
  erpShopName?: string
  platformShopId?: string
  testToken?: string
  defaultWmsWarehouseId?: number
}>({
  platform: PLATFORMS.WILDBERRIES,
  credential: {}
})

type State = 'IDLE' | 'EDITING' | 'TESTING' | 'TEST_SUCCESS' | 'TEST_FAIL'
const state = ref<State>('IDLE')
const credentialDirty = ref(false)
const editingCredential = ref(false)
const originalMaskedCredential = ref<Record<string, string>>({})
const testError = ref<string | undefined>()
const saving = ref(false)
const warehouseOptions = ref<{ label: string; value: number }[]>([])

const currentPlatformConfig = computed(() => PLATFORM_FORM_CONFIG[formModel.platform])
const requiresManualShopName = computed(() => currentPlatformConfig.value.shopNameRequired)
const testing = computed(() => state.value === 'TESTING')

const inferredPlatformShopId = computed(() => {
  const idKey = currentPlatformConfig.value.platformIdCredentialKey
  if (!idKey) return undefined
  const value = formModel.credential[idKey]
  return hasText(value) ? value!.trim() : undefined
})

const displayPlatformShopId = computed(() => formModel.platformShopId || inferredPlatformShopId.value || '')

const canTest = computed(() => {
  if (action.value === FormAction.UPDATE && !editingCredential.value) return false
  return currentPlatformConfig.value.credentialFields
    .filter((field) => field.required)
    .every((field) => hasText(formModel.credential[field.key]))
})

const canSave = computed(() => {
  if (!hasText(formModel.erpShopName)) return false
  if (!formModel.defaultWmsWarehouseId) return false

  const needRetest =
    action.value === FormAction.CREATE ||
    (action.value === FormAction.UPDATE && credentialDirty.value)

  if (needRetest && state.value !== 'TEST_SUCCESS') return false

  if (!hasText(displayPlatformShopId.value)) return false

  if (requiresManualShopName.value && !hasText(formModel.shopName)) return false

  return true
})

const testBtnText = computed(() => {
  switch (state.value) {
    case 'TESTING':
      return '测试中...'
    case 'TEST_SUCCESS':
    case 'TEST_FAIL':
      return '重新测试'
    default:
      return '测试并获取信息'
  }
})

function hasText(value?: string) {
  return !!value && value.trim().length > 0
}

function open(newAction: FormAction, record?: ShopVO) {
  visible.value = true
  resetForm()
  action.value = newAction
  loadWarehouseOptions()

  if (newAction === FormAction.CREATE) {
    modalTitle.value = '新增店铺'
    state.value = 'EDITING'
    editingCredential.value = true
    return
  }

  if (record?.id) {
    modalTitle.value = '编辑店铺'
    state.value = 'EDITING'
    loadDetail(record.id)
  }
}

async function loadDetail(id: number) {
  const resp = await getShop(id)
  if (resp.code !== 200) return

  const data = resp.data as ShopDetailVO
  formModel.id = data.id
  formModel.platform = data.platform as PlatformType
  formModel.shopName = data.name
  formModel.erpShopName = data.erpShopName
  formModel.platformShopId = data.platformShopId
  formModel.defaultWmsWarehouseId = data.defaultWmsWarehouseId
  originalMaskedCredential.value = data.credentialMask || {}
  formModel.credential = { ...originalMaskedCredential.value }

  state.value = 'EDITING'
  credentialDirty.value = false
  editingCredential.value = false
}

function resetForm() {
  Object.assign(formModel, {
    id: undefined,
    platform: PLATFORMS.WILDBERRIES,
    credential: {},
    shopName: undefined,
    erpShopName: undefined,
    platformShopId: undefined,
    testToken: undefined,
    defaultWmsWarehouseId: undefined
  })

  state.value = 'IDLE'
  credentialDirty.value = false
  editingCredential.value = false
  originalMaskedCredential.value = {}
  testError.value = undefined
}

function handleCredentialInput() {
  invalidateTest()
}

function invalidateTest() {
  if (!requiresManualShopName.value) {
    formModel.shopName = undefined
  }

  formModel.platformShopId = undefined
  formModel.testToken = undefined
  testError.value = undefined
  credentialDirty.value = true

  if (state.value !== 'EDITING') {
    state.value = 'EDITING'
  }
}

function handlePlatformChange() {
  formModel.shopName = undefined
  formModel.platformShopId = undefined
  formModel.testToken = undefined
  formModel.credential = {}
  testError.value = undefined
  credentialDirty.value = true

  if (state.value !== 'EDITING') {
    state.value = 'EDITING'
  }
}

function toggleEditingCredential(on: boolean) {
  editingCredential.value = on

  if (on) {
    formModel.credential = {}
    invalidateTest()
    return
  }

  formModel.credential = { ...originalMaskedCredential.value }
  formModel.testToken = undefined
  credentialDirty.value = false
  testError.value = undefined

  if (state.value !== 'EDITING') {
    state.value = 'EDITING'
  }
}

async function testCred() {
  if (!canTest.value || testing.value) return

  state.value = 'TESTING'
  testError.value = undefined

  try {
    const resp = await testCredential({
      platform: formModel.platform,
      credential: { ...formModel.credential }
    })

    if (resp.code !== 200) {
      state.value = 'TEST_FAIL'
      testError.value = resp.message
      return
    }

    const data = resp.data as TestCredentialResponse
    if (hasText(data.shopName)) {
      formModel.shopName = data.shopName
    }

    formModel.platformShopId = data.platformShopId
    formModel.testToken = data.testToken
    state.value = 'TEST_SUCCESS'
    credentialDirty.value = true

    if (data.shopNameRequired) {
      message.success('测试成功，请填写店铺名称')
    } else {
      message.success('测试成功')
    }
  } catch (e: any) {
    state.value = 'TEST_FAIL'
    testError.value = e?.message || '测试失败'
  }
}

async function handleSave() {
  if (!canSave.value) return

  if (!hasText(formModel.erpShopName)) {
    message.error('请填写 ERP 店铺名称')
    return
  }

  if (requiresManualShopName.value && !hasText(formModel.shopName)) {
    message.error('请填写店铺名称')
    return
  }

  const platformShopId = displayPlatformShopId.value
  if (!hasText(platformShopId)) {
    message.error('缺少店铺ID，请重新测试凭证')
    return
  }

  if (credentialDirty.value && !hasText(formModel.testToken)) {
    message.error('请先测试凭证')
    return
  }

  saving.value = true

  const req: CreateOrUpdateShopRequest = {
    platform: formModel.platform,
    credential: credentialDirty.value ? { ...formModel.credential } : {},
    shopName: (formModel.shopName || '').trim(),
    erpShopName: formModel.erpShopName!.trim(),
    platformShopId,
    testToken: credentialDirty.value ? formModel.testToken : undefined,
    defaultWmsWarehouseId: formModel.defaultWmsWarehouseId!
  }

  try {
    const resp = formModel.id ? await updateShop(formModel.id, req) : await createShop(req)
    if (resp.code === 200) {
      message.success('保存成功')
      visible.value = false
      emits('submit-success')
    } else {
      message.error(resp.message || '保存失败')
    }
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleClose() {
  visible.value = false
}

async function loadWarehouseOptions() {
  const resp = await getWarehouseOptions()
  if (resp.code !== 200) return
  warehouseOptions.value = (resp.data || [])
    .filter(item => item.warehouseType === 'OWN')
    .map(item => ({ label: `${item.warehouseName} (${item.warehouseCode})`, value: item.id }))
}

defineExpose({ open })
</script>
