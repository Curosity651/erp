<template>
  <a-card :bordered="false" class="global-config-card">
    <template #title>
      <a-space>
        <setting-outlined />
        <span>全局配置</span>
      </a-space>
    </template>
    <template #extra>
      <a-button type="primary" ghost @click="handleEdit">编辑配置</a-button>
    </template>

    <!-- 默认值分组 -->
    <div class="config-section">
      <div class="section-label">默认值（可被 SKU 独立配置覆盖）</div>
      <a-row :gutter="24">
        <a-col :span="8">
          <a-statistic title="安全库存" :value="config?.safetyStock ?? '-'" suffix="件" />
        </a-col>
        <a-col :span="8">
          <a-statistic title="预警阈值" :value="config?.notifyThresholdDays ?? '-'">
            <template #prefix>≤</template>
            <template #suffix>天</template>
          </a-statistic>
        </a-col>
        <a-col :span="8">
          <a-statistic title="预警通知">
            <template #formatter>
              <a-badge
                :status="config?.notifyEnabled ? 'success' : 'default'"
                :text="config?.notifyEnabled ? '已开启' : '已关闭'"
              />
            </template>
          </a-statistic>
        </a-col>
      </a-row>
    </div>

    <!-- 通知人员分组 -->
    <div class="config-section">
      <div class="section-label">通知人员（全局生效）</div>
      <div class="user-tags">
        <template v-if="config?.notifyUserIds?.length">
          <a-tag v-for="uid in config.notifyUserIds" :key="uid">
            {{ getUserName(uid) }}
          </a-tag>
        </template>
        <span v-else class="text-secondary">未设置</span>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑全局配置"
      :width="520"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form ref="formRef" :model="formState" layout="vertical" class="edit-form">
        <!-- 库存标准分组 -->
        <div class="form-section">
          <div class="form-section-header">
            <span class="form-section-title">库存标准</span>
          </div>
          <div class="form-section-body">
            <a-form-item label="默认安全库存">
              <a-input-number
                v-model:value="formState.safetyStock"
                :min="0"
                style="width: 100%"
                addon-after="件"
              />
              <template #help>
                <span class="form-help-text">当 SKU 可售天数低于预警阈值时触发通知</span>
              </template>
            </a-form-item>
          </div>
        </div>

        <!-- 预警通知分组 -->
        <div class="form-section">
          <div class="form-section-header">
            <span class="form-section-title">预警通知</span>
            <a-switch
              v-model:checked="formState.notifyEnabled"
              checked-children="开启"
              un-checked-children="关闭"
            />
          </div>
          <div v-if="formState.notifyEnabled" class="form-section-body form-section-body--nested">
            <a-form-item label="预警阈值">
              <a-input-number
                v-model:value="formState.notifyThresholdDays"
                :min="1"
                style="width: 100%"
                addon-before="可售天数 ≤"
                addon-after="天"
              />
            </a-form-item>
            <a-form-item label="通知人员" class="mb-0">
              <UserSelect
                v-model:value="formState.notifyUserIds"
                mode="multiple"
                placeholder="请选择通知人员"
                :options="userOptions"
                :loading="usersLoading"
                style="width: 100%"
              />
            </a-form-item>
          </div>
          <div v-else class="form-section-disabled">
            <span>开启后可配置预警阈值和通知人员</span>
          </div>
        </div>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import { doRequest } from '@/utils/axios/request'
import { isSuccess } from '@/api'
import { getGlobalConfig, saveGlobalConfig } from '@/api/wms/inventory-forecast'
import { useUserData } from '@/hooks/use-user-data'
import UserSelect from '@/components/Lov/UserSelect.vue'
import type { GlobalInventoryConfigVO } from '@/api/wms/inventory-forecast/types'

defineOptions({ name: 'GlobalConfigSection' })

const config = ref<GlobalInventoryConfigVO>()
const editModalVisible = ref(false)
const saving = ref(false)

const formState = reactive({
  safetyStock: 200,
  notifyEnabled: true,
  notifyThresholdDays: 7,
  notifyUserIds: [] as number[]
})

// 用户数据
const { allUsers: userOptions, loading: usersLoading, loadAllUsers, getUserName } = useUserData()

async function loadConfig() {
  const result = await getGlobalConfig()
  if (isSuccess(result)) {
    config.value = result.data
  }
}

function handleEdit() {
  if (config.value) {
    Object.assign(formState, {
      safetyStock: config.value.safetyStock,
      notifyEnabled: config.value.notifyEnabled,
      notifyThresholdDays: config.value.notifyThresholdDays,
      notifyUserIds: [...(config.value.notifyUserIds || [])]
    })
  }
  editModalVisible.value = true
}

function handleSave() {
  saving.value = true
  doRequest(saveGlobalConfig(formState), {
    successMessage: '保存成功',
    onSuccess: () => {
      loadConfig()
      editModalVisible.value = false
    },
    onFinally: () => {
      saving.value = false
    }
  })
}

onMounted(() => {
  loadConfig()
  loadAllUsers()
})

defineExpose({ config, loadConfig })
</script>

<style scoped>
.global-config-card {
  :deep(.ant-card-body) {
    padding: 24px;
  }
}

.config-section {
  margin-bottom: 16px;
}

.config-section:last-child {
  margin-bottom: 0;
}

.section-label {
  padding-left: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--ant-color-text-secondary);
  border-left: 3px solid var(--ant-color-primary);
}

.user-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.text-secondary {
  color: var(--ant-color-text-secondary);
}

.edit-form {
  padding-top: 4px;

  :deep(.ant-form-item) {
    margin-bottom: 16px;
  }

  :deep(.ant-form-item-label) {
    padding-bottom: 4px;
  }
}

.form-section {
  margin-bottom: 12px;
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: var(--ant-border-radius-lg);
  overflow: hidden;
}

.form-section:last-child {
  margin-bottom: 0;
}

.form-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: var(--ant-color-fill-quaternary);
  border-bottom: 1px solid var(--ant-color-border-secondary);
}

.form-section-title {
  font-weight: 500;
  color: var(--ant-color-text);
}

.form-section-body {
  padding: 12px;

  :deep(.ant-form-item:last-child) {
    margin-bottom: 0;
  }
}

.form-section-body--nested {
  background: var(--ant-color-fill-quaternary);
}

.form-section-disabled {
  padding: 16px 12px;
  text-align: center;
  color: var(--ant-color-text-tertiary);
  font-size: 13px;
}

.form-help-text {
  color: var(--ant-color-text-tertiary);
  font-size: 12px;
}

.mb-0 {
  margin-bottom: 0;
}
</style>
