<template>
  <!-- 表格页面 -->
  <div v-show="!showFormPage">
    <!-- 查询表单 -->
    <supplier-page-search :loading="tableRef?.loading" @search="searchTable" />

    <pro-table
      ref="tableRef"
      :header-title="t('product.supplier.pageTitle')"
      row-key="id"
      :request="tableRequest"
      :columns="columns"
      :scroll="{ x: 1000 }"
    >
      <!-- 操作按钮区域 -->
      <template #toolBarRender>
        <new-button v-if="hasPermission('product:supplier:add')" @click="handleNew" />
      </template>

      <!-- 数据表格区域 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'supplierInfo'">
          <div class="supplier-info">
            <div class="supplier-name">{{ record.name }}</div>
            <div class="supplier-code">{{ record.supplierCode }}</div>
          </div>
        </template>
        <template v-else-if="column.key === 'contactInfo'">
          <div class="contact-info">
            <div v-if="record.businessContactName" class="contact-name">
              <user-outlined style="margin-right: 4px; color: #1890ff" />
              {{ record.businessContactName }}
            </div>
            <div v-if="record.businessContactPhone" class="contact-phone">
              <phone-outlined style="margin-right: 4px; color: #52c41a" />
              {{ record.businessContactPhone }}
            </div>
            <div v-if="record.businessContactEmail" class="contact-email">
              <mail-outlined style="margin-right: 4px; color: #faad14" />
              {{ record.businessContactEmail }}
            </div>
            <div
              v-if="
                !record.businessContactName &&
                !record.businessContactPhone &&
                !record.businessContactEmail
              "
              class="no-contact"
            >
              <span style="color: #d9d9d9">{{ t('product.supplier.noContact') }}</span>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'locationInfo'">
          <div class="location-info">
            <div v-if="record.city" class="city">
              <environment-outlined style="margin-right: 4px; color: #1890ff" />
              {{ record.city }}
            </div>
            <div v-if="record.address" class="address">{{ record.address }}</div>
            <div v-if="!record.city && !record.address" class="no-location">
              <span style="color: #d9d9d9">{{ t('product.supplier.noAddress') }}</span>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'status'">
          <dict-tag dict-code="enable_status" :value="record.status"></dict-tag>
        </template>
        <template v-else-if="column.key === 'operate'">
          <operation-group>
            <a v-if="hasPermission('product:supplier:edit')" @click="handleEdit(record)">{{
              t('action.edit')
            }}</a>
            <delete-text-button
              v-if="hasPermission('product:supplier:del')"
              @confirm="() => handleDelete(record)"
            />
          </operation-group>
        </template>
      </template>
    </pro-table>
  </div>

  <!-- 表单页面 -->
  <supplier-form-page
    v-show="showFormPage"
    ref="formPageRef"
    @submit-success="handleFormSubmitSuccess"
    @cancel="handleFormCancel"
  />
</template>

<script setup lang="ts">
import { nextTick } from 'vue'
import ProTable from '#/table'
import type { ProColumns } from '#/table'
import type { ProTableInstanceExpose, TableRequest } from '#/table'
import SupplierPageSearch from './SupplierPageSearch.vue'
import SupplierFormPage from './SupplierFormPage.vue'
import { NewButton, DeleteTextButton } from '@/components/Button'
import { OperationGroup } from '@/components/Operation'
import { useAuthorize } from '@/hooks/permission'
import { mergePageParam } from '@/utils/page-utils'
import { doRequest } from '@/utils/axios/request'
import { pageSupplier, deleteSupplier } from '@/api/product/supplier'
import type { SupplierPageVO, SupplierQO } from '@/api/product/supplier/types'
import { FormAction } from '@/hooks/form'
import { DictTag } from '@/components/Dict'
import {
  UserOutlined,
  PhoneOutlined,
  MailOutlined,
  EnvironmentOutlined
} from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'SupplierPage' })

// 鉴权方法
const { hasPermission } = useAuthorize()
const { t } = useI18n()

// 表格组件引用
const tableRef = ref<ProTableInstanceExpose>()
const formPageRef = ref<InstanceType<typeof SupplierFormPage>>()

// 页面状态控制
const showFormPage = ref(false)

/* 刷新表格 */
const reloadTable = (resetPageIndex?: boolean) => {
  tableRef.value?.actionRef?.reload(resetPageIndex)
}

// 查询参数
let searchParams: SupplierQO = {}

/* 远程加载表格数据 */
const tableRequest: TableRequest = (params, sorter, filter) => {
  const pageParam = mergePageParam(params, sorter, filter)
  return pageSupplier({ ...pageParam, ...searchParams })
}

/* 查询供应商 */
const searchTable = (params: SupplierQO) => {
  searchParams = params
  reloadTable(true) // 会调用 tableRequest
}

/* 新建供应商 */
const handleNew = () => {
  showFormPage.value = true
  nextTick(() => {
    formPageRef.value?.initForm(FormAction.CREATE)
  })
}

/* 编辑供应商 */
const handleEdit = (record: SupplierPageVO) => {
  showFormPage.value = true
  nextTick(() => {
    formPageRef.value?.initForm(FormAction.UPDATE, record)
  })
}

/* 表单提交成功处理 */
const handleFormSubmitSuccess = () => {
  showFormPage.value = false
  reloadTable()
}

/* 表单取消处理 */
const handleFormCancel = () => {
  showFormPage.value = false
}

/* 删除供应商 */
const handleDelete = (record: SupplierPageVO) => {
  doRequest(deleteSupplier(record.id), {
    successMessage: t('message.removeSuccess'),
    onSuccess: () => reloadTable()
  })
}

const columns = computed<ProColumns[]>(() => [
  {
    title: t('product.supplier.info'),
    key: 'supplierInfo',
    width: 200,
    fixed: 'left'
  },
  {
    title: t('product.supplier.businessContact'),
    key: 'contactInfo',
    width: 220
  },
  {
    title: t('product.supplier.addressInfo'),
    key: 'locationInfo',
    width: 200,
    ellipsis: true
  },
  {
    title: t('product.supplier.status'),
    dataIndex: 'status',
    key: 'status',
    width: 80
  },
  {
    title: t('common.remarks'),
    dataIndex: 'remarks',
    width: 150,
    ellipsis: true
  },
  {
    title: t('common.createTime'),
    dataIndex: 'createTime',
    width: 150,
    sorter: true
  },
  {
    key: 'operate',
    title: t('common.operation'),
    align: 'center',
    width: 100,
    fixed: 'right'
  }
])
</script>

<style scoped>
.supplier-info {
  padding: 4px 0;
}

.supplier-name {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
  margin-bottom: 2px;
}

.supplier-code {
  font-size: 12px;
  color: #8c8c8c;
  font-family: 'Consolas', 'Monaco', monospace;
}

.contact-info {
  padding: 4px 0;
  line-height: 1.4;
}

.contact-name,
.contact-phone,
.contact-email {
  display: flex;
  align-items: center;
  margin-bottom: 2px;
  font-size: 12px;
}

.contact-name {
  font-weight: 500;
  color: #262626;
}

.contact-phone {
  color: #595959;
}

.contact-email {
  color: #595959;
}

.no-contact,
.no-location {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 40px;
  font-size: 12px;
}

.location-info {
  padding: 4px 0;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.city {
  display: flex;
  align-items: center;
  font-weight: 500;
  color: #262626;
  margin-bottom: 4px;
  font-size: 13px;
  width: 100%;
}

.address {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.3;
  margin-left: 0;
  display: block;
  width: 100%;
  word-wrap: break-word;
}
</style>
