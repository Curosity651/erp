package com.erp.admin.wms.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;

/**
 * 货架分配请求（C2）。可一次给多排分配（每排一条记录）。
 *
 * @author erp
 */
@Data
@Schema(title = "货架分配请求")
public class RackAssignDTO {

	@NotNull(message = "WMS服务商不能为空")
	@Schema(title = "被分配的 WMS 服务商 tenant_id")
	private Long wmsTenantId;

	@NotNull(message = "仓库不能为空")
	@Schema(title = "仓库ID")
	private Long warehouseId;

	@NotEmpty(message = "排号不能为空")
	@Schema(title = "货架排号列表(可多选)")
	private List<String> rackNos;

	@NotNull(message = "月租金不能为空")
	@DecimalMin(value = "0.00", message = "月租金不能小于0")
	@Schema(title = "月租金(CNY)")
	private BigDecimal monthlyFee;

	@NotNull(message = "生效日期不能为空")
	@Schema(title = "生效日期")
	private LocalDate effectiveFrom;

	@Schema(title = "结束日期(可空=当前有效)")
	private LocalDate effectiveTo;

	@Schema(title = "合同附件URL")
	private String contractFileUrl;

	@Schema(title = "备注")
	private String remark;

}
