<template>
  <a-modal :open="open" title="财务报表明细" width="900px" :footer="null" @cancel="handleClose">
    <div v-if="record" class="detail-container">
      <!-- 基本信息 -->
      <a-card title="基本信息" size="small" class="detail-section">
        <a-row :gutter="[16, 16]">
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">报表周期 / period_type:</span>
              <span class="detail-value">
                <a-tag :color="record.periodType === 'weekly' ? 'blue' : 'green'">
                  {{ record.periodType === 'weekly' ? '周报 Weekly' : '日报 Daily' }}
                </a-tag>
              </span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">报表ID / realizationreport_id:</span>
              <span class="detail-value">{{ formatValue(record.realizationreportId) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">店铺 / shop_name:</span>
              <span class="detail-value">{{ formatValue(record.shopName) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">币种 / currency_name:</span>
              <span class="detail-value">{{ formatValue(record.currencyName) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">报表开始日期 / date_from:</span>
              <span class="detail-value">{{ formatDate(record.dateFrom) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">报表结束日期 / date_to:</span>
              <span class="detail-value">{{ formatDate(record.dateTo) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">记录生成日期 / create_dt:</span>
              <span class="detail-value">{{ formatDate(record.createDt) }}</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="detail-item">
              <span class="detail-label">合同编码 / suppliercontract_code:</span>
              <span class="detail-value">{{ formatValue(record.suppliercontractCode) }}</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="detail-item">
              <span class="detail-label">行ID / rrd_id:</span>
              <span class="detail-value">{{ formatValue(record.rrdId) }}</span>
            </div>
          </a-col>
        </a-row>
      </a-card>

      <!-- 商品信息 -->
      <a-card title="商品信息" size="small" class="detail-section">
        <a-row :gutter="[16, 16]">
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">商品ID / nm_id:</span>
              <span class="detail-value">{{ formatValue(record.nmId) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">品牌 / brand_name:</span>
              <span class="detail-value">{{ formatValue(record.brandName) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">品类 / subject_name:</span>
              <span class="detail-value">{{ formatValue(record.subjectName) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">品类组 / sa_name:</span>
              <span class="detail-value">{{ formatValue(record.saName) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">尺寸 / ts_name:</span>
              <span class="detail-value">{{ formatValue(record.tsName) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">条码 / barcode:</span>
              <span class="detail-value">{{ formatValue(record.barcode) }}</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="detail-item">
              <span class="detail-label">货品ID / shk_id:</span>
              <span class="detail-value">{{ formatValue(record.shkId) }}</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="detail-item">
              <span class="detail-label">贴纸ID / sticker_id:</span>
              <span class="detail-value">{{ formatValue(record.stickerId) }}</span>
            </div>
          </a-col>
        </a-row>
      </a-card>

      <!-- 订单信息 -->
      <a-card title="订单信息" size="small" class="detail-section">
        <a-row :gutter="[16, 16]">
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">操作类型 / doc_type_name:</span>
              <span class="detail-value">{{ formatValue(record.docTypeName) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">数量 / quantity:</span>
              <span class="detail-value">{{ formatValue(record.quantity) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">订单UID / order_uid:</span>
              <span class="detail-value">{{ formatValue(record.orderUid) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">下单时间 / order_dt:</span>
              <span class="detail-value">{{ formatDateTime(record.orderDt) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">销售时间 / sale_dt:</span>
              <span class="detail-value">{{ formatDateTime(record.saleDt) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">操作日期 / rr_dt:</span>
              <span class="detail-value">{{ formatDate(record.rrDt) }}</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="detail-item">
              <span class="detail-label">供应商操作 / supplier_oper_name:</span>
              <span class="detail-value">{{ formatValue(record.supplierOperName) }}</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="detail-item">
              <span class="detail-label">唯一订单行ID / srid:</span>
              <span class="detail-value">{{ formatValue(record.srid) }}</span>
            </div>
          </a-col>
        </a-row>
      </a-card>

      <!-- 金额明细 -->
      <a-card title="金额明细" size="small" class="detail-section">
        <a-row :gutter="[16, 16]">
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">零售价 / retail_price:</span>
              <span class="detail-value">{{
                formatCurrency(record.retailPrice, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">零售额 / retail_amount:</span>
              <span class="detail-value">{{
                formatCurrency(record.retailAmount, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">折后价 / retail_price_withdisc_rub:</span>
              <span class="detail-value">{{
                formatCurrency(record.retailPriceWithdiscRub, 'RUB')
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">折扣百分比 / sale_percent:</span>
              <span class="detail-value">{{ formatPercent(record.salePercent) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">佣金百分比 / commission_percent:</span>
              <span class="detail-value">{{ formatPercent(record.commissionPercent) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">销售佣金 / ppvz_sales_commission:</span>
              <span class="detail-value">{{
                formatCurrency(record.ppvzSalesCommission, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">应付金额 / ppvz_for_pay:</span>
              <span class="detail-value highlight">{{
                formatCurrency(record.ppvzForPay, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">奖励金额 / ppvz_reward:</span>
              <span class="detail-value">{{
                formatCurrency(record.ppvzReward, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">产品折扣 / product_discount_for_report:</span>
              <span class="detail-value">{{
                formatCurrency(record.productDiscountForReport, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">供应商促销 / supplier_promo:</span>
              <span class="detail-value">{{
                formatCurrency(record.supplierPromo, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">收单手续费 / acquiring_fee:</span>
              <span class="detail-value">{{
                formatCurrency(record.acquiringFee, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">收单费百分比 / acquiring_percent:</span>
              <span class="detail-value">{{ formatPercent(record.acquiringPercent) }}</span>
            </div>
          </a-col>
        </a-row>
      </a-card>

      <!-- 物流信息 -->
      <a-card title="物流信息" size="small" class="detail-section">
        <a-row :gutter="[16, 16]">
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">仓库 / office_name:</span>
              <span class="detail-value">{{ formatValue(record.officeName) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">配送金额 / delivery_amount:</span>
              <span class="detail-value">{{
                formatCurrency(record.deliveryAmount, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">配送金额RUB / delivery_rub:</span>
              <span class="detail-value">{{ formatCurrency(record.deliveryRub, 'RUB') }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">物流箱型 / gi_box_type_name:</span>
              <span class="detail-value">{{ formatValue(record.giBoxTypeName) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">物流收货ID / gi_id:</span>
              <span class="detail-value">{{ formatValue(record.giId) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">配送价格 / dlv_prc:</span>
              <span class="detail-value">{{
                formatCurrency(record.dlvPrc, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">物流再计费成本 / rebill_logistic_cost:</span>
              <span class="detail-value">{{
                formatCurrency(record.rebillLogisticCost, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">物流再计费组织 / rebill_logistic_org:</span>
              <span class="detail-value">{{ formatValue(record.rebillLogisticOrg) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">站点国家 / site_country:</span>
              <span class="detail-value">{{ formatValue(record.siteCountry) }}</span>
            </div>
          </a-col>
        </a-row>
      </a-card>

      <!-- 税务信息 -->
      <a-card title="税务信息" size="small" class="detail-section">
        <a-row :gutter="[16, 16]">
          <a-col :span="12">
            <div class="detail-item">
              <span class="detail-label">供应商税号 / ppvz_inn:</span>
              <span class="detail-value">{{ formatValue(record.ppvzInn) }}</span>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="detail-item">
              <span class="detail-label">报关单号 / declaration_number:</span>
              <span class="detail-value">{{ formatValue(record.declarationNumber) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">VW金额 / ppvz_vw:</span>
              <span class="detail-value">{{
                formatCurrency(record.ppvzVw, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">VW金额含税 / ppvz_vw_nds:</span>
              <span class="detail-value">{{
                formatCurrency(record.ppvzVwNds, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">是否法人主体 / is_legal_entity:</span>
              <span class="detail-value">{{ formatBoolean(record.isLegalEntity) }}</span>
            </div>
          </a-col>
        </a-row>
      </a-card>

      <!-- 其他费用 -->
      <a-card title="其他费用" size="small" class="detail-section">
        <a-row :gutter="[16, 16]">
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">退货金额 / return_amount:</span>
              <span class="detail-value">{{
                formatCurrency(record.returnAmount, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">罚款 / penalty:</span>
              <span class="detail-value">{{
                formatCurrency(record.penalty, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">仓储费 / storage_fee:</span>
              <span class="detail-value">{{
                formatCurrency(record.storageFee, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">扣款 / deduction:</span>
              <span class="detail-value">{{
                formatCurrency(record.deduction, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">验收入库 / acceptance:</span>
              <span class="detail-value">{{
                formatCurrency(record.acceptance, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">附加支付 / additional_payment:</span>
              <span class="detail-value">{{
                formatCurrency(record.additionalPayment, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">返现金额 / cashback_amount:</span>
              <span class="detail-value">{{
                formatCurrency(record.cashbackAmount, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">返现折扣 / cashback_discount:</span>
              <span class="detail-value">{{
                formatCurrency(record.cashbackDiscount, record.currencyName)
              }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="detail-item">
              <span class="detail-label">返现佣金变化 / cashback_commission_change:</span>
              <span class="detail-value">{{
                formatCurrency(record.cashbackCommissionChange, record.currencyName)
              }}</span>
            </div>
          </a-col>
        </a-row>
      </a-card>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import type { WbReportDetailVO } from '@/api/financial/wb-report/types'
import {
  formatDate,
  formatDateTime,
  formatCurrency,
  formatEmptyValue,
  formatPercent,
  formatBoolean
} from '@/utils/financial-utils'

interface Props {
  open: boolean
  record: WbReportDetailVO | null
}

const props = defineProps<Props>()

const emits = defineEmits<{
  (e: 'update:open', value: boolean): void
}>()

const handleClose = () => {
  emits('update:open', false)
}

const formatValue = (value: any) => {
  return formatEmptyValue(value)
}
</script>

<style scoped>
.detail-container {
  max-height: 70vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
  word-break: break-all;
}

.detail-value.highlight {
  color: #1890ff;
  font-weight: 600;
  font-size: 15px;
}

:deep(.ant-card-head) {
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}

:deep(.ant-card-head-title) {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

:deep(.ant-card-body) {
  padding: 16px;
}
</style>
