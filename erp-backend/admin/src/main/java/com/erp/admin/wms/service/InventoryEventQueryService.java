package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.mapper.InventoryEventQueryMapper;
import com.erp.admin.wms.model.qo.StockFlowQO;
import com.erp.admin.wms.model.qo.StockPostingItemQO;
import com.erp.admin.wms.model.qo.StockPostingQO;
import com.erp.admin.wms.model.vo.StockFlowDetailVO;
import com.erp.admin.wms.model.vo.StockFlowPageVO;
import com.erp.admin.wms.model.vo.StockFlowTodaySummaryVO;
import com.erp.admin.wms.model.vo.StockFlowTrendVO;
import com.erp.admin.wms.model.vo.StockPostingDetailVO;
import com.erp.admin.wms.model.vo.StockPostingItemVO;
import com.erp.admin.wms.model.vo.StockPostingPageVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryEventQueryService {
	private final InventoryEventQueryMapper mapper;
	private final ErpOwnerScopeService ownerScopeService;
	private final WarehouseService warehouseService;
	private final SkuBriefService skuBriefService;

	public PageResult<StockFlowPageVO> queryFlowPage(PageParam pageParam, StockFlowQO qo) {
		qo.setErpTenantIds(ownerScopeService.readScope());
		if (qo.getRegionId() != null) {
			qo.setRegionWarehouseIds(warehouseService.getIdsByRegionId(qo.getRegionId()));
		}
		IPage<StockFlowPageVO> page = mapper.queryFlowPage(PageUtil.prodPage(pageParam), qo);
		enrichFlows(page.getRecords());
		return new PageResult<>(page.getRecords(), page.getTotal());
	}

	public StockFlowDetailVO getFlowDetail(Long id) {
		StockFlowDetailVO detail = mapper.selectFlowDetail(id, ownerScopeService.readScope());
		if (detail != null) {
			warehouseService.enrichWarehouseDisplay(Collections.singletonList(detail),
					StockFlowDetailVO::getWarehouseId, StockFlowDetailVO::setWarehouseDisplay);
			skuBriefService.enrichForQuery(Collections.singletonList(detail),
					StockFlowDetailVO::getSkuCode, StockFlowDetailVO::setSkuBrief);
		}
		return detail;
	}

	public List<String> listEventTypes(Long warehouseId, String skuCode) {
		return mapper.listEventTypes(warehouseId, skuCode, ownerScopeService.readScope());
	}

	public StockFlowTodaySummaryVO getTodaySummary(Long warehouseId) {
		return mapper.selectTodaySummary(warehouseId, ownerScopeService.readScope());
	}

	public List<StockFlowTrendVO> getTrend(Integer days, Long warehouseId) {
		return mapper.selectTrend(days, warehouseId, ownerScopeService.readScope());
	}

	public PageResult<StockPostingPageVO> queryEventPage(PageParam pageParam, StockPostingQO qo) {
		qo.setErpTenantIds(ownerScopeService.readScope());
		IPage<StockPostingPageVO> page = mapper.queryEventPage(PageUtil.prodPage(pageParam), qo);
		warehouseService.enrichWarehouseDisplay(page.getRecords(), StockPostingPageVO::getWarehouseId,
				StockPostingPageVO::setWarehouseDisplay);
		page.getRecords().forEach(item -> item.setPostingTypeDesc(eventTypeDescription(item.getPostingType())));
		return new PageResult<>(page.getRecords(), page.getTotal());
	}

	public StockPostingDetailVO getEventDetail(Long id) {
		List<Long> scope = ownerScopeService.readScope();
		StockPostingDetailVO detail = mapper.selectEventDetail(id, scope);
		Assert.notNull(detail, "库存操作记录不存在");
		detail.setPostingTypeDesc(eventTypeDescription(detail.getPostingType()));
		detail.setIoDirection(ioDirection(detail.getPostingType()));
		detail.setWarehouseDisplay(warehouseService.getDisplay(detail.getWarehouseId()));
		detail.setItems(mapper.selectEventItems(id, null, scope));
		enrichItems(detail.getItems());
		return detail;
	}

	public PageResult<StockPostingItemVO> queryEventItemPage(PageParam pageParam, StockPostingItemQO qo) {
		qo.setErpTenantIds(ownerScopeService.readScope());
		IPage<StockPostingItemVO> page = mapper.queryEventItemPage(PageUtil.prodPage(pageParam), qo);
		enrichItems(page.getRecords());
		return new PageResult<>(page.getRecords(), page.getTotal());
	}

	private void enrichFlows(List<StockFlowPageVO> records) {
		warehouseService.enrichWarehouseDisplay(records, StockFlowPageVO::getWarehouseId,
				StockFlowPageVO::setWarehouseDisplay);
		skuBriefService.enrichForQuery(records, StockFlowPageVO::getSkuCode, StockFlowPageVO::setSkuBrief);
	}

	private void enrichItems(List<StockPostingItemVO> records) {
		warehouseService.enrichWarehouseDisplay(records, StockPostingItemVO::getWarehouseId,
				StockPostingItemVO::setWarehouseDisplay);
		skuBriefService.enrichForQuery(records, StockPostingItemVO::getSkuCode, StockPostingItemVO::setSkuBrief);
	}

	private String ioDirection(String type) {
		if ("INBOUND_PUTAWAY".equals(type) || "RETURN_PUTAWAY".equals(type)
				|| "STOCKTAKE_GAIN".equals(type)) {
			return "IN";
		}
		if ("SHIP".equals(type) || "DECREASE".equals(type) || "STOCKTAKE_LOSS".equals(type)
				|| "SCRAP".equals(type)) {
			return "OUT";
		}
		return "INTERNAL";
	}

	private String eventTypeDescription(String type) {
		if (type == null) return null;
		switch (type) {
			case "INBOUND_PUTAWAY": return "入库上架";
			case "RETURN_PUTAWAY": return "退货上架";
			case "DECREASE": return "库存扣减";
			case "RESERVE": return "销售预占";
			case "RELEASE": return "释放预占";
			case "SHIP": return "出库签出";
			case "MOVE": return "库位调整";
			case "STOCKTAKE_GAIN": return "盘点盘盈";
			case "STOCKTAKE_LOSS": return "盘点盘亏";
			case "SCRAP_RESERVE": return "报废预留";
			case "SCRAP_RELEASE": return "取消报废预留";
			case "SCRAP": return "报废出库";
			default: return type;
		}
	}
}
