package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.ballcat.common.core.validation.group.CreateGroup;
import org.ballcat.common.core.validation.group.UpdateGroup;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 调拨单数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "调拨单数据传输对象")
public class TransferOrderDTO {

	@Null(groups = CreateGroup.class, message = "新建时ID必须为空")
	@NotNull(groups = UpdateGroup.class, message = "编辑时ID不能为空")
	@Schema(title = "主键ID")
	private Long id;

	@NotBlank(message = "调拨类型不能为空")
	@Schema(title = "调拨类型: NORMAL-普通调拨（系统唯一类型）")
	private String transferType;

	@NotNull(message = "源仓库不能为空")
	@Schema(title = "源仓库ID")
	private Long fromWarehouseId;

	@NotNull(message = "目标仓库不能为空")
	@Schema(title = "目标仓库ID")
	private Long toWarehouseId;

	@NotNull(groups = { CreateGroup.class, UpdateGroup.class }, message = "货主不能为空")
	@Schema(title = "货主（货物归属），整单归属一个货主")
	private Long erpTenantId;

	@Size(max = 500, message = "备注长度不能超过500")
	@Schema(title = "备注")
	private String remark;

	@Valid
	@NotEmpty(message = "调拨明细不能为空")
	@Schema(title = "调拨明细列表")
	private List<TransferOrderItemDTO> items;

	@Schema(title = "是否确认出库（新建时使用）")
	private Boolean confirmShip;

}
