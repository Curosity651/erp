package com.erp.admin.wms.service;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.tenant.model.entity.SysTenant;
import com.erp.admin.wms.converter.StocktakeItemConverter;
import com.erp.admin.wms.mapper.StocktakeItemMapper;
import com.erp.admin.wms.model.entity.StocktakeOrderItem;
import com.erp.admin.wms.model.enums.StocktakeItemStatus;
import com.erp.admin.wms.model.vo.StocktakeItemVO;
import com.erp.admin.wms.model.vo.StocktakeProgressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 盘点单明细服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StocktakeItemService extends ExtendServiceImpl<StocktakeItemMapper, StocktakeOrderItem> {

	private final SkuBriefService skuBriefService;

	private final SysTenantMapper sysTenantMapper;

	private final WarehouseSkuCodeService warehouseSkuCodeService;

	/**
	 * 根据盘点单ID查询明细列表
	 * @param stocktakeOrderId 盘点单ID
	 * @return 明细列表
	 */
	public List<StocktakeOrderItem> getByStocktakeOrderId(Long stocktakeOrderId) {
		return baseMapper.selectByStocktakeOrderId(stocktakeOrderId);
	}

	public List<StocktakeOrderItem> getByTaskId(Long taskId) {
		return baseMapper.selectByTaskId(taskId);
	}

	public List<StocktakeItemVO> getVoListByTaskId(Long taskId) {
		return convertToVoList(baseMapper.selectByTaskId(taskId));
	}

	/**
	 * 根据盘点单ID查询明细VO列表（含SKU展示信息）
	 * @param stocktakeOrderId 盘点单ID
	 * @return 明细VO列表
	 */
	public List<StocktakeItemVO> getVoListByStocktakeOrderId(Long stocktakeOrderId) {
		List<StocktakeOrderItem> items = baseMapper.selectByStocktakeOrderId(stocktakeOrderId);
		return convertToVoList(items);
	}

	/**
	 * 查询盘点进度统计
	 * @param stocktakeOrderId 盘点单ID
	 * @return 盘点进度
	 */
	public StocktakeProgressVO getProgress(Long stocktakeOrderId) {
		return baseMapper.selectProgress(stocktakeOrderId);
	}

	/**
	 * 查询有差异的明细列表
	 * @param stocktakeOrderId 盘点单ID
	 * @return 有差异的明细列表
	 */
	public List<StocktakeOrderItem> getDiffItems(Long stocktakeOrderId) {
		return baseMapper.selectDiffItems(stocktakeOrderId);
	}

	/**
	 * 批量保存明细
	 * @param stocktakeOrderId 盘点单ID
	 * @param items 明细列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void batchSave(Long stocktakeOrderId, List<StocktakeOrderItem> items) {
		for (StocktakeOrderItem item : items) {
			item.setStocktakeOrderId(stocktakeOrderId);
			item.setStocktakeStatus(StocktakeItemStatus.PENDING.getCode());
		}
		this.saveBatch(items);
		log.info("Batch saved {} items for stocktakeOrderId={}", items.size(), stocktakeOrderId);
	}

	/**
	 * 根据盘点单ID删除明细
	 * @param stocktakeOrderId 盘点单ID
	 * @return 删除数量
	 */
	@Transactional(rollbackFor = Exception.class)
	public int deleteByStocktakeOrderId(Long stocktakeOrderId) {
		int count = baseMapper.deleteByStocktakeOrderId(stocktakeOrderId);
		log.info("Deleted {} items for stocktakeOrderId={}", count, stocktakeOrderId);
		return count;
	}

	/**
	 * 将实体列表转换为VO列表（含SKU展示信息）
	 * @param items 实体列表
	 * @return VO列表
	 */
	public List<StocktakeItemVO> convertToVoList(List<StocktakeOrderItem> items) {
		if (items == null || items.isEmpty()) {
			return new ArrayList<>();
		}
		List<StocktakeItemVO> voList = StocktakeItemConverter.INSTANCE.entityListToVOList(items);
		enrichByOwner(items, voList);
		return voList;
	}

	/**
	 * 按货主分组、在各自租户上下文下填充 SKU 展示信息。
	 * <p>SKU 目录按 tenant_id 隔离，平台跨货主盘点时同一单可含多货主明细；逐组用 {@link TenantContext#runAs}
	 * 切到该明细的 erp_tenant_id 再富化，保证名称/图片可见。对货主自身调用则切回自己（等价无操作）。
	 * <p>依赖 entityListToVOList 与入参 items 一一对应（MapStruct 保序）。
	 * @param items  明细实体（含 erp_tenant_id）
	 * @param voList 对应 VO（顺序与 items 一致）
	 */
	private void enrichByOwner(List<StocktakeOrderItem> items, List<StocktakeItemVO> voList) {
		Map<Long, List<StocktakeItemVO>> byOwner = new LinkedHashMap<>();
		for (int i = 0; i < items.size(); i++) {
			Long owner = items.get(i).getErpTenantId();
			byOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(voList.get(i));
		}
		Map<Long, String> ownerNameMap = loadOwnerNames(byOwner.keySet());
		byOwner.forEach((owner, group) -> {
			String ownerName = owner == null ? null : ownerNameMap.get(owner);
			group.forEach(vo -> vo.setOwnerName(ownerName));
			if (owner != null) {
				group.forEach(vo -> vo.setWarehouseSkuCode(
						warehouseSkuCodeService.build(owner, vo.getSkuCode())));
				TenantContext.runAs(owner, () -> {
					skuBriefService.enrichForQuery(group, StocktakeItemVO::getSkuCode, StocktakeItemVO::setSkuBrief);
					return null;
				});
			}
			else {
				skuBriefService.enrichForQuery(group, StocktakeItemVO::getSkuCode, StocktakeItemVO::setSkuBrief);
			}
		});
	}

	/** 批量查货主ID → 货主名称。 */
	private Map<Long, String> loadOwnerNames(Collection<Long> ownerIds) {
		List<Long> ids = ownerIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
		if (ids.isEmpty()) {
			return Collections.emptyMap();
		}
		return sysTenantMapper.selectBatchIds(ids)
				.stream()
				.collect(Collectors.toMap(SysTenant::getId, SysTenant::getTenantName, (a, b) -> a));
	}

}
