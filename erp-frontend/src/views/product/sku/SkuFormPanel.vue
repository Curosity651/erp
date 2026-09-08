<template>
  <div class="sku-form-panel">
    <!-- 顶部导航栏 -->
    <div class="form-header">
      <div class="header-content">
        <div class="header-left">
          <a-button type="text" class="back-btn" @click="handleCancel">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
            {{ t('product.sku.form.back') }}
          </a-button>
          <a-divider type="vertical" />
          <div class="title-section">
            <h1 class="page-title">{{ pageTitle }}</h1>
            <p v-if="pageSubTitle" class="page-subtitle">{{ pageSubTitle }}</p>
          </div>
        </div>
        <div v-if="mode !== 'view'" class="header-actions">
          <a-space size="middle">
            <a-button :disabled="submitLoading" @click="handleReset">
              <template #icon>
                <ReloadOutlined />
              </template>
              {{ t('action.reset') }}
            </a-button>
            <a-button type="primary" :loading="submitLoading" @click="handleSubmit">
              <template #icon>
                <CheckOutlined />
              </template>
              {{ isUpdateMode ? t('product.sku.form.update') : t('product.sku.form.create') }}
            </a-button>
          </a-space>
        </div>
      </div>
    </div>

    <!-- 表单内容区域 -->
    <div class="form-content">
      <div class="form-container">
        <!-- 简化版表单，暂时只包含基本字段 -->
        <a-form
          ref="formRef"
          :model="formData"
          :rules="validationRules"
          layout="vertical"
          :disabled="mode === 'view'"
          @finish="onFinish"
          @finish-failed="onFinishFailed"
        >
          <!-- 隐藏的ID字段 -->
          <a-form-item v-if="isUpdateMode" style="display: none">
            <a-input v-model:value="formData.id" />
          </a-form-item>

          <!-- 表单内容区域 -->
          <div class="form-sections">
            <!-- 基本信息卡片 -->
            <a-row>
              <a-col :span="24">
                <div class="form-section">
                  <div class="section-header">
                    <div class="section-icon">
                      <InfoCircleOutlined />
                    </div>
                    <div class="section-title">
                      <h3>{{ t('product.sku.form.basicInfo') }}</h3>
                      <p>{{ t('product.sku.form.basicInfoDescription') }}</p>
                    </div>
                  </div>
                  <div class="section-content">
                    <a-row :gutter="16">
                      <a-col :span="8">
                        <a-form-item
                          :label="t('product.sku.search.skuCode')"
                          name="skuCode"
                          :validate-status="getFieldStatus('skuCode')"
                          :help="getFieldError('skuCode')"
                        >
                          <a-input
                            v-model:value="formData.skuCode"
                            :placeholder="t('product.sku.form.skuCodePlaceholder')"
                            :disabled="(isUpdateMode && mode !== 'copy') || mode === 'view'"
                            size="large"
                            @blur="validateSkuCode"
                          >
                            <template #suffix>
                              <LoadingOutlined v-if="skuCodeValidating" />
                              <CheckCircleOutlined
                                v-else-if="
                                  !getFieldError('skuCode') &&
                                  formData.skuCode &&
                                  !(isUpdateMode && props.mode !== 'copy')
                                "
                                style="color: #52c41a"
                              />
                            </template>
                          </a-input>
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item
                          :label="t('product.sku.search.skuNo')"
                          name="skuNo"
                          :validate-status="getFieldStatus('skuNo')"
                          :help="getFieldError('skuNo')"
                        >
                          <a-input-number
                            v-model:value="formData.skuNo"
                            :placeholder="t('product.sku.search.skuNoPlaceholder')"
                            :min="1"
                            :max="999999"
                            style="width: 100%"
                            size="large"
                            @blur="validateSkuNo"
                          >
                            <template #suffix>
                              <LoadingOutlined v-if="skuNoValidating" />
                              <CheckCircleOutlined
                                v-else-if="
                                  !getFieldError('skuNo') && formData.skuNo && formData.skuNo > 0
                                "
                                style="color: #52c41a"
                              />
                            </template>
                          </a-input-number>
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item :label="t('product.sku.search.spuCode')" name="spuCode">
                          <a-input
                            v-model:value="formData.spuCode"
                            :placeholder="t('product.sku.form.spuCodePlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="16">
                      <a-col :span="24">
                        <a-form-item
                          :label="t('product.sku.form.barcodes')"
                          name="barcodes"
                          :extra="t('product.sku.form.barcodesExtra')"
                        >
                          <a-select
                            v-model:value="formData.barcodes"
                            mode="tags"
                            :max-tag-count="8"
                            :token-separators="[',', ' ', '\n']"
                            :placeholder="t('product.sku.form.barcodesPlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="16">
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.salesCountry')" name="salesCountry">
                          <country-select
                            v-model:value="formData.salesCountry"
                            :placeholder="t('product.sku.search.countryPlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.search.category')" name="categoryId">
                          <category-tree-select
                            v-model:value="formData.categoryId"
                            :placeholder="t('product.sku.search.categoryPlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="16">
                      <a-col :span="12">
                        <a-form-item
                          :label="t('product.sku.search.productStatus')"
                          name="productStatus"
                        >
                          <dict-select
                            v-model:value="formData.productStatus"
                            dict-code="product_status"
                            :placeholder="t('product.sku.search.statusPlaceholder')"
                            size="large"
                          >
                          </dict-select>
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.search.brand')" name="brandCode">
                          <brand-select
                            v-model:value="formData.brandCode"
                            :placeholder="t('product.sku.search.brandPlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-form-item
                      :label="t('product.sku.search.projectGroup')"
                      name="projectGroupCode"
                    >
                      <project-group-select
                        v-model:value="formData.projectGroupCode"
                        :placeholder="t('product.sku.search.projectGroupPlaceholder')"
                        size="large"
                      />
                    </a-form-item>

                    <!-- 产品名称信息 -->
                    <a-row :gutter="16">
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.form.chineseName')" name="chineseName">
                          <a-input
                            v-model:value="formData.chineseName"
                            :placeholder="t('product.sku.form.chineseNamePlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.form.russianName')" name="russianName">
                          <a-input
                            v-model:value="formData.russianName"
                            :placeholder="t('product.sku.form.russianNamePlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-form-item :label="t('product.sku.description')" name="description">
                      <a-textarea
                        v-model:value="formData.description"
                        :placeholder="t('product.sku.form.descriptionPlaceholder')"
                        :rows="4"
                        show-count
                        :maxlength="1000"
                        size="large"
                      />
                    </a-form-item>

                    <!-- 海关申报信息 -->
                    <a-row :gutter="16">
                      <a-col :span="12">
                        <a-form-item
                          :label="t('product.sku.form.customsName')"
                          name="customsDeclarationName"
                        >
                          <a-input
                            v-model:value="formData.customsDeclarationName"
                            :placeholder="t('product.sku.form.customsNamePlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          :label="t('product.sku.form.customsCode')"
                          name="customsDeclarationCode"
                        >
                          <a-input
                            v-model:value="formData.customsDeclarationCode"
                            :placeholder="t('product.sku.form.customsCodePlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <!-- 产品特性 -->
                    <a-form-item :label="t('product.sku.search.features')">
                      <a-space direction="vertical" size="middle">
                        <a-checkbox
                          :checked="!!formData.needsPower"
                          @change="(e: any) => (formData.needsPower = e.target.checked ? 1 : 0)"
                        >
                          <span style="margin-left: 8px">
                            <ThunderboltOutlined style="color: #faad14; margin-right: 4px" />
                            {{ t('product.sku.search.powerStrip') }}
                            <span style="color: #8c8c8c; font-size: 12px; margin-left: 8px">
                              ({{ t('product.sku.form.powerStripDescription') }})
                            </span>
                          </span>
                        </a-checkbox>
                        <a-checkbox
                          :checked="!!formData.seasonal"
                          @change="(e: any) => (formData.seasonal = e.target.checked ? 1 : 0)"
                        >
                          <span style="margin-left: 8px">
                            <CalendarOutlined style="color: #52c41a; margin-right: 4px" />
                            {{ t('product.sku.form.seasonalProduct') }}
                            <span style="color: #8c8c8c; font-size: 12px; margin-left: 8px">
                              ({{ t('product.sku.form.seasonalDescription') }})
                            </span>
                          </span>
                        </a-checkbox>
                        <a-checkbox
                          :checked="!!formData.hasRgbLight"
                          @change="(e: any) => (formData.hasRgbLight = e.target.checked ? 1 : 0)"
                        >
                          <span style="margin-left: 8px">
                            <span style="color: #722ed1; margin-right: 4px">🌈</span>
                            {{ t('product.sku.form.rgbLight') }}
                            <span style="color: #8c8c8c; font-size: 12px; margin-left: 8px">
                              ({{ t('product.sku.form.rgbDescription') }})
                            </span>
                          </span>
                        </a-checkbox>
                        <a-checkbox
                          :checked="!!formData.hasGlass"
                          @change="(e: any) => (formData.hasGlass = e.target.checked ? 1 : 0)"
                        >
                          <span style="margin-left: 8px">
                            <span style="color: #0958d9; margin-right: 4px">🔍</span>
                            {{ t('product.sku.featureGlass') }}
                            <span style="color: #8c8c8c; font-size: 12px; margin-left: 8px">
                              ({{ t('product.sku.form.glassDescription') }})
                            </span>
                          </span>
                        </a-checkbox>
                      </a-space>
                    </a-form-item>
                  </div>
                </div>
              </a-col>
            </a-row>

            <!-- 物流包装信息卡片 -->
            <a-row>
              <a-col :span="24">
                <div class="form-section">
                  <div class="section-header">
                    <div class="section-icon">
                      <CarOutlined />
                    </div>
                    <div class="section-title">
                      <h3>{{ t('product.sku.form.logisticsPackaging') }}</h3>
                      <p>{{ t('product.sku.form.logisticsPackagingDescription') }}</p>
                    </div>
                  </div>
                  <div class="section-content">
                    <a-row :gutter="16">
                      <a-col :span="8">
                        <a-form-item
                          :label="t('product.sku.search.shippingType')"
                          name="shippingType"
                        >
                          <dict-select
                            v-model:value="formData.shippingType"
                            dict-code="shipping_type"
                            :placeholder="t('product.sku.search.shippingTypePlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item :label="t('product.sku.packageType')" name="packageType">
                          <dict-select
                            v-model:value="formData.packageType"
                            dict-code="package_type"
                            :placeholder="t('product.sku.search.packageTypePlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item
                          :label="t('product.sku.search.billingWeightType')"
                          name="billingWeightType"
                        >
                          <dict-select
                            v-model:value="formData.billingWeightType"
                            dict-code="billing_weight_type"
                            :placeholder="t('product.sku.search.billingWeightPlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <!-- 物理属性 -->
                    <a-row :gutter="16">
                      <a-col :span="8">
                        <a-form-item :label="t('product.sku.surfaceColor')" name="surfaceColor">
                          <a-input
                            v-model:value="formData.surfaceColor"
                            :placeholder="t('product.sku.form.surfaceColorPlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item :label="t('product.sku.search.frameColor')" name="frameColor">
                          <a-input
                            v-model:value="formData.frameColor"
                            :placeholder="t('product.sku.form.frameColorPlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item :label="t('product.sku.search.material')" name="material">
                          <a-input
                            v-model:value="formData.material"
                            :placeholder="t('product.sku.form.materialPlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item
                          :label="t('product.sku.form.bcBoxStrength')"
                          name="bcBoxMinBreakage"
                        >
                          <a-input
                            v-model:value="formData.bcBoxMinBreakage"
                            :placeholder="t('product.sku.form.bcBoxStrengthPlaceholder')"
                            size="large"
                          >
                            <template #suffix>
                              <span style="color: #8c8c8c">kPa</span>
                            </template>
                          </a-input>
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item :label="t('product.sku.form.splitBoxInfo')" name="packaging">
                          <a-input
                            v-model:value="formData.packaging"
                            :placeholder="t('product.sku.form.splitBoxPlaceholder')"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <!-- 外箱尺寸与单箱毛重 -->
                    <div class="dimensions-section">
                      <h4 class="subsection-title">
                        <BoxPlotOutlined style="margin-right: 8px; color: #1890ff" />
                        {{ t('product.sku.form.outerBoxDimensions') }}
                      </h4>

                      <a-row :gutter="16">
                        <a-col :xs="24" :sm="12" :lg="6">
                          <a-form-item
                            :label="t('product.sku.form.outerLength')"
                            name="outerLengthMm"
                            required
                          >
                            <a-input-number
                              v-model:value="outerLengthCm"
                              :placeholder="t('product.sku.form.lengthPlaceholder')"
                              :min="0.1"
                              :precision="1"
                              style="width: 100%"
                              size="large"
                            >
                              <template #addonAfter>cm</template>
                            </a-input-number>
                          </a-form-item>
                        </a-col>
                        <a-col :xs="24" :sm="12" :lg="6">
                          <a-form-item
                            :label="t('product.sku.form.outerWidth')"
                            name="outerWidthMm"
                            required
                          >
                            <a-input-number
                              v-model:value="outerWidthCm"
                              :placeholder="t('product.sku.form.widthPlaceholder')"
                              :min="0.1"
                              :precision="1"
                              style="width: 100%"
                              size="large"
                            >
                              <template #addonAfter>cm</template>
                            </a-input-number>
                          </a-form-item>
                        </a-col>
                        <a-col :xs="24" :sm="12" :lg="6">
                          <a-form-item
                            :label="t('product.sku.form.outerHeight')"
                            name="outerHeightMm"
                            required
                          >
                            <a-input-number
                              v-model:value="outerHeightCm"
                              :placeholder="t('product.sku.form.heightPlaceholder')"
                              :min="0.1"
                              :precision="1"
                              style="width: 100%"
                              size="large"
                            >
                              <template #addonAfter>cm</template>
                            </a-input-number>
                          </a-form-item>
                        </a-col>
                        <a-col :xs="24" :sm="12" :lg="6">
                          <a-form-item
                            :label="t('product.sku.form.grossWeight')"
                            name="outerGrossWeightG"
                            required
                          >
                            <a-input-number
                              v-model:value="outerGrossWeightKg"
                              :placeholder="t('product.sku.form.grossWeightPlaceholder')"
                              :min="0.001"
                              :precision="3"
                              style="width: 100%"
                              size="large"
                            >
                              <template #addonAfter>kg</template>
                            </a-input-number>
                          </a-form-item>
                        </a-col>
                      </a-row>
                    </div>

                    <!-- 自动计算结果显示 -->
                    <div class="calculated-results">
                      <h4 class="subsection-title">
                        <CalculatorOutlined style="margin-right: 8px; color: #52c41a" />
                        {{ t('product.sku.form.calculatedResults') }}
                        <a-tooltip :title="t('product.sku.form.calculatedResultsTip')">
                          <InfoCircleOutlined
                            style="margin-left: 8px; color: #8c8c8c; font-size: 14px"
                          />
                        </a-tooltip>
                      </h4>

                      <a-row :gutter="16">
                        <a-col :xs="24" :sm="12" :md="6">
                          <a-form-item :label="t('product.sku.form.packageVolume')">
                            <a-input
                              :value="getFormattedValue('packageVolume')"
                              disabled
                              size="large"
                              class="calculated-input volume"
                            >
                              <template #prefix>
                                <BoxPlotOutlined style="color: #1890ff" />
                              </template>
                            </a-input>
                          </a-form-item>
                        </a-col>
                        <a-col :xs="24" :sm="12" :md="6">
                          <a-form-item :label="t('product.sku.form.densityKg')">
                            <a-input
                              :value="getFormattedValue('densityKgM3')"
                              disabled
                              size="large"
                              class="calculated-input density-kg"
                            >
                              <template #prefix>
                                <DashboardOutlined style="color: #52c41a" />
                              </template>
                            </a-input>
                          </a-form-item>
                        </a-col>
                        <a-col :xs="24" :sm="12" :md="6">
                          <a-form-item :label="t('product.sku.form.densityG')">
                            <a-input
                              :value="getFormattedValue('densityGCm3')"
                              disabled
                              size="large"
                              class="calculated-input density-g"
                            >
                              <template #prefix>
                                <DashboardOutlined style="color: #722ed1" />
                              </template>
                            </a-input>
                          </a-form-item>
                        </a-col>
                        <a-col :xs="24" :sm="12" :md="6">
                          <a-form-item :label="t('product.sku.containerCapacity')">
                            <a-input
                              :value="getFormattedValue('containerCapacity')"
                              disabled
                              size="large"
                              class="calculated-input capacity"
                            >
                              <template #prefix>
                                <ContainerOutlined style="color: #faad14" />
                              </template>
                              <template #suffix>
                                <span style="color: #8c8c8c">{{
                                  t('product.sku.form.unitPiece')
                                }}</span>
                              </template>
                            </a-input>
                          </a-form-item>
                        </a-col>
                      </a-row>
                    </div>
                  </div>
                </div>
              </a-col>
            </a-row>

            <!-- 产品信息卡片 -->
            <a-row>
              <a-col :span="24">
                <div class="form-section">
                  <div class="section-header">
                    <div class="section-icon">
                      <AppstoreOutlined />
                    </div>
                    <div class="section-title">
                      <h3>{{ t('product.sku.form.productSpecs') }}</h3>
                      <p>{{ t('product.sku.form.productSpecsDescription') }}</p>
                    </div>
                  </div>
                  <div class="section-content">
                    <a-form-item
                      :label="t('product.sku.functionalRequirements')"
                      name="functionalRequirements"
                    >
                      <a-textarea
                        v-model:value="formData.functionalRequirements"
                        :placeholder="t('product.sku.form.functionalRequirementsPlaceholder')"
                        :rows="3"
                        size="large"
                      />
                    </a-form-item>
                  </div>
                </div>
              </a-col>
            </a-row>

            <!-- 采购信息卡片 -->
            <a-row>
              <a-col :span="24">
                <div class="form-section">
                  <div class="section-header">
                    <div class="section-icon">
                      <DollarOutlined />
                    </div>
                    <div class="section-title">
                      <h3>{{ t('product.sku.purchaseInfo') }}</h3>
                      <p>{{ t('product.sku.form.purchaseDescription') }}</p>
                    </div>
                  </div>
                  <div class="section-content">
                    <!-- 供应商信息 -->
                    <a-row :gutter="16">
                      <a-col :span="12">
                        <a-form-item
                          :label="t('product.sku.form.supplierName')"
                          name="supplierCode"
                        >
                          <supplier-select
                            v-model:value="formData.supplierCode"
                            size="large"
                            @change="handleSupplierChange"
                            @select="handleSupplierSelect"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <!-- 展示型只读框，不参与表单校验，故不设 name，避免与上方 supplierCode 重复注册 -->
                        <a-form-item :label="t('product.sku.form.supplierCode')">
                          <a-input
                            v-model:value="formData.supplierCode"
                            :placeholder="t('product.sku.form.supplierCodePlaceholder')"
                            size="large"
                            disabled
                            style="background-color: #f5f5f5"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <!-- 价格信息 -->
                    <a-row :gutter="16">
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.form.priceType')" name="includeTax">
                          <a-button-group size="large" style="width: 100%">
                            <a-button
                              :type="!formData.includeTax ? 'primary' : 'default'"
                              style="width: 50%; height: 40px"
                              @click="
                                () => {
                                  formData.includeTax = 0
                                  handleTaxChange()
                                }
                              "
                            >
                              <span
                                style="
                                  display: flex;
                                  align-items: center;
                                  justify-content: center;
                                  gap: 8px;
                                "
                              >
                                <span style="font-size: 16px">💰</span>
                                {{ t('product.sku.form.priceExcludingTax') }}
                              </span>
                            </a-button>
                            <a-button
                              :type="formData.includeTax ? 'primary' : 'default'"
                              style="width: 50%; height: 40px"
                              @click="
                                () => {
                                  formData.includeTax = 1
                                  handleTaxChange()
                                }
                              "
                            >
                              <span
                                style="
                                  display: flex;
                                  align-items: center;
                                  justify-content: center;
                                  gap: 8px;
                                "
                              >
                                <span style="font-size: 16px">🧾</span>
                                {{ t('product.sku.form.priceIncludingTax') }}
                              </span>
                            </a-button>
                          </a-button-group>
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item
                          v-if="formData.includeTax"
                          :label="t('product.sku.taxRate')"
                          name="taxRate"
                        >
                          <a-input-number
                            v-model:value="formData.taxRate"
                            :placeholder="t('product.sku.form.taxRatePlaceholder')"
                            :precision="2"
                            style="width: 100%"
                            size="large"
                          >
                            <template #addonAfter>%</template>
                          </a-input-number>
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <!-- 采购价格 -->
                    <a-row :gutter="16">
                      <a-col :span="24">
                        <a-form-item
                          :label="
                            formData.includeTax
                              ? t('product.sku.form.purchasePriceIncludingTax')
                              : t('product.sku.form.purchasePriceExcludingTax')
                          "
                          name="purchasePrice"
                        >
                          <a-input-number
                            v-model:value="formData.purchasePrice"
                            :placeholder="
                              formData.includeTax
                                ? t('product.sku.form.purchasePriceIncludingTaxPlaceholder')
                                : t('product.sku.form.purchasePriceExcludingTaxPlaceholder')
                            "
                            :min="0"
                            :precision="2"
                            style="width: 100%"
                            addon-before="¥"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <!-- 订购信息 -->
                    <a-row :gutter="16">
                      <a-col :span="8">
                        <a-form-item
                          :label="t('product.sku.minimumOrder')"
                          name="minimumOrderQuantity"
                        >
                          <a-input-number
                            v-model:value="formData.minimumOrderQuantity"
                            :placeholder="t('product.sku.form.minimumOrderPlaceholder')"
                            :min="1"
                            style="width: 100%"
                            size="large"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item
                          :label="t('product.sku.productionCycle')"
                          name="productionCycle"
                        >
                          <a-input-number
                            v-model:value="formData.productionCycle"
                            :placeholder="t('product.sku.form.productionCyclePlaceholder')"
                            :min="1"
                            style="width: 100%"
                            size="large"
                          >
                            <template #addonAfter>{{ t('product.sku.form.unitDay') }}</template>
                          </a-input-number>
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </div>
                </div>
              </a-col>
            </a-row>

            <!-- 人员分配卡片 -->
            <a-row>
              <a-col :span="24">
                <div class="form-section">
                  <div class="section-header">
                    <div class="section-icon">
                      <TeamOutlined />
                    </div>
                    <div class="section-title">
                      <h3>{{ t('product.sku.form.staffAssignment') }}</h3>
                      <p>{{ t('product.sku.form.staffAssignmentDescription') }}</p>
                    </div>
                  </div>
                  <div class="section-content">
                    <a-row :gutter="16">
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.roleDeveloper')" name="developerId">
                          <user-select
                            v-model:value="formData.developerId"
                            :placeholder="t('product.sku.search.developerPlaceholder')"
                            size="large"
                            :loading="userDataLoading"
                            :options="allUsers"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.roleOperator')" name="operatorId">
                          <user-select
                            v-model:value="formData.operatorId"
                            :placeholder="t('product.sku.search.operatorPlaceholder')"
                            size="large"
                            :loading="userDataLoading"
                            :options="allUsers"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>

                    <a-row :gutter="16">
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.roleQc')" name="qcId">
                          <user-select
                            v-model:value="formData.qcId"
                            :placeholder="t('product.sku.search.qcPlaceholder')"
                            size="large"
                            :loading="userDataLoading"
                            :options="allUsers"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="12">
                        <a-form-item :label="t('product.sku.rolePurchaser')" name="purchaserId">
                          <user-select
                            v-model:value="formData.purchaserId"
                            :placeholder="t('product.sku.search.purchaserPlaceholder')"
                            size="large"
                            :loading="userDataLoading"
                            :options="allUsers"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </div>
                </div>
              </a-col>
            </a-row>

            <!-- 产品图片卡片 -->
            <a-row>
              <a-col :span="24">
                <div class="form-section">
                  <div class="section-header">
                    <div class="section-icon">
                      <PictureOutlined />
                    </div>
                    <div class="section-title">
                      <h3>{{ t('product.sku.productImage') }}</h3>
                      <p>{{ t('product.sku.form.imagesDescription') }}</p>
                    </div>
                  </div>
                  <div class="section-content">
                    <!-- 图片验证状态提示 -->
                    <div v-if="props.mode !== 'view'" class="image-validation-status">
                      <a-alert
                        :type="imageValidationStatus.isValid ? 'success' : 'warning'"
                        :show-icon="true"
                        :message="
                          imageValidationStatus.isValid
                            ? t('product.sku.form.imagesUploaded', {
                                actual: imageValidationStatus.actualCount,
                                platform: imageValidationStatus.platformCount
                              })
                            : t('product.sku.form.imageRequired')
                        "
                        style="margin-bottom: 16px"
                      />
                    </div>

                    <!-- 使用专门的图片管理组件 -->
                    <SkuImageManager
                      :sku-id="props.skuId"
                      :readonly="props.mode === 'view'"
                      :files="imageFilesForDisplay"
                      @files-change="handleImageFilesChange"
                    />
                  </div>
                </div>
              </a-col>
            </a-row>

            <!-- 工程文件卡片 -->
            <a-row>
              <a-col :span="24">
                <div class="form-section">
                  <div class="section-header">
                    <div class="section-icon">
                      <FolderOpenOutlined />
                    </div>
                    <div class="section-title">
                      <h3>{{ t('product.sku.form.engineeringFiles') }}</h3>
                      <p>{{ t('product.sku.form.engineeringFilesDescription') }}</p>
                    </div>
                  </div>
                  <div class="section-content">
                    <!-- 工程文件管理组件 -->
                    <engineering-file-manager-optimized
                      ref="engineeringFileManagerRef"
                      :sku-id="props.skuId"
                      :files="engineeringFilesForDisplay"
                      :readonly="mode === 'view'"
                      @files-change="handleEngineeringFilesChange"
                    />
                  </div>
                </div>
              </a-col>
            </a-row>
          </div>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useSkuForm } from '@/hooks/use-sku-form'
import { useSkuApi } from '@/hooks/use-sku-api'
import { useSkuCalculator } from '@/hooks/use-sku-calculator'
import { useUserData } from '@/hooks/use-user-data'
import { isSuccess } from '@/api'
import CategoryTreeSelect from '@/components/Lov/CategoryTreeSelect.vue'
import SupplierSelect from '@/components/Lov/SupplierSelect.vue'
import BrandSelect from '@/components/Lov/BrandSelect.vue'
import ProjectGroupSelect from '@/components/Lov/ProjectGroupSelect.vue'
import CountrySelect from '@/components/Lov/CountrySelect.vue'
import UserSelect from '@/components/Lov/UserSelect.vue'
import DictSelect from '@/components/Dict/group/DictSelect.vue'
import SkuImageManager from '@/views/product/sku/components/SkuImageManager.vue'
import type { SkuDetailVO, SkuFileDTO, SkuFileVO } from '@/api/product/sku/types'
import {
  AppstoreOutlined,
  ArrowLeftOutlined,
  BoxPlotOutlined,
  CalculatorOutlined,
  CalendarOutlined,
  CarOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  ContainerOutlined,
  DashboardOutlined,
  DollarOutlined,
  FolderOpenOutlined,
  InfoCircleOutlined,
  LoadingOutlined,
  PictureOutlined,
  ReloadOutlined,
  TeamOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import EngineeringFileManagerOptimized from '@/views/product/sku/components/SkuEngineeringFileManager.vue'
import { useI18n } from 'vue-i18n'

// 组件属性定义
interface Props {
  mode: 'create' | 'edit' | 'view' | 'copy'
  skuId?: number
  initialData?: any
}

// 组件事件定义
interface Emits {
  (e: 'cancel'): void

  (e: 'submit-success'): void
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'create'
})

const emit = defineEmits<Emits>()
const { t } = useI18n()

defineOptions({ name: 'SkuFormPanel' })

// 使用表单状态管理
const {
  formState,
  formData,
  isUpdateMode,
  hasUnsavedChanges,
  validationRules,
  initForm,
  resetForm,
  validateForm,
  getSubmitData,
  setFieldError,
  clearFieldError
} = useSkuForm()

// 使用SKU API
const { createSku, updateSku, getSkuDetail, validateSkuCode: apiValidateSkuCode } = useSkuApi()

// 使用SKU计算器
const skuCalculator = useSkuCalculator()
const { validationErrors: calculatorErrors, setDimensions } = skuCalculator

// 使用用户数据
const {
  allUsers,
  loading: userDataLoading,
  isInitialized: userDataInitialized,
  loadAllUsers
} = useUserData()

// 单独获取格式化值以保持响应性
const formattedValues = skuCalculator.formattedValues

const dimensionModel = (field: 'outerLengthMm' | 'outerWidthMm' | 'outerHeightMm') =>
  computed<number | undefined>({
    get: () => (formData[field] ? formData[field]! / 10 : undefined),
    set: value => {
      formData[field] = value && value > 0 ? Math.round(value * 10) : undefined
      if (field === 'outerLengthMm') formData.packageLength = value
      if (field === 'outerWidthMm') formData.packageWidth = value
      if (field === 'outerHeightMm') formData.packageHeight = value
      formData.packageUnit = 'CM'
      updateCalculatorDimensions()
    }
  })

const outerLengthCm = dimensionModel('outerLengthMm')
const outerWidthCm = dimensionModel('outerWidthMm')
const outerHeightCm = dimensionModel('outerHeightMm')
const outerGrossWeightKg = computed<number | undefined>({
  get: () => (formData.outerGrossWeightG ? formData.outerGrossWeightG / 1000 : undefined),
  set: value => {
    formData.outerGrossWeightG = value && value > 0 ? Math.round(value * 1000) : undefined
    formData.weight = value
    formData.weightUnit = 'KG'
    updateCalculatorDimensions()
  }
})

// 页面状态
const submitLoading = ref(false)
const initializing = ref(false)

// 文件上传相关 - 现在使用 OSS 直传，不再需要 fileUploader

// 工程文件管理器引用
const engineeringFileManagerRef = ref()

// 🎯 重构后：分离但统一的文件管理

// 1. 图片文件显示数据 - 只提取图片相关文件
const imageFilesForDisplay = computed(() => {
  if (!formData.files) return {}

  const imageFiles: Record<string, SkuFileVO[]> = {}

  // 只提取图片相关的文件类型
  const imageFileTypes = ['actual_image', 'platform_image']
  imageFileTypes.forEach(fileType => {
    if (formData.files![fileType]) {
      imageFiles[fileType] = formData.files![fileType]
    }
  })

  return imageFiles
})

// 文件类型定义
const FILE_TYPES = {
  ACTUAL_IMAGE: 'actual_image',
  PLATFORM_IMAGE: 'platform_image',
  ENGINEERING: [
    'manual',
    'box_mark',
    'install_video',
    'quality_report',
    'platform_render_video',
    'model_3d',
    'engineering_drawing',
    'bom',
    'drop_test_video',
    'package_manual',
    'quotation',
    'sample_confirmation'
  ]
} as const

// 🎯 重构后：统一的文件变化处理函数
const updateFormDataFiles = (fileType: string, fileList: SkuFileDTO[]) => {
  // 确保 formData.files 存在
  if (!formData.files) {
    formData.files = {}
  }

  // 更新指定类型的文件
  if (fileList && fileList.length > 0) {
    // 类型转换：SkuFileDTO[] -> SkuFileVO[]
    formData.files[fileType] = fileList as SkuFileVO[]
  } else {
    // 如果文件列表为空，删除该类型
    delete formData.files[fileType]
  }
}

// 处理图片文件变化 - 传入参数是最终的图片状态
const handleImageFilesChange = (files: Record<string, SkuFileDTO[]>) => {
  console.log('图片文件发生变化:', files)
  console.log('当前 formData.files 状态:', formData.files)

  // 定义所有图片文件类型
  const imageFileTypes = ['actual_image', 'platform_image']

  // 使用统一的文件更新函数处理所有图片类型
  imageFileTypes.forEach(fileType => {
    const fileList = files[fileType] || []
    updateFormDataFiles(fileType, fileList)
  })

  // 实时验证图片是否满足要求（至少有一类图片）
  const actualImages = formData.files?.actual_image || []
  const platformImages = formData.files?.platform_image || []

  console.log('图片状态更新结果:', {
    actualCount: actualImages.length,
    platformCount: platformImages.length,
    isValid: actualImages.length > 0 || platformImages.length > 0
  })

  console.log('更新后的 formData.files:', formData.files)
}

// 工程文件显示数据 - 从 formData.files 中提取工程文件部分
const engineeringFilesForDisplay = computed(() => {
  if (!formData.files) return undefined

  const engineeringFiles: Record<string, SkuFileVO[]> = {}

  Object.entries(formData.files).forEach(([fileType, files]) => {
    if (FILE_TYPES.ENGINEERING.includes(fileType as any) && Array.isArray(files)) {
      engineeringFiles[fileType] = files
    }
  })

  return Object.keys(engineeringFiles).length > 0 ? engineeringFiles : undefined
})

// 处理工程文件变化
const handleEngineeringFilesChange = (engineeringFiles: Record<string, SkuFileDTO[]>) => {
  console.log('工程文件发生变化:', engineeringFiles)

  // 确保 formData.files 存在
  if (!formData.files) {
    formData.files = {}
  }

  // 先清除所有工程文件类型
  FILE_TYPES.ENGINEERING.forEach(fileType => {
    delete formData.files![fileType]
  })

  // 添加新的工程文件
  Object.entries(engineeringFiles).forEach(([fileType, files]) => {
    if (files.length > 0) {
      // 类型转换：SkuFileDTO[] -> SkuFileVO[]
      formData.files![fileType] = files as SkuFileVO[]
    }
  })
}

// 收集所有文件数据，直接从 formData.files 获取
const collectFilesData = (): Record<string, SkuFileDTO[]> => {
  // 直接返回 formData.files，因为所有组件都在维护这个字段
  return formData.files || {}
}

// 页面标题
const pageTitle = computed(() => {
  switch (props.mode) {
    case 'edit':
      return t('product.sku.form.editTitle')
    case 'view':
      return t('product.sku.form.viewTitle')
    case 'copy':
      return t('product.sku.form.copyTitle')
    case 'create':
    default:
      return t('product.sku.form.createTitle')
  }
})

const pageSubTitle = computed(() => {
  if (isUpdateMode.value && formData.skuCode) {
    return t('product.sku.form.codeSubtitle', { sku: formData.skuCode })
  }
  return t('product.sku.form.subtitle')
})

// 监听表单数据变化，更新计算器
watch(
  () => [
    formData.packageLength,
    formData.packageWidth,
    formData.packageHeight,
    formData.weight,
    formData.packageUnit,
    formData.weightUnit
  ],
  () => {
    updateCalculatorDimensions()
  },
  { deep: true }
)

// 处理税收选择变化
const handleTaxChange = () => {
  if (!formData.includeTax) {
    // 不含税模式：清除税率
    formData.taxRate = undefined
  }
}

// 供应商选择处理
const handleSupplierSelect = (supplier: any) => {
  // 自动填充供应商名称
  formData.supplierName = supplier.name
  console.log('选择供应商:', supplier)
}

// 供应商变化处理
const handleSupplierChange = (value?: string, supplier?: any) => {
  if (value && supplier) {
    // 选择了供应商，自动填充名称
    formData.supplierName = supplier.name
  } else {
    // 清空了供应商，清空名称
    formData.supplierName = ''
  }
}

// 获取格式化的计算结果值
const getFormattedValue = (key: keyof typeof formattedValues) => {
  try {
    if (!formattedValues || !formattedValues[key]) {
      return ''
    }
    // formattedValues 中的每个属性都是 computed，需要访问 .value
    return formattedValues[key].value || ''
  } catch (error) {
    console.warn('获取格式化值失败:', key, error)
    return ''
  }
}

// 更新计算器尺寸数据
const updateCalculatorDimensions = () => {
  // 转换单位到米和千克，并将字符串转换为数字
  let lengthInM: number | undefined = formData.packageLength
    ? Number(formData.packageLength)
    : undefined
  let widthInM: number | undefined = formData.packageWidth
    ? Number(formData.packageWidth)
    : undefined
  let heightInM: number | undefined = formData.packageHeight
    ? Number(formData.packageHeight)
    : undefined
  let weightInKg: number | undefined = formData.weight ? Number(formData.weight) : undefined

  // 检查数字转换是否有效
  if (lengthInM !== undefined && (isNaN(lengthInM) || lengthInM <= 0)) lengthInM = undefined
  if (widthInM !== undefined && (isNaN(widthInM) || widthInM <= 0)) widthInM = undefined
  if (heightInM !== undefined && (isNaN(heightInM) || heightInM <= 0)) heightInM = undefined
  if (weightInKg !== undefined && (isNaN(weightInKg) || weightInKg <= 0)) weightInKg = undefined

  // 尺寸单位转换
  if (formData.packageUnit === 'CM' || formData.packageUnit === 'cm') {
    lengthInM = lengthInM ? lengthInM / 100 : undefined
    widthInM = widthInM ? widthInM / 100 : undefined
    heightInM = heightInM ? heightInM / 100 : undefined
  } else if (formData.packageUnit === 'MM' || formData.packageUnit === 'mm') {
    lengthInM = lengthInM ? lengthInM / 1000 : undefined
    widthInM = widthInM ? widthInM / 1000 : undefined
    heightInM = heightInM ? heightInM / 1000 : undefined
  }

  // 重量单位转换
  if (formData.weightUnit === 'g') {
    weightInKg = weightInKg ? weightInKg / 1000 : undefined
  }

  const dimensions = {
    packageLength: lengthInM,
    packageWidth: widthInM,
    packageHeight: heightInM,
    weight: weightInKg
  }

  setDimensions(dimensions)
}

// 从 SkuDetailVO.files 数据中加载文件列表
const loadFileListFromData = (skuData: SkuDetailVO) => {
  console.log('[loadFileListFromData] 从 SKU 数据中加载文件列表:', skuData.files)

  if (!skuData.files) {
    // 确保 formData.files 初始化
    formData.files = {}
    return
  }

  // 将 files 数据设置到 formData 中，这是所有文件组件的数据源
  // 由于图片文件列表现在是计算属性，会自动从 formData.files 中获取数据
  formData.files = skuData.files
}

// 加载文件列表
const loadFileList = async () => {
  // 如果是编辑或查看模式，文件列表会在 initForm 时通过 loadFileListFromData 加载
  // 这里不需要额外处理
  console.log('loadFileList 调用，当前模式:', props.mode)
}

// 获取字段状态
const getFieldStatus = (field: string) => {
  return formState.errors[field] ? 'error' : ''
}

// 获取字段错误信息
const getFieldError = (field: string) => {
  return formState.errors[field]?.[0] || ''
}

// 验证产品图片
const validateProductImages = (): boolean => {
  // 获取实图图片和平台图片
  const actualImages = formData.files?.actual_image || []
  const platformImages = formData.files?.platform_image || []

  // 检查是否至少有一类图片
  if (actualImages.length === 0 && platformImages.length === 0) {
    message.error(t('product.sku.form.imageRequired'))
    return false
  }

  return true
}

// 图片验证状态计算属性
const imageValidationStatus = computed(() => {
  const actualImages = formData.files?.actual_image || []
  const platformImages = formData.files?.platform_image || []

  const status = {
    hasImages: actualImages.length > 0 || platformImages.length > 0,
    actualCount: actualImages.length,
    platformCount: platformImages.length,
    isValid: actualImages.length > 0 || platformImages.length > 0
  }

  // 添加调试日志，帮助诊断问题
  console.log('[imageValidationStatus] 当前图片状态:', {
    formDataFiles: formData.files,
    actualImages,
    platformImages,
    status
  })

  return status
})

// SKU编码验证状态
const skuCodeValidating = ref(false)
// SKU序号验证状态
const skuNoValidating = ref(false)

// SKU编码验证
const validateSkuCode = async () => {
  if (!formData.skuCode || (isUpdateMode.value && props.mode !== 'copy') || props.mode === 'view')
    return

  // 基本格式验证
  if (formData.skuCode.length < 3) {
    setFieldError('skuCode', t('product.sku.form.validation.codeMin'))
    return
  }

  if (formData.skuCode.length > 100) {
    setFieldError('skuCode', t('product.sku.form.validation.codeMax'))
    return
  }

  // 格式验证
  if (!/^[A-Z0-9_-]+$/.test(formData.skuCode)) {
    setFieldError('skuCode', t('product.sku.form.validation.codePattern'))
    return
  }

  skuCodeValidating.value = true
  try {
    const result = await apiValidateSkuCode(formData.skuCode)
    if (!result.isValid) {
      setFieldError('skuCode', result.message || t('product.sku.form.validation.codeExists'))
    } else {
      clearFieldError('skuCode')
      // 验证通过，不显示成功提示
    }
  } catch (error) {
    console.error(t('product.sku.form.validation.codeFailed'), error)
    setFieldError('skuCode', t('product.sku.form.validation.retry'))
  } finally {
    skuCodeValidating.value = false
  }
}

// SKU序号验证（仅本地格式校验，不再校验唯一性）
const validateSkuNo = () => {
  if (props.mode === 'view') return

  const value = formData.skuNo as unknown as number | undefined | null

  if (value === undefined || value === null) {
    setFieldError('skuNo', t('product.sku.form.validation.numberRequired'))
    return
  }

  if (typeof value !== 'number' || isNaN(value)) {
    setFieldError('skuNo', t('product.sku.form.validation.numberType'))
    return
  }

  if (value < 1) {
    setFieldError('skuNo', t('product.sku.form.validation.numberPositive'))
    return
  }

  if (value > 999999) {
    setFieldError('skuNo', t('product.sku.form.validation.numberMax'))
    return
  }

  clearFieldError('skuNo')
}

// 表单提交成功
const onFinish = async (values: any) => {
  await handleSubmit()
}

// 表单提交失败
const onFinishFailed = (errorInfo: any) => {
  console.log('表单验证失败:', errorInfo)
  message.error(t('product.sku.form.validation.formFailed'))
}

// 处理提交
const handleSubmit = async () => {
  if (props.mode === 'view') return

  // 1. 自定义业务验证（Ant Design表单校验已通过）
  const isValid = await validateForm()
  if (!isValid) {
    // validateForm内部已经显示了错误提示，这里不再重复显示
    return
  }

  // 2. 产品图片验证
  const imageValid = validateProductImages()
  if (!imageValid) {
    return
  }

  // 3. 计算器验证
  if (calculatorErrors.value.length > 0) {
    message.error(t('product.sku.form.validation.dimensions'))
    return
  }

  submitLoading.value = true
  try {
    // 4. 收集和格式化表单数据
    const submitData = getSubmitData()

    // 5. 收集文件数据
    submitData.files = collectFilesData()

    // 6. 数据完整性检查
    if (!submitData.skuCode) {
      message.error(t('product.sku.form.validation.codeRequired'))
      return
    }

    console.log('提交数据:', submitData)

    // 7. 调用API并检查响应
    let response
    if (isUpdateMode.value) {
      response = await updateSku(submitData)
    } else {
      response = await createSku(submitData)
    }

    // 8. 检查接口响应是否成功
    if (!isSuccess(response)) {
      // 接口业务逻辑失败，显示错误消息但不关闭页面
      const errorMsg = response.message || t('product.sku.form.operationFailed')
      message.error({
        content: errorMsg,
        duration: 5,
        key: 'submit-error'
      })
      return
    }

    // 9. 成功后处理（成功提示由SKU拦截器处理，这里不再重复显示）
    formState.hasChanges = false

    // 延迟触发成功事件，让用户看到成功提示
    setTimeout(() => {
      emit('submit-success')
    }, 100)
  } catch (error: any) {
    console.error('提交失败:', error)

    // 8. 错误处理
    let errorMessage = t('product.sku.form.saveFailed')

    if (error?.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error?.message) {
      errorMessage = error.message
    }

    // 特殊错误处理
    if (errorMessage.includes('SKU编码')) {
      setFieldError('skuCode', errorMessage)
    } else if (errorMessage.includes('供应商')) {
      setFieldError('supplierCode', errorMessage)
    }

    message.error({
      content: errorMessage,
      duration: 5,
      key: 'submit-error'
    })
  } finally {
    submitLoading.value = false
  }
}

// 处理重置
const handleReset = () => {
  Modal.confirm({
    title: t('product.sku.form.resetTitle'),
    content: t('product.sku.form.resetContent'),
    okText: t('product.sku.form.resetConfirm'),
    cancelText: t('action.cancel'),
    okType: 'danger',
    onOk: () => {
      // 重置表单
      resetForm()

      message.success({
        content: t('product.sku.form.resetSuccess'),
        duration: 2
      })
    }
  })
}

// 处理取消
const handleCancel = () => {
  if (hasUnsavedChanges.value) {
    Modal.confirm({
      title: t('product.sku.form.leaveTitle'),
      content: t('product.sku.form.leaveContent'),
      okText: t('product.sku.form.leaveConfirm'),
      cancelText: t('action.cancel'),
      okType: 'danger',
      onOk: () => {
        emit('cancel')
      }
    })
  } else {
    emit('cancel')
  }
}

// 页面初始化
const initPanel = async () => {
  if (initializing.value) {
    console.log('面板正在初始化中，跳过重复调用')
    return
  }

  console.log('开始初始化面板...', { mode: props.mode, skuId: props.skuId })
  initializing.value = true

  try {
    // 并行加载用户数据和SKU数据
    const promises = []

    // 加载用户数据（如果还未初始化）
    if (!userDataInitialized.value) {
      promises.push(loadAllUsers())
    }

    // 根据模式加载SKU数据
    if (props.mode === 'edit' && props.skuId) {
      // 编辑模式，加载数据
      console.log('编辑模式，加载SKU数据:', props.skuId)
      promises.push(
        getSkuDetail(props.skuId).then(skuData => {
          initForm(skuData)
          loadFileListFromData(skuData)
          updateCalculatorDimensions()
          return loadFileList()
        })
      )
    } else if (props.mode === 'copy' && props.initialData) {
      // 复制模式，使用传入的数据
      console.log('复制模式，使用初始数据')

      // 处理复制数据：清空 SKU 编码和序号
      const copyData = { ...props.initialData }
      console.log('复制数据:', copyData.id)
      copyData.skuCode = '' // 清空 SKU 编码，让用户重新输入
      copyData.skuNo = undefined // 清空 SKU 序号，让用户重新输入

      initForm(copyData)
      updateCalculatorDimensions()
    } else if (props.mode === 'view' && props.skuId) {
      // 查看模式，加载数据
      console.log('查看模式，加载SKU数据:', props.skuId)
      promises.push(
        getSkuDetail(props.skuId).then(skuData => {
          initForm(skuData)
          loadFileListFromData(skuData)
          updateCalculatorDimensions()
          return loadFileList()
        })
      )
    } else {
      // 新建模式
      console.log('新建模式，使用默认初始化')
      initForm()
    }

    // 等待所有异步操作完成
    await Promise.all(promises)

    console.log('面板初始化完成')
  } catch (error) {
    console.error('面板初始化失败:', error)
    message.error(t('product.sku.form.initFailed'))
    emit('cancel')
  } finally {
    initializing.value = false
  }
}

// 监听属性变化，重新初始化
watch(
  () => [props.mode, props.skuId, props.initialData],
  () => {
    console.log('属性发生变化')
    initPanel()
  },
  { immediate: true }
)
</script>

<style scoped>
.sku-form-panel {
  /* 抵消内容区域的默认 margin */
  margin: -16px;
  /* 计算高度：100vh - header(48px) - multitab(40px) - footer(84px) */
  min-height: calc(100vh - 172px);
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.form-header {
  background: white;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  color: #666;
  font-weight: 500;
  transition: all 0.3s ease;
}

.back-btn:hover {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.1);
}

.title-section {
  margin: 0;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.page-subtitle {
  margin: 4px 0 0 0;
  color: #8c8c8c;
  font-size: 14px;
}

.header-actions :deep(.ant-btn) {
  border-radius: 6px;
  font-weight: 500;
}

/* 表单内容区域 */
.form-content {
  flex: 1;
  padding: 24px 0;
  overflow-y: auto;
}

.form-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 16px;
}

/* 步骤指示器 */
.form-steps {
  background: white;
  border-radius: 6px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8e8e8;
}

.form-steps :deep(.ant-steps-item-title) {
  font-weight: 500;
}

/* 表单区域 */
.form-sections {
  gap: 24px;
}

/* 表单卡片 */
.form-section {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
  border: 1px solid #e8e8e8;
}

.section-header {
  background: #fafafa;
  padding: 20px 24px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.section-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.section-title p {
  margin: 4px 0 0 0;
  color: #8c8c8c;
  font-size: 14px;
}

.section-content {
  padding: 32px 24px;
}

/* 表单项样式 */
:deep(.ant-form-item) {
  margin-bottom: 24px;
}

:deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #262626;
  font-size: 14px;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-input-number) {
  border-radius: 6px;
}

/* 步骤指示器 */
.form-steps {
  background: white;
  border-radius: 6px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid #e8e8e8;
}

.form-steps :deep(.ant-steps-item-title) {
  font-weight: 500;
}

/* 表单区域 */
.form-sections {
  gap: 24px;
}

/* 体积参数区域样式 */
.dimensions-section {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  margin: 20px 0;
  border: 1px solid #e8e8e8;
}

.subsection-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  display: flex;
  align-items: center;
}

/* 包装尺寸组合输入框样式 */
.package-dimensions-wrapper {
  margin-bottom: 24px !important;
}

.package-dimensions-group {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
}

.dimension-input-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.dimension-input {
  margin-bottom: 0 !important;
  width: 100%;
}

.dimension-input :deep(.ant-form-item-control) {
  width: 100%;
  min-height: 40px;
}

.dimension-input :deep(.ant-input-number) {
  width: 100%;
}

.dimension-input :deep(.ant-form-item-explain) {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 1;
  margin-top: 4px;
}

.dimension-separator {
  color: #999;
  font-weight: 500;
  font-size: 16px;
  padding: 0 4px;
  user-select: none;
  margin-top: 6px;
  line-height: 40px;
  flex-shrink: 0;
}

.dimension-unit-wrapper {
  min-width: 120px;
  flex-shrink: 0;
}

.dimension-unit-select {
  margin-bottom: 0 !important;
  width: 100%;
}

.dimension-unit-select :deep(.ant-form-item-control) {
  width: 100%;
  min-height: 40px;
}

.dimension-unit-select :deep(.ant-select) {
  width: 100%;
}

/* 计算结果样式 */
.calculated-results {
  background: linear-gradient(135deg, #f6ffed 0%, #f0f9ff 100%);
  border-radius: 8px;
  padding: 20px;
  margin: 20px 0;
  border: 1px solid #b7eb8f;
}

.calculated-results :deep(.ant-form-item-label) {
  font-weight: 600;
  color: #262626;
}

.calculated-input {
  font-weight: 600;
}

.calculated-input.volume :deep(.ant-input[disabled]) {
  background-color: #f0f9ff;
  border-color: #1890ff;
  color: #1890ff;
}

.calculated-input.density-kg :deep(.ant-input[disabled]) {
  background-color: #f6ffed;
  border-color: #52c41a;
  color: #52c41a;
}

.calculated-input.density-g :deep(.ant-input[disabled]) {
  background-color: #f9f0ff;
  border-color: #722ed1;
  color: #722ed1;
}

.calculated-input.capacity :deep(.ant-input[disabled]) {
  background-color: #fff7e6;
  border-color: #faad14;
  color: #faad14;
}

/* 图片上传区域样式 */
.image-upload-section {
  margin: 24px 0;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.image-upload-section:first-child {
  margin-top: 0;
}

.image-upload-section:last-child {
  margin-bottom: 0;
}

/* 图片验证状态样式 */
.image-validation-status {
  margin-bottom: 16px;
}

.image-validation-status :deep(.ant-alert) {
  border-radius: 6px;
}

.image-validation-status :deep(.ant-alert-success) {
  background-color: #f6ffed;
  border-color: #b7eb8f;
}

.image-validation-status :deep(.ant-alert-warning) {
  background-color: #fffbe6;
  border-color: #ffe58f;
}

/* OSS 图片上传组件样式已在组件内部定义 */

/* 图片预览模态框样式 */
.image-preview-modal .preview-info {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
}

.image-preview-modal :deep(.ant-descriptions-item-label) {
  font-weight: 600;
}

/* 按钮组样式优化 */
:deep(.ant-btn-group .ant-btn) {
  border-radius: 0 !important;
  transition: all 0.3s ease;
}

:deep(.ant-btn-group .ant-btn:first-child) {
  border-radius: 6px 0 0 6px !important;
}

:deep(.ant-btn-group .ant-btn:last-child) {
  border-radius: 0 6px 6px 0 !important;
}

:deep(.ant-btn-group .ant-btn-primary) {
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.2);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .form-container {
    padding: 0 16px;
  }

  .header-content {
    padding: 16px;
  }
}

@media (max-width: 768px) {
  .form-container {
    padding: 0 12px;
  }

  .header-content {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .header-left {
    width: 100%;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions :deep(.ant-space) {
    width: 100%;
    justify-content: flex-end;
  }

  .section-content {
    padding: 24px 16px;
  }

  .form-steps {
    padding: 16px;
  }

  .section-header {
    padding: 20px 16px;
  }

  .page-title {
    font-size: 20px;
  }
}
</style>
