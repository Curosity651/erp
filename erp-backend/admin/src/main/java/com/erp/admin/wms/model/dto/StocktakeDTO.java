package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.ballcat.common.core.validation.group.CreateGroup;
import org.ballcat.common.core.validation.group.UpdateGroup;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 盘点单数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "盘点单数据传输对象")
public class StocktakeDTO {

	@Null(groups = CreateGroup.class, message = "新建时ID必须为空")
	@NotNull(groups = UpdateGroup.class, message = "编辑时ID不能为空")
	@Schema(title = "主键ID")
	private Long id;

	@NotNull(message = "盘点仓库不能为空")
	@Schema(title = "盘点仓库ID")
	private Long warehouseId;

	@NotNull(message = "盘点日期不能为空")
	@Schema(title = "盘点日期")
	private LocalDate stocktakeDate;

	@NotBlank(message = "盘点范围不能为空")
	@Schema(title = "盘点范围: ALL-全部SKU / PARTIAL-指定SKU")
	private String stocktakeScope;

	@NotBlank(message = "盘点模式不能为空")
	@Schema(title = "盘点模式: FULL/CYCLE/SPECIAL")
	private String stocktakeMode;

	@Schema(title = "循环盘点选中的库位ID")
	private List<Long> locationIds;

	@Schema(title = "专项盘点SKU")
	private List<String> specialSkuCodes;

	@Schema(title = "专项盘点货主")
	private Long specialOwnerId;

	@Schema(title = "专项盘点是否全仓寻货")
	private Boolean specialSearchAll;

	@Schema(title = "是否为虚拟库位专项盘点")
	private Boolean virtualLocationOnly;

	@Schema(title = "是否盲盘")
	private Boolean blindCount;

	@Size(max = 500, message = "备注长度不能超过500")
	@Schema(title = "备注")
	private String remark;

}
