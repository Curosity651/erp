package com.erp.admin.financial.model.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WB 财务报表导出视图对象（全量字段）
 */
@ContentRowHeight(20)
@Data
@Schema(description = "WB 财务报表导出数据")
public class WbReportDetailExportVO {

	@ColumnWidth(18)
	@ExcelProperty({"主键ID", "id"})
	private Long id;

	@ColumnWidth(18)
	@ExcelProperty({"店铺ID", "shop_id"})
	private Long shopId;

	@ColumnWidth(15)
	@ExcelProperty({"报表周期类型", "period_type"})
	private String periodType;

	@ColumnWidth(18)
	@ExcelProperty({"报表ID", "realizationreport_id"})
	private Long realizationreportId;

	@ColumnWidth(18)
	@ExcelProperty({"报表开始日期", "date_from"})
	private LocalDate dateFrom;

	@ColumnWidth(18)
	@ExcelProperty({"报表结束日期", "date_to"})
	private LocalDate dateTo;

	@ColumnWidth(18)
	@ExcelProperty({"记录生成日期", "create_dt"})
	private LocalDate createDt;

	@ColumnWidth(18)
	@ExcelProperty({"币种名称", "currency_name"})
	private String currencyName;

	@ColumnWidth(20)
	@ExcelProperty({"合同编码", "suppliercontract_code"})
	private String suppliercontractCode;

	@ColumnWidth(18)
	@ExcelProperty({"行ID", "rrd_id"})
	private String rrdId;

	@ColumnWidth(18)
	@ExcelProperty({"物流收货ID", "gi_id"})
	private Long giId;

	@ColumnWidth(18)
	@ExcelProperty({"配送价格", "dlv_prc"})
	private BigDecimal dlvPrc;

	@ColumnWidth(18)
	@ExcelProperty({"固定费率开始日期", "fix_tariff_date_from"})
	private LocalDate fixTariffDateFrom;

	@ColumnWidth(18)
	@ExcelProperty({"固定费率结束日期", "fix_tariff_date_to"})
	private LocalDate fixTariffDateTo;

	@ColumnWidth(18)
	@ExcelProperty({"品类名称", "subject_name"})
	private String subjectName;

	@ColumnWidth(18)
	@ExcelProperty({"商品ID(NMID)", "nm_id"})
	private Long nmId;

	@ColumnWidth(18)
	@ExcelProperty({"品牌名", "brand_name"})
	private String brandName;

	@ColumnWidth(18)
	@ExcelProperty({"品类组名称", "sa_name"})
	private String saName;

	@ColumnWidth(12)
	@ExcelProperty({"尺寸", "ts_name"})
	private String tsName;

	@ColumnWidth(18)
	@ExcelProperty({"条码", "barcode"})
	private String barcode;

	@ColumnWidth(20)
	@ExcelProperty({"操作类型", "doc_type_name"})
	private String docTypeName;

	@ColumnWidth(12)
	@ExcelProperty({"数量", "quantity"})
	private Integer quantity;

	@ColumnWidth(18)
	@ExcelProperty({"零售价", "retail_price"})
	private BigDecimal retailPrice;

	@ColumnWidth(18)
	@ExcelProperty({"零售额", "retail_amount"})
	private BigDecimal retailAmount;

	@ColumnWidth(18)
	@ExcelProperty({"折扣百分比", "sale_percent"})
	private BigDecimal salePercent;

	@ColumnWidth(18)
	@ExcelProperty({"佣金百分比", "commission_percent"})
	private BigDecimal commissionPercent;

	@ColumnWidth(18)
	@ExcelProperty({"仓库名称", "office_name"})
	private String officeName;

	@ColumnWidth(22)
	@ExcelProperty({"供应商操作名称", "supplier_oper_name"})
	private String supplierOperName;

	@ColumnWidth(22)
	@ExcelProperty({"下单时间", "order_dt"})
	private LocalDateTime orderDt;

	@ColumnWidth(22)
	@ExcelProperty({"销售时间", "sale_dt"})
	private LocalDateTime saleDt;

	@ColumnWidth(18)
	@ExcelProperty({"操作日期", "rr_dt"})
	private LocalDate rrDt;

	@ColumnWidth(18)
	@ExcelProperty({"货品ID", "shk_id"})
	private Long shkId;

	@ColumnWidth(20)
	@ExcelProperty({"折后价(RUB)", "retail_price_withdisc_rub"})
	private BigDecimal retailPriceWithdiscRub;

	@ColumnWidth(18)
	@ExcelProperty({"配送金额", "delivery_amount"})
	private BigDecimal deliveryAmount;

	@ColumnWidth(18)
	@ExcelProperty({"退货金额", "return_amount"})
	private BigDecimal returnAmount;

	@ColumnWidth(20)
	@ExcelProperty({"配送金额(RUB)", "delivery_rub"})
	private BigDecimal deliveryRub;

	@ColumnWidth(18)
	@ExcelProperty({"物流箱型", "gi_box_type_name"})
	private String giBoxTypeName;

	@ColumnWidth(18)
	@ExcelProperty({"产品折扣", "product_discount_for_report"})
	private BigDecimal productDiscountForReport;

	@ColumnWidth(20)
	@ExcelProperty({"供应商促销金额", "supplier_promo"})
	private BigDecimal supplierPromo;

	@ColumnWidth(18)
	@ExcelProperty({"SPP百分比", "ppvz_spp_prc"})
	private BigDecimal ppvzSppPrc;

	@ColumnWidth(22)
	@ExcelProperty({"基础KWV百分比", "ppvz_kwv_prc_base"})
	private BigDecimal ppvzKvwPrcBase;

	@ColumnWidth(18)
	@ExcelProperty({"KWV百分比", "ppvz_kwv_prc"})
	private BigDecimal ppvzKvwPrc;

	@ColumnWidth(22)
	@ExcelProperty({"评级加成百分比", "sup_rating_prc_up"})
	private BigDecimal supRatingPrcUp;

	@ColumnWidth(18)
	@ExcelProperty({"是否KGV2模式", "is_kgvp_v2"})
	private Integer isKgvpV2;

	@ColumnWidth(18)
	@ExcelProperty({"销售佣金", "ppvz_sales_commission"})
	private BigDecimal ppvzSalesCommission;

	@ColumnWidth(18)
	@ExcelProperty({"应付金额", "ppvz_for_pay"})
	private BigDecimal ppvzForPay;

	@ColumnWidth(18)
	@ExcelProperty({"奖励金额", "ppvz_reward"})
	private BigDecimal ppvzReward;

	@ColumnWidth(18)
	@ExcelProperty({"收单手续费", "acquiring_fee"})
	private BigDecimal acquiringFee;

	@ColumnWidth(18)
	@ExcelProperty({"收单费百分比", "acquiring_percent"})
	private BigDecimal acquiringPercent;

	@ColumnWidth(18)
	@ExcelProperty({"支付处理方式", "payment_processing"})
	private String paymentProcessing;

	@ColumnWidth(18)
	@ExcelProperty({"收单银行", "acquiring_bank"})
	private String acquiringBank;

	@ColumnWidth(18)
	@ExcelProperty({"VW金额", "ppvz_vw"})
	private BigDecimal ppvzVw;

	@ColumnWidth(22)
	@ExcelProperty({"VW金额(含增值税)", "ppvz_vw_nds"})
	private BigDecimal ppvzVwNds;

	@ColumnWidth(18)
	@ExcelProperty({"仓库名称(结算)", "ppvz_office_name"})
	private String ppvzOfficeName;

	@ColumnWidth(18)
	@ExcelProperty({"仓库ID(结算)", "ppvz_office_id"})
	private Long ppvzOfficeId;

	@ColumnWidth(18)
	@ExcelProperty({"供应商ID", "ppvz_supplier_id"})
	private Long ppvzSupplierId;

	@ColumnWidth(18)
	@ExcelProperty({"供应商名称", "ppvz_supplier_name"})
	private String ppvzSupplierName;

	@ColumnWidth(18)
	@ExcelProperty({"供应商税号", "ppvz_inn"})
	private String ppvzInn;

	@ColumnWidth(18)
	@ExcelProperty({"报关单号", "declaration_number"})
	private String declarationNumber;

	@ColumnWidth(18)
	@ExcelProperty({"奖励类型", "bonus_type_name"})
	private String bonusTypeName;

	@ColumnWidth(18)
	@ExcelProperty({"贴纸ID", "sticker_id"})
	private String stickerId;

	@ColumnWidth(18)
	@ExcelProperty({"站点国家", "site_country"})
	private String siteCountry;

	@ColumnWidth(18)
	@ExcelProperty({"是否DBS订单", "srv_dbs"})
	private Boolean srvDbs;

	@ColumnWidth(18)
	@ExcelProperty({"罚款金额", "penalty"})
	private BigDecimal penalty;

	@ColumnWidth(18)
	@ExcelProperty({"附加支付", "additional_payment"})
	private BigDecimal additionalPayment;

	@ColumnWidth(22)
	@ExcelProperty({"物流再计费成本", "rebill_logistic_cost"})
	private BigDecimal rebillLogisticCost;

	@ColumnWidth(22)
	@ExcelProperty({"物流再计费组织", "rebill_logistic_org"})
	private String rebillLogisticOrg;

	@ColumnWidth(18)
	@ExcelProperty({"仓储费", "storage_fee"})
	private BigDecimal storageFee;

	@ColumnWidth(18)
	@ExcelProperty({"扣款", "deduction"})
	private BigDecimal deduction;

	@ColumnWidth(18)
	@ExcelProperty({"验收入库", "acceptance"})
	private BigDecimal acceptance;

	@ColumnWidth(18)
	@ExcelProperty({"装配单ID", "assembly_id"})
	private Long assemblyId;

	@ColumnWidth(18)
	@ExcelProperty({"KIZ码", "kiz"})
	private String kiz;

	@ColumnWidth(24)
	@ExcelProperty({"唯一订单行ID(srid)", "srid"})
	private String srid;

	@ColumnWidth(18)
	@ExcelProperty({"报表类型", "report_type"})
	private Integer reportType;

	@ColumnWidth(18)
	@ExcelProperty({"是否法人主体", "is_legal_entity"})
	private Boolean isLegalEntity;

	@ColumnWidth(18)
	@ExcelProperty({"TRBX ID", "trbx_id"})
	private String trbxId;

	@ColumnWidth(24)
	@ExcelProperty({"分期共同融资金额", "installment_cofinancing_amount"})
	private BigDecimal installmentCofinancingAmount;

	@ColumnWidth(24)
	@ExcelProperty({"WB折扣百分比", "wibes_wb_discount_percent"})
	private BigDecimal wibesWbDiscountPercent;

	@ColumnWidth(18)
	@ExcelProperty({"返现金额", "cashback_amount"})
	private BigDecimal cashbackAmount;

	@ColumnWidth(18)
	@ExcelProperty({"返现折扣", "cashback_discount"})
	private BigDecimal cashbackDiscount;

	@ColumnWidth(24)
	@ExcelProperty({"返现佣金变化", "cashback_commission_change"})
	private BigDecimal cashbackCommissionChange;

	@ColumnWidth(22)
	@ExcelProperty({"订单UID", "order_uid"})
	private String orderUid;

	@ColumnWidth(18)
	@ExcelProperty({"支付计划", "payment_schedule"})
	private Integer paymentSchedule;

	@ColumnWidth(22)
	@ExcelProperty({"创建时间", "create_time"})
	private LocalDateTime createTime;

	@ColumnWidth(22)
	@ExcelProperty({"更新时间", "update_time"})
	private LocalDateTime updateTime;

}
