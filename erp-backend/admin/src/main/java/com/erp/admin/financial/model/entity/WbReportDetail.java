package com.erp.admin.financial.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.admin.platform.wildberries.json.WbDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.ballcat.mybatisplus.alias.TableAlias;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableAlias("wrd")
@TableName("wb_report_detail")
@Data
@Schema(description = "WB 报表明细原始数据（reportDetailByPeriod）")
public class WbReportDetail {

    @Schema(description = "主键ID")
    private Long id;

	@Schema(title = "店铺ID")
	private Long shopId;

	@Schema(description = "报表周期类型：weekly-周报, daily-日报")
	private String periodType;

    @JsonProperty("realizationreport_id")
    @Schema(description = "报表ID")
    private Long realizationreportId;

    @JsonProperty("date_from")
    @Schema(description = "报表开始日期")
    private LocalDate dateFrom;

    @JsonProperty("date_to")
    @Schema(description = "报表结束日期")
    private LocalDate dateTo;

    @JsonProperty("create_dt")
    @Schema(description = "记录生成日期")
    private LocalDate createDt;

    @JsonProperty("currency_name")
    @Schema(description = "币种名称")
    private String currencyName;

    @JsonProperty("suppliercontract_code")
    @Schema(description = "合同编码")
    private String suppliercontractCode;

    @JsonProperty("rrd_id")
    @Schema(description = "行ID（报表明细唯一标识）")
    private Long rrdId;

    @JsonProperty("gi_id")
    @Schema(description = "物流收货ID")
    private Long giId;

    @JsonProperty("dlv_prc")
    @Schema(description = "配送价格")
    private BigDecimal dlvPrc;

    @JsonProperty("fix_tariff_date_from")
    @Schema(description = "固定费率开始日期")
    private LocalDate fixTariffDateFrom;

    @JsonProperty("fix_tariff_date_to")
    @Schema(description = "固定费率结束日期")
    private LocalDate fixTariffDateTo;

    @JsonProperty("subject_name")
    @Schema(description = "品类名称")
    private String subjectName;

    @JsonProperty("nm_id")
    @Schema(description = "商品ID（NMID）")
    private Long nmId;

    @JsonProperty("brand_name")
    @Schema(description = "品牌名")
    private String brandName;

    @JsonProperty("sa_name")
    @Schema(description = "品类组名称")
    private String saName;

    @JsonProperty("ts_name")
    @Schema(description = "尺寸")
    private String tsName;

    @JsonProperty("barcode")
    @Schema(description = "条码")
    private String barcode;

    @JsonProperty("doc_type_name")
    @Schema(description = "操作类型（出售/退货等）")
    private String docTypeName;

    @JsonProperty("quantity")
    @Schema(description = "数量")
    private Integer quantity;

    @JsonProperty("retail_price")
    @Schema(description = "零售价")
    private BigDecimal retailPrice;

    @JsonProperty("retail_amount")
    @Schema(description = "零售额")
    private BigDecimal retailAmount;

    @JsonProperty("sale_percent")
    @Schema(description = "折扣百分比")
    private BigDecimal salePercent;

    @JsonProperty("commission_percent")
    @Schema(description = "佣金百分比")
    private BigDecimal commissionPercent;

    @JsonProperty("office_name")
    @Schema(description = "仓库名称")
    private String officeName;

    @JsonProperty("supplier_oper_name")
    @Schema(description = "供应商操作名称")
    private String supplierOperName;

    @JsonProperty("order_dt")
    @JsonDeserialize(using = WbDateTimeDeserializer.class)
    @Schema(description = "下单时间")
    private LocalDateTime orderDt;

    @JsonProperty("sale_dt")
    @JsonDeserialize(using = WbDateTimeDeserializer.class)
    @Schema(description = "销售时间")
    private LocalDateTime saleDt;

    @JsonProperty("rr_dt")
    @JsonDeserialize(using = com.erp.admin.platform.wildberries.json.WbFlexibleDateDeserializer.class)
    @Schema(description = "操作日期（可能包含时间，按日期存储）")
    private LocalDate rrDt;

    @JsonProperty("shk_id")
    @Schema(description = "货品ID")
    private Long shkId;

    @JsonProperty("retail_price_withdisc_rub")
    @Schema(description = "折后价（RUB）")
    private BigDecimal retailPriceWithdiscRub;

    @JsonProperty("delivery_amount")
    @Schema(description = "配送金额")
    private BigDecimal deliveryAmount;

    @JsonProperty("return_amount")
    @Schema(description = "退货金额")
    private BigDecimal returnAmount;

    @JsonProperty("delivery_rub")
    @Schema(description = "配送金额（RUB）")
    private BigDecimal deliveryRub;

    @JsonProperty("gi_box_type_name")
    @Schema(description = "物流箱型")
    private String giBoxTypeName;

    @JsonProperty("product_discount_for_report")
    @Schema(description = "产品折扣")
    private BigDecimal productDiscountForReport;

    @JsonProperty("supplier_promo")
    @Schema(description = "供应商促销金额")
    private BigDecimal supplierPromo;

    @JsonProperty("ppvz_spp_prc")
    @Schema(description = "SPP 百分比")
    private BigDecimal ppvzSppPrc;

    @JsonProperty("ppvz_kvw_prc_base")
    @Schema(description = "基础KWV百分比")
    private BigDecimal ppvzKvwPrcBase;

    @JsonProperty("ppvz_kvw_prc")
    @Schema(description = "KWV百分比")
    private BigDecimal ppvzKvwPrc;

    @JsonProperty("sup_rating_prc_up")
    @Schema(description = "评级加成百分比")
    private BigDecimal supRatingPrcUp;

    @JsonProperty("is_kgvp_v2")
    @Schema(description = "是否为KGV2模式")
    private Integer isKgvpV2;

    @JsonProperty("ppvz_sales_commission")
    @Schema(description = "销售佣金")
    private BigDecimal ppvzSalesCommission;

    @JsonProperty("ppvz_for_pay")
    @Schema(description = "应付金额")
    private BigDecimal ppvzForPay;

    @JsonProperty("ppvz_reward")
    @Schema(description = "奖励金额")
    private BigDecimal ppvzReward;

    @JsonProperty("acquiring_fee")
    @Schema(description = "收单手续费")
    private BigDecimal acquiringFee;

    @JsonProperty("acquiring_percent")
    @Schema(description = "收单费百分比")
    private BigDecimal acquiringPercent;

    @JsonProperty("payment_processing")
    @Schema(description = "支付处理方式")
    private String paymentProcessing;

    @JsonProperty("acquiring_bank")
    @Schema(description = "收单银行")
    private String acquiringBank;

    @JsonProperty("ppvz_vw")
    @Schema(description = "VW金额")
    private BigDecimal ppvzVw;

    @JsonProperty("ppvz_vw_nds")
    @Schema(description = "VW金额（含增值税）")
    private BigDecimal ppvzVwNds;

    @JsonProperty("ppvz_office_name")
    @Schema(description = "仓库名称")
    private String ppvzOfficeName;

    @JsonProperty("ppvz_office_id")
    @Schema(description = "仓库ID")
    private Long ppvzOfficeId;

    @JsonProperty("ppvz_supplier_id")
    @Schema(description = "供应商ID")
    private Long ppvzSupplierId;

    @JsonProperty("ppvz_supplier_name")
    @Schema(description = "供应商名称")
    private String ppvzSupplierName;

    @JsonProperty("ppvz_inn")
    @Schema(description = "供应商税号")
    private String ppvzInn;

    @JsonProperty("declaration_number")
    @Schema(description = "报关单号")
    private String declarationNumber;

    @JsonProperty("bonus_type_name")
    @Schema(description = "奖励类型")
    private String bonusTypeName;

    @JsonProperty("sticker_id")
    @Schema(description = "贴纸ID")
    private String stickerId;

    @JsonProperty("site_country")
    @Schema(description = "站点国家")
    private String siteCountry;

    @JsonProperty("srv_dbs")
    @Schema(description = "是否DBS订单")
    private Boolean srvDbs;

    @JsonProperty("penalty")
    @Schema(description = "罚款金额")
    private BigDecimal penalty;

    @JsonProperty("additional_payment")
    @Schema(description = "附加支付")
    private BigDecimal additionalPayment;

    @JsonProperty("rebill_logistic_cost")
    @Schema(description = "物流再计费成本")
    private BigDecimal rebillLogisticCost;

    @JsonProperty("rebill_logistic_org")
    @Schema(description = "物流再计费组织")
    private String rebillLogisticOrg;

    @JsonProperty("storage_fee")
    @Schema(description = "仓储费")
    private BigDecimal storageFee;

    @JsonProperty("deduction")
    @Schema(description = "扣款")
    private BigDecimal deduction;

    @JsonProperty("acceptance")
    @Schema(description = "验收入库")
    private BigDecimal acceptance;

    @JsonProperty("assembly_id")
    @Schema(description = "装配单ID")
    private Long assemblyId;

    @JsonProperty("kiz")
    @Schema(description = "KIZ码")
    private String kiz;

    @JsonProperty("srid")
    @Schema(description = "唯一订单行ID（全局唯一）")
    private String srid;

    @JsonProperty("report_type")
    @Schema(description = "报表类型")
    private Integer reportType;

    @JsonProperty("is_legal_entity")
    @Schema(description = "是否法人主体")
    private Boolean isLegalEntity;

    @JsonProperty("trbx_id")
    @Schema(description = "TRBX ID")
    private String trbxId;

    @JsonProperty("installment_cofinancing_amount")
    @Schema(description = "分期共同融资金额")
    private BigDecimal installmentCofinancingAmount;

    @JsonProperty("wibes_wb_discount_percent")
    @Schema(description = "WB折扣百分比")
    private BigDecimal wibesWbDiscountPercent;

    @JsonProperty("cashback_amount")
    @Schema(description = "返现金额")
    private BigDecimal cashbackAmount;

    @JsonProperty("cashback_discount")
    @Schema(description = "返现折扣")
    private BigDecimal cashbackDiscount;

    @JsonProperty("cashback_commission_change")
    @Schema(description = "返现佣金变化")
    private BigDecimal cashbackCommissionChange;

    @JsonProperty("order_uid")
    @Schema(description = "订单UID")
    private String orderUid;

    @JsonProperty("payment_schedule")
    @Schema(description = "支付计划")
    private Integer paymentSchedule;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
