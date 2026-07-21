package com.erp.admin.financial.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WB 财务报表明细分页展示 VO
 */
@Data
@Schema(title = "WB 财务报表明细分页数据")
public class WbReportDetailPageVO {

	private Long id;

	@Schema(title = "店铺ID")
	private Long shopId;

	@Schema(title = "报表周期类型")
	private String periodType;

	@Schema(title = "报表ID")
	private Long realizationreportId;

	@Schema(title = "报表开始日期")
	private LocalDate dateFrom;

	@Schema(title = "报表结束日期")
	private LocalDate dateTo;

	@Schema(title = "记录生成日期")
	private LocalDate createDt;

	@Schema(title = "币种名称")
	private String currencyName;

	@Schema(title = "合同编码")
	private String suppliercontractCode;

	@Schema(title = "行ID")
	private Long rrdId;

	@Schema(title = "物流收货ID")
	private Long giId;

	@Schema(title = "配送价格")
	private BigDecimal dlvPrc;

	@Schema(title = "固定费率开始日期")
	private LocalDate fixTariffDateFrom;

	@Schema(title = "固定费率结束日期")
	private LocalDate fixTariffDateTo;

	@Schema(title = "品类名称")
	private String subjectName;

	@Schema(title = "商品ID")
	private Long nmId;

	@Schema(title = "品牌")
	private String brandName;

	@Schema(title = "品类组")
	private String saName;

	@Schema(title = "尺寸")
	private String tsName;

	@Schema(title = "条码")
	private String barcode;

	@Schema(title = "操作类型")
	private String docTypeName;

	@Schema(title = "数量")
	private Integer quantity;

	@Schema(title = "零售价")
	private BigDecimal retailPrice;

	@Schema(title = "零售额")
	private BigDecimal retailAmount;

	@Schema(title = "折扣百分比")
	private BigDecimal salePercent;

	@Schema(title = "佣金百分比")
	private BigDecimal commissionPercent;

	@Schema(title = "仓库名称")
	private String officeName;

	@Schema(title = "供应商操作名称")
	private String supplierOperName;

	@Schema(title = "订单时间")
	private LocalDateTime orderDt;

	@Schema(title = "销售时间")
	private LocalDateTime saleDt;

	@Schema(title = "报表日期")
	private LocalDate rrDt;

	@Schema(title = "货品ID")
	private Long shkId;

	@Schema(title = "折后价(RUB)")
	private BigDecimal retailPriceWithdiscRub;

	@Schema(title = "配送金额")
	private BigDecimal deliveryAmount;

	@Schema(title = "退货金额")
	private BigDecimal returnAmount;

	@Schema(title = "配送金额(RUB)")
	private BigDecimal deliveryRub;

	@Schema(title = "物流箱型")
	private String giBoxTypeName;

	@Schema(title = "产品折扣")
	private BigDecimal productDiscountForReport;

	@Schema(title = "供应商促销金额")
	private BigDecimal supplierPromo;

	@Schema(title = "SPP百分比")
	private BigDecimal ppvzSppPrc;

	@Schema(title = "基础KWV百分比")
	private BigDecimal ppvzKvwPrcBase;

	@Schema(title = "KWV百分比")
	private BigDecimal ppvzKvwPrc;

	@Schema(title = "评级加成百分比")
	private BigDecimal supRatingPrcUp;

	@Schema(title = "是否KGV2模式")
	private Boolean isKgvpV2;

	@Schema(title = "销售佣金")
	private BigDecimal ppvzSalesCommission;

	@Schema(title = "应付金额")
	private BigDecimal ppvzForPay;

	@Schema(title = "奖励金额")
	private BigDecimal ppvzReward;

	@Schema(title = "收单手续费")
	private BigDecimal acquiringFee;

	@Schema(title = "收单费百分比")
	private BigDecimal acquiringPercent;

	@Schema(title = "支付处理方式")
	private String paymentProcessing;

	@Schema(title = "收单银行")
	private String acquiringBank;

	@Schema(title = "VW金额")
	private BigDecimal ppvzVw;

	@Schema(title = "VW金额(含税)")
	private BigDecimal ppvzVwNds;

	@Schema(title = "仓库名称(结算)")
	private String ppvzOfficeName;

	@Schema(title = "仓库ID(结算)")
	private Long ppvzOfficeId;

	@Schema(title = "供应商ID")
	private Long ppvzSupplierId;

	@Schema(title = "供应商名称")
	private String ppvzSupplierName;

	@Schema(title = "供应商税号")
	private String ppvzInn;

	@Schema(title = "报关单号")
	private String declarationNumber;

	@Schema(title = "奖励类型")
	private String bonusTypeName;

	@Schema(title = "贴纸ID")
	private String stickerId;

	@Schema(title = "站点国家")
	private String siteCountry;

	@Schema(title = "是否DBS订单")
	private Boolean srvDbs;

	@Schema(title = "罚款金额")
	private BigDecimal penalty;

	@Schema(title = "附加支付")
	private BigDecimal additionalPayment;

	@Schema(title = "物流再计费成本")
	private BigDecimal rebillLogisticCost;

	@Schema(title = "物流再计费组织")
	private String rebillLogisticOrg;

	@Schema(title = "仓储费")
	private BigDecimal storageFee;

	@Schema(title = "扣款")
	private BigDecimal deduction;

	@Schema(title = "验收入库")
	private BigDecimal acceptance;

	@Schema(title = "装配单ID")
	private Long assemblyId;

	@Schema(title = "KIZ码")
	private String kiz;

	@Schema(title = "唯一订单行ID")
	private String srid;

	@Schema(title = "报表类型")
	private String reportType;

	@Schema(title = "是否法人主体")
	private Boolean isLegalEntity;

	@Schema(title = "TRBX ID")
	private String trbxId;

	@Schema(title = "分期共同融资金额")
	private BigDecimal installmentCofinancingAmount;

	@Schema(title = "WB折扣百分比")
	private BigDecimal wibesWbDiscountPercent;

	@Schema(title = "返现金额")
	private BigDecimal cashbackAmount;

	@Schema(title = "返现折扣")
	private BigDecimal cashbackDiscount;

	@Schema(title = "返现佣金变化")
	private BigDecimal cashbackCommissionChange;

	@Schema(title = "订单UID")
	private String orderUid;

	@Schema(title = "支付计划")
	private String paymentSchedule;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
