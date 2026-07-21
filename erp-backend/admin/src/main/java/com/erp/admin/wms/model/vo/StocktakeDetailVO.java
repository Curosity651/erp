package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 盘点单详情视图对象
 *
 * @author erp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "盘点单详情视图对象")
public class StocktakeDetailVO extends StocktakePageVO {

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "盘点明细列表")
	private List<StocktakeItemVO> items;

}
