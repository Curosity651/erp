package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 平台收货 DTO（D2.4）：录入各 SKU 实收数量。
 *
 * @author erp
 */
@Data
@Schema(title = "平台收货DTO")
public class InboundReceiveDTO {

	@NotNull(message = "入库执行单ID不能为空")
	@Schema(title = "WMS入库执行单ID")
	private Long inboundOrderId;

	@Valid
	@NotEmpty(message = "收货明细不能为空")
	@Schema(title = "收货明细")
	private List<ReceiveItem> items;

	@NotEmpty(message = "收货现场照片不能为空")
	@Schema(title = "收货现场照片文件ID")
	private List<Long> evidenceFileIds;

	@Data
	@Schema(title = "收货明细项")
	public static class ReceiveItem {

		@NotNull(message = "Inbound item ID is required")
		@Schema(title = "Inbound item ID")
		private Long inboundOrderItemId;

		@NotBlank(message = "SKU编码不能为空")
		@Schema(title = "SKU编码")
		private String skuCode;

		@NotNull(message = "实收数量不能为空")
		@Min(value = 0, message = "实收数量不能为负")
		@Schema(title = "实收数量")
		private Integer actualQuantity;

	}

}
