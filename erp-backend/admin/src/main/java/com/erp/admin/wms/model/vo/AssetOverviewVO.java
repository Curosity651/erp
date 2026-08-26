package com.erp.admin.wms.model.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/** Owner inventory asset overview, separated from accounts payable. */
@Data
@Builder
public class AssetOverviewVO {

	private List<AssetFinanceOverviewVO.AssetCurrencyVO> assets;

	private Integer unvaluedSkuCount;

	private Integer unvaluedQuantity;

	private Integer totalHeldQuantity;

	private List<AssetFinanceOverviewVO.HoldingPositionVO> holdingPositions;

	private LocalDateTime fboLastSyncedAt;

	private Boolean fboStale;

}
