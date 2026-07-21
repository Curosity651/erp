package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 调拨单详情视图对象
 *
 * @author erp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "调拨单详情视图对象")
public class TransferOrderDetailVO extends TransferOrderPageVO {

	@Schema(title = "备注")
	private String remark;

	@Schema(title = "调拨明细列表")
	private List<TransferOrderItemVO> items;

}
