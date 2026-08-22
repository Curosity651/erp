package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调整单分页视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "调整单分页视图对象")
public class AdjustmentPageVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "调整单号")
	private String adjustmentNo;

	@Schema(title = "调整类型")
	private String adjustmentType;

	@Schema(title = "调整仓库ID")
	private Long warehouseId;

	@Schema(title = "调整仓库名称")
	private String warehouseName;

	@Schema(title = "货主ID")
	private Long erpTenantId;

	@Schema(title = "货主名称")
	private String ownerName;

	@Schema(title = "所属WMS服务商ID")
	private Long wmsTenantId;

	@Schema(title = "所属WMS服务商名称")
	private String operatorName;

	@Schema(title = "调整日期")
	private LocalDate adjustmentDate;

	@Schema(title = "状态: PENDING_OWNER-待货主确认 / PENDING_DESTROY-待仓库销毁 / SCRAPPED-已销毁 / REJECTED-已驳回 / CANCELLED-已取消")
	private String orderStatus;

	@Schema(title = "调整原因")
	private String adjustmentReason;

	@Schema(title = "货主驳回原因")
	private String rejectReason;

	@Schema(title = "SKU数量")
	private Integer skuCount;

	@Schema(title = "调整总数量")
	private Integer totalQuantity;

	@Schema(title = "确认时间")
	private LocalDateTime confirmTime;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

}
