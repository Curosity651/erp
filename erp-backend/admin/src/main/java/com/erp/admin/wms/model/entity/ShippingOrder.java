package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 物流单实体
 *
 * @author erp
 */
@Data
@TableName("wms_shipping_order")
@Schema(title = "物流单实体")
public class ShippingOrder {

	@TableId(type = IdType.AUTO)
	@Schema(title = "主键ID")
	private Long id;

	@TableField(fill = FieldFill.INSERT)
	@Schema(title = "归属货主 erp_tenant_id（数据级隔离，新建时自动盖章）")
	private Long erpTenantId;

	@Schema(title = "物流单号")
	private String shippingNo;

	@Schema(title = "物流商ID")
	private Long providerId;

	@Schema(title = "发货日期")
	private LocalDate shippingDate;

	@Schema(title = "物流方式: GRAY-灰关 / WHITE-白关")
	private String shippingMethod;

	@Schema(title = "物流线路: EAST-东线 / WEST-西线 / RAIL-铁路")
	private String shippingRoute;

	@Schema(title = "目标区域ID")
	private Long targetRegionId;

	@Schema(title = "预计运输时效(天)")
	private Integer estimatedDays;

	@Schema(title = "预计到货日期")
	private LocalDate estimatedArrivalDate;

	@Schema(title = "发货件数")
	private Integer packageCount;

	@Schema(title = "总重量(KG)")
	private BigDecimal totalWeight;

	@Schema(title = "物流单价(USD/kg，灰关)")
	private BigDecimal unitPrice;

	@Schema(title = "运输费用(USD，白关)")
	private BigDecimal shippingFee;

	@Schema(title = "杂费(USD，白关)")
	private BigDecimal miscFee;

	@Schema(title = "物流总金额(USD)")
	private BigDecimal totalAmount;

	@Schema(title = "物流总金额(CNY)")
	private BigDecimal totalAmountCny;

	@Schema(title = "付款状态: 0-未付 / 1-已付")
	private Integer paymentStatus;

	@Schema(title = "付款凭证文件ID（关联sys_file表）")
	private Long paymentVoucherFileId;

	@Schema(title = "物流单状态")
	private String shippingStatus;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "关联库存过账单ID（确认发货时生成）")
	private Long stockPostingId;

	@Version
	@Schema(title = "乐观锁版本号")
	private Integer version;

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
