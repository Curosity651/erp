package com.erp.admin.wms.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.SkuService;
import com.erp.admin.wms.mapper.OwnerInventoryQueryMapper;
import com.erp.admin.wms.model.dto.RegionSkuStockDTO;
import com.erp.admin.wms.model.dto.WarehouseAggregateDTO;
import com.erp.admin.wms.model.qo.InventoryQO;
import com.erp.admin.wms.model.vo.InventoryDetailVO;
import com.erp.admin.wms.model.vo.InventoryPageVO;
import com.erp.admin.wms.model.vo.InventorySummaryVO;
import com.erp.admin.wms.model.vo.SkuSummaryVO;
import com.erp.admin.wms.model.vo.WarehouseSummaryVO;
import com.erp.admin.wms.util.VolumeCalculator;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class OwnerInventoryQueryService {

	private final OwnerInventoryQueryMapper mapper;
	private final ErpOwnerScopeService ownerScopeService;
	private final WarehouseService warehouseService;
	private final SkuBriefService skuBriefService;
	private final SkuService skuService;

	public InventorySummaryVO getSummary() {
		InventorySummaryVO result = mapper.selectSummary(scope(), unrestricted());
		return result == null ? new InventorySummaryVO() : result;
	}

	public List<WarehouseSummaryVO> getSummaryByWarehouse(String ignoredWarehouseType) {
		List<WarehouseSummaryVO> rows = mapper.selectWarehouseSummary(scope(), unrestricted());
		warehouseService.enrichWarehouseDisplay(rows, WarehouseSummaryVO::getWarehouseId,
				WarehouseSummaryVO::setWarehouseDisplay);
		return rows;
	}

	public PageResult<SkuSummaryVO> getSummaryBySku(PageParam pageParam, String keyword, String stockStatus) {
		IPage<SkuSummaryVO> page = mapper.selectSkuSummary(PageUtil.prodPage(pageParam), scope(), unrestricted(),
				keyword, stockStatus);
		List<SkuSummaryVO> rows = page.getRecords();
		skuBriefService.enrichForQuery(rows, SkuSummaryVO::getSkuCode, SkuSummaryVO::setSkuBrief);
		Set<String> codes = rows.stream().map(SkuSummaryVO::getSkuCode).collect(Collectors.toSet());
		Map<String, Sku> skuMap = codes.isEmpty() ? Collections.emptyMap() : skuService.getSkuMapByCodes(codes);
		for (SkuSummaryVO row : rows) {
			BigDecimal unit = VolumeCalculator.calculateUnitVolume(skuMap.get(row.getSkuCode()));
			row.setUnitVolume(unit);
			if (unit != null) row.setTotalVolume(VolumeCalculator.calculateTotalVolume(unit, row.getWarehouseQuantity()));
		}
		return new PageResult<>(rows, page.getTotal());
	}

	public PageResult<InventoryPageVO> queryPage(PageParam pageParam, InventoryQO qo) {
		IPage<InventoryPageVO> page = mapper.selectLocationPage(PageUtil.prodPage(pageParam), scope(), unrestricted(), qo);
		List<InventoryPageVO> rows = page.getRecords();
		warehouseService.enrichWarehouseDisplay(rows, InventoryPageVO::getWarehouseId,
				InventoryPageVO::setWarehouseDisplay);
		skuBriefService.enrichForQuery(rows, InventoryPageVO::getSkuCode, InventoryPageVO::setSkuBrief);
		return new PageResult<>(rows, page.getTotal());
	}

	public InventoryDetailVO getDetail(Long warehouseId, String skuCode) {
		InventoryDetailVO detail = mapper.selectDetail(scope(), unrestricted(), warehouseId, skuCode);
		Assert.notNull(detail, "库存记录不存在");
		warehouseService.enrichWarehouseDisplay(Collections.singletonList(detail), InventoryDetailVO::getWarehouseId,
				InventoryDetailVO::setWarehouseDisplay);
		skuBriefService.enrichForQuery(Collections.singletonList(detail), InventoryDetailVO::getSkuCode,
				InventoryDetailVO::setSkuBrief);
		detail.setRecentFlows(Collections.emptyList());
		return detail;
	}

	public List<RegionSkuStockDTO> getRegionSkuStocks(Collection<Long> regionIds, String keyword) {
		return mapper.selectRegionSkuStocks(scope(), unrestricted(), regionIds, keyword);
	}

	public List<WarehouseAggregateDTO> getRegionAggregates(Collection<Long> regionIds) {
		return mapper.selectRegionAggregates(scope(), unrestricted(), regionIds);
	}

	private List<Long> scope() {
		List<Long> result = ownerScopeService.readScope();
		return result == null ? Collections.emptyList() : result;
	}

	private boolean unrestricted() {
		return ownerScopeService.readScope() == null;
	}
}
