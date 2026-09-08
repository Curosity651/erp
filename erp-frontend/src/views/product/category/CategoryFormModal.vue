<template>
  <a-modal
    :title="title"
    :open="visible"
    :mask-closable="false"
    :body-style="{ paddingBottom: '8px' }"
    :confirm-loading="submitLoading"
    :width="450"
    @ok="handleSubmit"
    @cancel="handleClose"
  >
    <a-form :model="formModel" :label-col="labelCol" :wrapper-col="wrapperCol">
      <a-form-item v-if="isUpdateForm" style="display: none">
        <a-input v-model:value="formModel.id" />
      </a-form-item>
      <a-form-item :label="t('product.category.name')" v-bind="validateInfos.name">
        <a-input
          v-model:value="formModel.name"
          :placeholder="t('product.category.namePlaceholder')"
        />
      </a-form-item>
      <a-form-item :label="t('product.category.code')" v-bind="validateInfos.code">
        <a-input
          v-model:value="formModel.code"
          :placeholder="t('product.category.codePlaceholder')"
          :disabled="isUpdateForm"
        />
      </a-form-item>
      <a-form-item :label="t('product.category.parent')">
        <a-tree-select
          v-model:value="formModel.parentId"
          :tree-data="categoryTreeData"
          :field-names="{ label: 'name', value: 'id', children: 'children' }"
          :placeholder="t('product.category.parentPlaceholder')"
          allow-clear
          tree-default-expand-all
          style="width: 100%"
          @change="handleParentChange"
        >
          <template #title="{ name, code }">
            <span
              >{{ name }} <span style="color: #999">({{ code }})</span></span
            >
          </template>
        </a-tree-select>
        <div v-if="isUpdateForm" style="color: #999; font-size: 12px; margin-top: 4px">
          {{ t('product.category.parentTip') }}
        </div>
      </a-form-item>

      <a-form-item :label="t('product.category.sort')">
        <a-input-number
          v-model:value="formModel.sort"
          :placeholder="t('product.category.sortPlaceholder')"
          :min="0"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item :label="t('product.category.status')">
        <dict-radio-group v-model:value="formModel.status" dict-code="enable_status" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { useModal } from '@/hooks/modal'
import { FormAction, useAdminForm, useFormAction } from '@/hooks/form'
import type { FormRequestMapping } from '@/hooks/form'
import type { CategoryDTO, CategoryPageVO } from '@/api/product/category/types'
import { createCategory, updateCategory, listCategory } from '@/api/product/category'
import { overrideProperties } from '@/utils/bean-utils'
import type { ColProps } from 'ant-design-vue'
import { DictRadioGroup } from '@/components/Dict'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 扩展CategoryPageVO以支持树形结构
interface CategoryTreeVO extends CategoryPageVO {
  children?: CategoryTreeVO[]
}

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

// 品类树形数据
const categoryTreeData = ref<CategoryTreeVO[]>([])

// 当前编辑的品类ID（用于过滤树形数据）
const currentCategoryId = ref<number | undefined>()

// 表单模型
const formModel = reactive<Partial<CategoryDTO>>({
  // 品类ID
  id: undefined,
  // 品类名称
  name: undefined,
  // 品类编码，唯一
  code: undefined,
  // 上级品类ID，undefined表示顶级品类（提交时转换为0）
  parentId: undefined,
  // 品类层级（1级/2级/3级等）- 后端自动计算
  level: undefined,
  // 排序
  sort: 1,
  // 状态（1-启用，0-停用）
  status: 1
})

// 表单的校验规则
const formRule = computed(() => ({
  name: [
    { required: true, message: t('product.category.validation.name'), trigger: 'blur' },
    { max: 50, message: t('product.category.validation.nameLength'), trigger: 'blur' }
  ],
  code: [
    { required: true, message: t('product.category.validation.code'), trigger: 'blur' },
    { max: 30, message: t('product.category.validation.codeLength'), trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_-]+$/,
      message: t('product.category.validation.codePattern'),
      trigger: 'blur'
    }
  ]
}))

// 表单的提交请求
const formRequestMapping: FormRequestMapping<CategoryDTO> = {
  [FormAction.CREATE]: createCategory,
  [FormAction.UPDATE]: updateCategory
}

const { submitLoading, validateAndSubmit, resetFields, validateInfos } = useAdminForm(
  formAction,
  formRequestMapping,
  formModel,
  formRule
)

/* 表单提交处理 */
const handleSubmit = () => {
  const model = { ...formModel } as CategoryDTO
  // 将 undefined 转换为 0（顶级品类）
  if (model.parentId === undefined || model.parentId === null) {
    model.parentId = 0
  }
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

/* 加载品类树形数据 */
const loadCategoryTree = async () => {
  try {
    const response = await listCategory({})
    const allCategories = response.data || []

    // 构建树形结构，排除当前编辑的品类及其子孙品类
    const buildTree = (items: CategoryPageVO[], parentId = 0): CategoryTreeVO[] => {
      return items
        .filter(item => {
          // 排除当前编辑的品类
          if (currentCategoryId.value && item.id === currentCategoryId.value) {
            return false
          }
          // 排除当前品类的所有子孙品类
          if (currentCategoryId.value && isDescendantOf(item.id, currentCategoryId.value, items)) {
            return false
          }
          return (item.parentId || 0) === parentId
        })
        .sort((a, b) => (a.sort || 0) - (b.sort || 0))
        .map(item => ({
          ...item,
          children: buildTree(items, item.id)
        }))
    }

    categoryTreeData.value = buildTree(allCategories)
  } catch (error) {
    message.error(t('product.category.loadFailed'))
    console.error(t('product.category.loadFailed'), error)
  }
}

/* 检查某个品类是否是指定品类的子孙品类 */
const isDescendantOf = (
  categoryId: number,
  ancestorId: number,
  allCategories: CategoryPageVO[]
): boolean => {
  const category = allCategories.find(c => c.id === categoryId)
  if (!category || !category.parentId || category.parentId === 0) {
    return false
  }
  if (category.parentId === ancestorId) {
    return true
  }
  return isDescendantOf(category.parentId, ancestorId, allCategories)
}

/* 处理父品类变更 */
const handleParentChange = (value: number | undefined) => {
  // 保持 undefined 状态，在提交时再转换为 0
  // 这样可以让树形选择器正确显示为空（而不是显示空括号）
}

defineExpose({
  async open(newFormAction: FormAction, record?: CategoryPageVO, parentRecord?: CategoryPageVO) {
    openModal()
    resetFields()

    if (newFormAction === FormAction.CREATE) {
      currentCategoryId.value = undefined
      if (parentRecord) {
        title.value = t('product.category.addChildTitle', { name: parentRecord.name })
        formModel.parentId = parentRecord.id
      } else {
        title.value = t('product.category.createTitle')
        formModel.parentId = undefined // 使用 undefined 表示顶级品类
      }
    } else {
      title.value = t('product.category.editTitle')
      overrideProperties(formModel, record)
      currentCategoryId.value = record?.id
      // 将 parentId 为 0 的转换为 undefined，以便树形选择器正确显示
      if (formModel.parentId === 0) {
        formModel.parentId = undefined
      }
    }

    formAction.value = newFormAction

    // 加载品类树形数据
    await loadCategoryTree()
  }
})
</script>
