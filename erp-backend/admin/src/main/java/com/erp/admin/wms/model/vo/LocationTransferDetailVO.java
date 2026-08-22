package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 库位调整单详情视图对象。
 *
 * @author erp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "库位调整单详情视图对象")
public class LocationTransferDetailVO extends LocationTransferPageVO {

	@Schema(title = "明细列表")
	private List<LocationTransferItemVO> items;

	@Schema(title = "调整完成后需要更新标签的托盘")
	private List<PalletSummaryVO> printablePallets;

}
