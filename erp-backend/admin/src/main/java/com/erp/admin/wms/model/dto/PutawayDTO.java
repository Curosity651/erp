package com.erp.admin.wms.model.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 上架批次写入请求（D1 方案②）。D2 收货上架会真正调用，本期亦作演示入口。
 *
 * @author erp
 */
@Data
@Schema(title = "上架批次写入请求")
public class PutawayDTO {

	@Schema(title = "货架归属(WMS服务商)，无则0")
	private Long wmsTenantId;

	@Schema(title = "货物归属(货主)", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long erpTenantId;

	@Schema(title = "仓库ID(自有仓)", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long warehouseId;

	@Schema(title = "SKU编码", requiredMode = Schema.RequiredMode.REQUIRED)
	private String skuCode;

	@Schema(title = "入库明细ID(批次溯源)，无则0")
	private Long inboundItemId;

	@Schema(title = "上架日期，缺省=今天(UTC)")
	private LocalDate inboundDate;

	@Schema(title = "上架数量", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer quantity;

	@Schema(title = "品质 GOOD/DAMAGED，缺省 GOOD")
	private String quality;

	@Schema(title = "库位编码", requiredMode = Schema.RequiredMode.REQUIRED)
	private String locationCode;

	@Schema(title = "分区ID")
	private Long zoneId;

	@Schema(title = "是否可分配(标准/退货区=1)，缺省 1")
	private Integer allocatable;

}
