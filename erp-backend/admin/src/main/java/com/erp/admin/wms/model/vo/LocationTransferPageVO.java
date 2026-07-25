package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库位调整单分页视图对象。
 *
 * @author erp
 */
@Data
@Schema(title = "库位调整单分页视图对象")
public class LocationTransferPageVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "调整单号")
	private String transferNo;

	@Schema(title = "仓库ID")
	private Long warehouseId;

	@Schema(title = "仓库名称")
	private String warehouseName;

	@Schema(title = "货主ID")
	private Long erpTenantId;

	@Schema(title = "货主名称")
	private String ownerName;

	@Schema(title = "所属WMS服务商ID")
	private Long wmsTenantId;

	@Schema(title = "所属WMS服务商名称")
	private String operatorName;

	@Schema(title = "状态: PENDING-待调整 / COMPLETED-已完成 / CANCELLED-已取消")
	private String orderStatus;

	@Schema(title = "来源类型")
	private String sourceType;

	@Schema(title = "来源业务ID")
	private Long sourceId;

	@Schema(title = "来源单号")
	private String sourceNo;

	@Schema(title = "调整原因编码")
	private String reasonCode;

	@Schema(title = "调整原因")
	private String reason;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "明细行数")
	private Integer itemCount;

	@Schema(title = "移动总数量")
	private Integer totalQuantity;

	@Schema(title = "调整完成时间")
	private LocalDateTime completeTime;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

}
