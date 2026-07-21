package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调拨单分页视图对象
 *
 * @author erp
 */
@Data
@Schema(title = "调拨单分页视图对象")
public class TransferOrderPageVO {

	@Schema(title = "主键ID")
	private Long id;

	@Schema(title = "调拨单号")
	private String transferNo;

	@Schema(title = "调拨类型: NORMAL-普通调拨 / FBO_INBOUND-FBO入库")
	private String transferType;

	@Schema(title = "源仓库ID")
	private Long fromWarehouseId;

	@Schema(title = "源仓库名称")
	private String fromWarehouseName;

	@Schema(title = "目标仓库ID")
	private Long toWarehouseId;

	@Schema(title = "目标仓库名称")
	private String toWarehouseName;

	@Schema(title = "状态: DRAFT-草稿 / IN_TRANSIT-在途 / COMPLETED-已入库 / CANCELLED-已取消")
	private String orderStatus;

	@Schema(title = "出库时间")
	private LocalDateTime shipTime;

	@Schema(title = "结束时间（完成/取消/撤回）")
	private LocalDateTime endTime;

	@Schema(title = "SKU数量")
	private Integer skuCount;

	@Schema(title = "调拨总数量")
	private Integer totalQuantity;

	@Schema(title = "创建时间")
	private LocalDateTime createTime;

}
