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
 * 平台上架 DTO（D2.4）：把已收货数量分配到库位（按 SKU×库位×品质 一行）。
 *
 * <p>需覆盖各 SKU 的全部收货数量（一次上架完成）；同 SKU×库位×品质会在服务层合并。
 *
 * @author erp
 */
@Data
@Schema(title = "平台上架DTO")
public class InboundPutawayDTO {

	@NotNull(message = "入库执行单ID不能为空")
	@Schema(title = "WMS入库执行单ID")
	private Long inboundOrderId;

	@Valid
	@NotEmpty(message = "上架明细不能为空")
	@Schema(title = "上架分配明细")
	private List<PutawayLine> lines;

	@Data
	@Schema(title = "上架分配行")
	public static class PutawayLine {

		@NotBlank(message = "SKU编码不能为空")
		@Schema(title = "SKU编码")
		private String skuCode;

		@NotBlank(message = "库位编码不能为空")
		@Schema(title = "库位编码")
		private String locationCode;

		@NotNull(message = "上架数量不能为空")
		@Min(value = 1, message = "上架数量必须大于0")
		@Schema(title = "上架数量")
		private Integer quantity;

		@Schema(title = "品质: GOOD良品 / DAMAGED次品（默认GOOD）")
		private String quality;

		@Schema(title = "分区ID（可选）")
		private Long zoneId;

	}

}
