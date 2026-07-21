package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 库位调整单查询对象。
 *
 * @author erp
 */
@Data
@Schema(title = "库位调整单查询对象")
public class LocationTransferQO {

	@Schema(title = "调整单号")
	private String transferNo;

	@Schema(title = "仓库ID")
	private Long warehouseId;

	@Schema(title = "货主ID")
	private Long erpTenantId;

	@Schema(title = "所属WMS服务商ID（平台端可按服务商维度筛选）")
	private Long wmsTenantId;

	@Schema(title = "状态: PENDING / COMPLETED / CANCELLED")
	private String orderStatus;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "创建日期起始")
	private LocalDate createTimeStart;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "创建日期结束")
	private LocalDate createTimeEnd;

}
