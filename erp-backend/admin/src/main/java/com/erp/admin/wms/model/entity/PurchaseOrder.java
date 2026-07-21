package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购单实体
 *
 * @author erp
 */
@Data
@TableName("wms_purchase_order")
@Schema(title = "采购单实体")
public class PurchaseOrder {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "归属货主 erp_tenant_id（数据级隔离，新建时自动盖章）")
	private Long erpTenantId;

	@Schema(title = "采购单号（合同编号）")
	private String orderNo;

	@Schema(title = "供应商ID")
	private Long supplierId;

	@Schema(title = "下单日期")
	private LocalDate orderDate;

	@Schema(title = "预计交货日期")
	private LocalDate expectedDeliveryDate;

	@Schema(title = "实际交货日期")
	private LocalDate actualDeliveryDate;

	@Schema(title = "合同总金额")
	private BigDecimal totalAmount;

	@Schema(title = "币种")
	private String currencyCode;

	@Schema(title = "是否含税: 1-含税 / 0-不含税")
	private Integer taxIncluded;

	@Schema(title = "首付款比例")
	private BigDecimal prepayRatio;

	@Schema(title = "首付款金额")
	private BigDecimal prepayAmount;

	@Schema(title = "尾款账期(天)")
	private Integer balancePaymentDays;

	@Schema(title = "首付款状态: 0-未付 / 1-已付")
	private Integer prepayStatus;

	@Schema(title = "首付款时间")
	private LocalDateTime prepayTime;

	@Schema(title = "尾款状态: 0-未付 / 1-已付")
	private Integer balanceStatus;

	@Schema(title = "尾款时间")
	private LocalDateTime balancePayTime;

	@Schema(title = "采购单状态")
	private String orderStatus;

	@Schema(title = "发货状态")
	private String shippingStatus;

	@Schema(title = "入库状态")
	private String receivingStatus;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "创建人")
	private Long createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	@Schema(title = "更新人")
	private Long updateBy;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@Schema(title = "逻辑删除标识")
	private Long deleted;

}
