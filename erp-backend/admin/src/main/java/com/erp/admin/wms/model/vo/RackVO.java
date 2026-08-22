package com.erp.admin.wms.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 货架（排）预览视图（C2，《完整技术设计方案》§7.4）。
 *
 * <p>C2 暂不含库存占用量(occupiedQty 等)，留 D 阶段接 wms_physical_inventory 后补。
 *
 * @author erp
 */
@Data
@Schema(title = "货架预览")
public class RackVO {

	@Schema(title = "仓库ID")
	private Long warehouseId;

	@Schema(title = "排号")
	private String rackNo;

	@Schema(title = "状态 IDLE空闲/RESERVED合同预留/OCCUPIED占用")
	private String status;

	@Schema(title = "分配记录ID")
	private Long assignmentId;

	@Schema(title = "分配给的WMS服务商ID")
	private Long assignedWmsTenantId;

	@Schema(title = "分配给的WMS服务商名称")
	private String assignedWmsTenantName;

	@Schema(title = "是否由服务合同控制")
	private Boolean contractControlled;

	@Schema(title = "服务合同ID")
	private Long contractId;

	@Schema(title = "服务合同编号")
	private String contractNo;

	@Schema(title = "月租金")
	private BigDecimal monthlyFee;

	@Schema(title = "生效日期")
	private LocalDate effectiveFrom;

	@Schema(title = "结束日期")
	private LocalDate effectiveTo;

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "是否即将到期(≤30天)")
	private Boolean expiringSoon;

	@Schema(title = "该排总库位数")
	private Integer locationCount;

}
