<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :centered="true"
    :body-style="{ padding: '24px 40px 8px 40px' }"
    :confirm-loading="submitLoading"
    :width="650"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="labelCol" :wrapper-col="wrapperCol">
      <!-- 原始的菜单ID 用于支持菜单ID 的修改功能-->
      <a-form-item v-if="isUpdateForm" style="display: none">
        <a-input v-model:value="formModel.originalId" />
      </a-form-item>

      <a-form-item :label="t('system.menu.parent')">
        <a-tree-select
          v-model:value="formModel.parentId"
          :placeholder="t('system.menu.parentHint')"
          :dropdown-style="{ maxHeight: '350px', overflow: 'auto' }"
          :tree-data="parentMenuTree"
          :tree-default-expanded-keys="[0]"
          :field-names="{ value: 'id' }"
        >
          <template #title="treeNode">
            <span> 【{{ treeNode.title }}】{{ treeNode.id }} </span>
          </template>
        </a-tree-select>
      </a-form-item>

      <a-form-item :label="t('system.menu.type')">
        <dict-radio-group v-model:value="formModel.type" class="menu-type" dict-code="menu_type" />
      </a-form-item>

      <a-row :gutter="16">
        <a-col :xs="24" :sm="24" :md="12">
          <a-form-item
            :label-col="rowLabelCol"
            :wrapper-col="rowWrapperCol"
            v-bind="validateInfos.id"
          >
            <template #label>
              {{ t('system.menu.id') }}
              <a-tooltip :title="t('system.menu.idHelp')">
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-input v-model:value="formModel.id" :placeholder="t('message.pleaseEnter')" />
          </a-form-item>
        </a-col>

        <a-col :xs="24" :sm="24" :md="12">
          <a-form-item
            :label="t('system.menu.displaySort')"
            :label-col="rowLabelCol"
            :wrapper-col="rowWrapperCol"
            v-bind="validateInfos.sort"
          >
            <a-input-number
              v-model:value="formModel.sort"
              :placeholder="t('system.menu.sortHint')"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item :label="t('system.menu.name')" v-bind="validateInfos.title">
        <a-input
          v-model:value="formModel.title"
          :placeholder="t('message.pleaseEnter')"
          style="width: 65%"
        />
        <!--        <a v-if="enableI18n && isCreateForm" style="margin-left: 8px" @click="toggleI18nAdvanced">-->
        <!--          {{ i18nAdvanced ? '收起' : '展开' }}国际化名称-->
        <!--          <a-icon :type="i18nAdvanced ? 'up' : 'down'" />-->
        <!--        </a>-->
      </a-form-item>

      <!-- 开启国际化 && 新建菜单 && 不是按钮时 -->
      <!--      <a-form-item v-show="i18nAdvanced" v-if="enableI18n && isCreateForm && menuType !== 2">-->
      <!--        <span slot="label">-->
      <!--          名称国际化-->
      <!--          <a-tooltip title="菜单标题将作为国际化信息的标识">-->
      <!--            <a-icon type="question-circle" />-->
      <!--          </a-tooltip>-->
      <!--        </span>-->
      <!--        <language-text ref="languageText" />-->
      <!--      </a-form-item>-->

      <template v-if="!isButton">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="24" :md="12">
            <a-form-item
              :label="t('system.menu.icon')"
              :label-col="rowLabelCol"
              :wrapper-col="rowWrapperCol"
            >
              <a-input v-model:value="formModel.icon" :placeholder="t('common.select')">
                <template #prefix>
                  <AntIcon v-if="formModel.icon" :type="formModel.icon" />
                </template>
                <template #addonAfter>
                  <SettingOutlined @click="showIconSelect" />
                </template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="24" :md="12">
            <a-form-item
              :label="t('system.menu.path')"
              :label-col="rowLabelCol"
              :wrapper-col="rowWrapperCol"
              v-bind="validateInfos.path"
            >
              <a-input v-model:value="formModel.path" :placeholder="t('message.pleaseEnter')" />
            </a-form-item>
          </a-col>
        </a-row>
      </template>

      <template v-if="isMenu">
        <a-row :gutter="16">
          <a-col :xs="24" :sm="24" :md="12">
            <a-form-item
              :label="t('system.menu.openMode')"
              :label-col="rowLabelCol"
              :wrapper-col="rowWrapperCol"
            >
              <a-select v-model:value="formModel.targetType">
                <a-select-option :value="1">{{ t('system.menu.internal') }}</a-select-option>
                <a-select-option :value="2">{{ t('system.menu.embedded') }}</a-select-option>
                <a-select-option :value="3">{{ t('system.menu.external') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="24" :md="12">
            <a-form-item
              :label="t('system.menu.cache')"
              :label-col="rowLabelCol"
              :wrapper-col="rowWrapperCol"
            >
              <a-radio-group v-model:value="formModel.keepAlive">
                <a-radio :value="1">{{ t('system.menu.enable') }}</a-radio>
                <a-radio :value="0">{{ t('system.menu.disable') }}</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item :label="t('system.menu.uri')" v-bind="validateInfos.uri">
          <a-input v-model:value="formModel.uri" :placeholder="t('message.pleaseEnter')" />
        </a-form-item>
      </template>

      <!-- 按钮没有显示隐藏一说 -->
      <template v-if="!isButton">
        <a-form-item :label="t('system.menu.visible')">
          <a-radio-group v-model:value="formModel.hidden">
            <a-radio :value="0">{{ t('system.menu.show') }}</a-radio>
            <a-radio :value="1">{{ t('system.menu.hide') }}</a-radio>
          </a-radio-group>
        </a-form-item>
      </template>

      <!-- 按钮才有授权标识 -->
      <template v-if="isButton">
        <a-form-item :label="t('system.menu.authorization')" v-bind="validateInfos.permission">
          <a-input v-model:value="formModel.permission" :placeholder="t('message.pleaseEnter')" />
        </a-form-item>
      </template>

      <a-form-item :label="t('system.menu.remarks')">
        <a-textarea
          v-model:value="formModel.remarks"
          :placeholder="t('system.menu.remarksHint')"
          :auto-size="{ minRows: 3, maxRows: 6 }"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <icon-selector-modal ref="iconSelectorModalRef" @choose="handleIconChoose"></icon-selector-modal>
</template>

<script setup lang="ts">
import { useModal } from '@/hooks/modal'
import { useAdminForm, useFormAction, FormAction } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import { createMenu, updateMenu } from '@/api/system/menu'
import AntIcon from '#/layout/components/AntIcon/index'
import type { ColProps } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { SysMenuType } from '@/api/system/menu/types'
import type { SysMenuVO, SysMenuDTO } from '@/api/system/menu/types'
import { listToTree } from '@/utils/tree-utils'
import { overrideProperties } from '@/utils/bean-utils'
import IconSelectorModal from '@/components/IconSelector/IconSelectorModal.vue'
import type { Icon } from '@/components/IconSelector/types'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps<{
  menuList: SysMenuVO[]
}>()

const emits = defineEmits<{
  (e: 'submit-success'): void
}>()

const labelCol: ColProps = { sm: { span: 24 }, md: { span: 4 } }
const wrapperCol: ColProps = { sm: { span: 24 }, md: { span: 20 } }
const rowLabelCol: ColProps = { sm: { span: 24 }, md: { span: 8 } }
const rowWrapperCol: ColProps = { sm: { span: 24 }, md: { span: 16 } }

type SysMenuTree = Partial<SysMenuVO> & { children?: SysMenuTree[] }

// 有父目录的菜单树
const parentMenuTree: SysMenuTree[] = [{ id: 0, title: t('system.menu.root') }]
watchEffect(() => {
  parentMenuTree[0].title = t('system.menu.root')
  parentMenuTree[0].children = props.menuList
    ? listToTree<SysMenuTree>(
        props.menuList.filter(x => x.type !== SysMenuType.BUTTON),
        0
      )
    : ([] as SysMenuTree[])
})

const { title, visible, openModal, closeModal } = useModal()

const { formAction, isUpdateForm } = useFormAction()

const iconSelectorModalRef = ref()

// 表单模型
const formModel = reactive<SysMenuDTO>({
  id: undefined,
  parentId: 0,
  title: '',
  icon: '',
  permission: '',
  path: '',
  targetType: 1,
  uri: '',
  sort: 1,
  keepAlive: 1,
  hidden: 0,
  type: SysMenuType.DIRECTORY,
  remarks: ''
})

// 是否是菜单类型
const isMenu = computed(() => formModel.type === SysMenuType.MENU)
// 是否是按钮类型
const isButton = computed(() => formModel.type === SysMenuType.BUTTON)

/* 菜单ID 的规则校验 */
const checkMenuId = async (_rule: Rule, value: number) => {
  const idStr = String(value ?? '')

  if (!idStr) {
    return Promise.reject(t('system.menu.idRequired'))
  }

  // 更新态：菜单ID 为既有值、通常并不修改，且历史/平台菜单（如 170500 海外仓作业、170700 客户管理、
  // 165000 库存管理、10028 个人页等）本就不符合 XX0000/XXXX00 六位约定（当初经 SQL 迁移直接入库）。
  // 若此处仍强校验格式，会导致"仅改显示排序/名称"也被这条规则拦下报错、无法保存。后端并不校验该格式，
  // 故更新时只要求 ID 非空、不再校验格式；新建态保留原约定引导。
  if (isUpdateForm.value) {
    return Promise.resolve()
  }

  if (idStr.length !== 6) {
    return Promise.reject(t('system.menu.idLength'))
  }

  if (formModel.type === SysMenuType.DIRECTORY && !idStr.endsWith('0000')) {
    return Promise.reject(t('system.menu.directoryId'))
  } else if (formModel.type === SysMenuType.MENU && !idStr.endsWith('00')) {
    return Promise.reject(t('system.menu.menuId'))
  }

  return Promise.resolve()
}
//菜单图标选择
const showIconSelect = () => {
  iconSelectorModalRef.value.show()
}

const handleIconChoose = (icon: Icon) => {
  formModel.icon = icon
}

// 表单的校验规则
const formRule = reactive({
  id: [{ required: true, validator: checkMenuId }],
  title: [{ required: true, message: t('system.menu.nameRequired') }],
  sort: [{ required: true, message: t('system.menu.sortRequired') }],
  path: [
    { required: !isButton.value, message: t('system.menu.pathRequired') },
    { pattern: /^[a-z0-9-]+$/, message: t('system.menu.pathPattern') }
  ],
  uri: [{ required: isMenu, message: t('system.menu.uriRequired') }],
  permission: [{ required: isButton, message: t('system.menu.permissionRequired') }]
})

// 表单的提交请求
const formRequestMapping: FormRequestMapping<SysMenuDTO> = {
  [FormAction.CREATE]: createMenu,
  [FormAction.UPDATE]: updateMenu
}

const { submitLoading, validateAndSubmit, resetFields, validateInfos } = useAdminForm(
  formAction,
  formRequestMapping,
  formModel,
  formRule
)

/* 表单提交处理 */
const handleSubmit = () => {
  validateAndSubmit(toRaw(formModel), {
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
  open(newFormAction: FormAction, record?: SysMenuVO) {
    openModal()
    resetFields()
    if (newFormAction === FormAction.CREATE) {
      title.value = t('system.menu.new')
      formModel.parentId = record?.id || 0
    } else {
      title.value = t('system.menu.editTitle')
      overrideProperties(formModel, record)
      formModel.originalId = record?.id
    }
    formAction.value = newFormAction
  }
})
</script>
