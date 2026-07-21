package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 调整单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "调整单查询对象")
public class AdjustmentQO {

	@Schema(title = "调整单号")
	private String adjustmentNo;

	@Schema(title = "调整仓库ID")
	private Long warehouseId;

	@Schema(title = "货主ID（货主端由服务端按身份强制注入，平台端可选）")
	private Long erpTenantId;

	@Schema(title = "所属WMS服务商ID（平台端可按服务商维度筛选）")
	private Long wmsTenantId;

	@Schema(title = "状态: PENDING_OWNER / SCRAPPED / REJECTED / CANCELLED")
	private String orderStatus;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "调整日期起始")
	private LocalDate adjustmentDateStart;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "调整日期结束")
	private LocalDate adjustmentDateEnd;

}
