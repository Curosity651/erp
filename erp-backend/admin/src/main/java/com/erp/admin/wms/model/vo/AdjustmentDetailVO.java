package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 调整单详情视图对象
 *
 * @author erp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "调整单详情视图对象")
public class AdjustmentDetailVO extends AdjustmentPageVO {

	@Schema(title = "调整明细列表")
	private List<AdjustmentItemVO> items;

}
