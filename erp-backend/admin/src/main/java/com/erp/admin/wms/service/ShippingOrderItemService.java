package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.converter.ShippingOrderItemConverter;
import com.erp.admin.wms.mapper.ShippingOrderItemMapper;
import com.erp.admin.wms.model.dto.ShippingOrderItemDTO;
import com.erp.admin.wms.model.dto.ShippingAllocationRowDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.ShippingOrderItem;
import com.erp.admin.wms.model.param.ReceivedQuantityUpdateParam;
import com.erp.admin.wms.model.vo.ShippingOrderArrivalStatsVO;
import com.erp.admin.wms.model.vo.ShippingOrderItemVO;
import com.erp.admin.wms.model.vo.ShippingOrderSkuCountVO;
import com.erp.admin.wms.model.vo.ShippingQuantityStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 物流单明细服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingOrderItemService extends ExtendServiceImpl<ShippingOrderItemMapper, ShippingOrderItem> {

	private final SkuBriefService skuBriefService;

	/**
	 * 根据物流单ID查询明细列表
	 * @param shippingOrderId 物流单ID
	 * @return 明细列表
	 */
	public List<ShippingOrderItem> getByShippingOrderId(Long shippingOrderId) {
		return baseMapper.selectByShippingOrderId(shippingOrderId);
	}

	/**
	 * 校验并锁定物流单使用的采购明细容量。
	 * 所有待发货草稿形成逻辑占用；当前物流单自身的占用不重复计算。
	 */
	public void validateAndLockShippingAllocation(Long shippingOrderId, List<ShippingOrderItem> items) {
		if (CollectionUtils.isEmpty(items)) {
			throw new IllegalStateException("物流单明细不能为空");
		}

		Map<Long, Integer> requestedQuantity = new HashMap<>();
		Map<Long, String> skuCodes = new HashMap<>();
		for (ShippingOrderItem item : items) {
			Long purchaseOrderItemId = item.getPurchaseOrderItemId();
			Integer quantity = item.getQuantity();
			if (purchaseOrderItemId == null || quantity == null || quantity <= 0) {
				throw new IllegalStateException("物流单存在无效的采购明细或发货数量");
			}
			requestedQuantity.merge(purchaseOrderItemId, quantity, Integer::sum);
			skuCodes.putIfAbsent(purchaseOrderItemId, item.getSkuCode());
		}

		List<Long> purchaseOrderItemIds = new ArrayList<>(requestedQuantity.keySet());
		Collections.sort(purchaseOrderItemIds);
		List<Long> lockedIds = baseMapper.lockPurchaseOrderItems(purchaseOrderItemIds);
		if (lockedIds.size() != purchaseOrderItemIds.size()) {
			throw new IllegalStateException("物流单关联的采购明细不存在");
		}

		List<ShippingAllocationRowDTO> rows = baseMapper.selectShippingAllocationRows(
				shippingOrderId, purchaseOrderItemIds);
		Map<Long, ShippingAllocationRowDTO> rowMap = rows.stream().collect(Collectors.toMap(
				ShippingAllocationRowDTO::getPurchaseOrderItemId, row -> row));

		for (Long purchaseOrderItemId : purchaseOrderItemIds) {
			ShippingAllocationRowDTO row = rowMap.get(purchaseOrderItemId);
			if (row == null) {
				throw new IllegalStateException("物流单关联的采购明细不存在");
			}
			int requested = requestedQuantity.get(purchaseOrderItemId);
			int total = row.getTotalQuantity() == null ? 0 : row.getTotalQuantity();
			int shipped = row.getShippedQuantity() == null ? 0 : row.getShippedQuantity();
			int pendingReserved = row.getPendingReservedQuantity() == null ? 0 : row.getPendingReservedQuantity();
			int available = Math.max(total - shipped - pendingReserved, 0);
			if (requested > available) {
				String skuCode = row.getSkuCode() != null ? row.getSkuCode() : skuCodes.get(purchaseOrderItemId);
				throw new IllegalStateException(String.format(
						"SKU[%s]可用发货数量不足：需要%d，可用%d，其他草稿占用%d",
						skuCode, requested, available, pendingReserved));
			}
		}
	}

	/**
	 * 根据物流单ID查询明细VO列表（含采购单号、SKU简要信息）
	 * @param shippingOrderId 物流单ID
	 * @return 明细VO列表
	 */
	public List<ShippingOrderItemVO> getVoListByShippingOrderId(Long shippingOrderId) {
		List<ShippingOrderItemVO> items = baseMapper.selectItemVOsByShippingOrderId(shippingOrderId);

		// 填充 SKU 简要信息
		if (!items.isEmpty()) {
			skuBriefService.enrichForQuery(
					items,
					ShippingOrderItemVO::getSkuCode,
					ShippingOrderItemVO::setSkuBrief
			);
		}

		return items;
	}

	/**
	 * 批量保存明细
	 * @param shippingOrderId 物流单ID
	 * @param itemDTOs 明细DTO列表
	 * @return 保存后的明细列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<ShippingOrderItem> batchSave(Long shippingOrderId, List<ShippingOrderItemDTO> itemDTOs) {
		List<ShippingOrderItem> items = ShippingOrderItemConverter.INSTANCE.dtoListToEntityList(itemDTOs);

		for (ShippingOrderItem item : items) {
			item.setShippingOrderId(shippingOrderId);
			// 初始化已到货数量
			item.setReceivedQuantity(0);
			item.setInboundReservedQuantity(0);
		}

		this.saveBatch(items);
		log.info("Batch saved {} items for shippingOrderId={}", items.size(), shippingOrderId);
		return items;
	}

	/**
	 * 根据物流单ID删除明细
	 * @param shippingOrderId 物流单ID
	 * @return 删除数量
	 */
	@Transactional(rollbackFor = Exception.class)
	public int deleteByShippingOrderId(Long shippingOrderId) {
		int count = baseMapper.deleteByShippingOrderId(shippingOrderId);
		log.info("Deleted {} items for shippingOrderId={}", count, shippingOrderId);
		return count;
	}

	@Transactional(rollbackFor = Exception.class)
	public void reserveInboundQuantities(Long shippingOrderId, List<PurchaseInboundOrderItem> inboundItems) {
		if (CollectionUtils.isEmpty(inboundItems)) {
			return;
		}

		List<PurchaseInboundOrderItem> sortedItems = inboundItems.stream()
				.sorted(Comparator.comparing(PurchaseInboundOrderItem::getShippingOrderItemId))
				.collect(Collectors.toList());
		Set<Long> itemIds = new HashSet<>();

		for (PurchaseInboundOrderItem inboundItem : sortedItems) {
			Long shippingItemId = inboundItem.getShippingOrderItemId();
			Integer expectedQuantity = inboundItem.getExpectedQuantity();
			if (shippingItemId == null || expectedQuantity == null || expectedQuantity <= 0) {
				throw new IllegalStateException("入库明细缺少有效的物流明细或应收数量");
			}
			if (!itemIds.add(shippingItemId)) {
				throw new IllegalStateException("同一物流明细不能在一张入库单中重复提交");
			}

			ShippingOrderItem shippingItem = this.getById(shippingItemId);
			if (shippingItem == null
					|| !Objects.equals(shippingItem.getShippingOrderId(), shippingOrderId)
					|| !Objects.equals(shippingItem.getPurchaseOrderId(), inboundItem.getPurchaseOrderId())
					|| !Objects.equals(shippingItem.getPurchaseOrderItemId(), inboundItem.getPurchaseOrderItemId())
					|| !Objects.equals(shippingItem.getSkuCode(), inboundItem.getSkuCode())) {
				throw new IllegalStateException("SKU[" + inboundItem.getSkuCode() + "]与所选物流单明细不匹配");
			}

			int affected = baseMapper.reserveInboundQuantity(shippingItemId, expectedQuantity);
			if (affected == 0) {
				throw new IllegalStateException("SKU[" + inboundItem.getSkuCode() + "]可提交入库数量不足");
			}
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void settleInboundReservations(List<PurchaseInboundOrderItem> inboundItems) {
		if (CollectionUtils.isEmpty(inboundItems)) {
			return;
		}

		List<PurchaseInboundOrderItem> sortedItems = inboundItems.stream()
				.sorted(Comparator.comparing(PurchaseInboundOrderItem::getShippingOrderItemId))
				.collect(Collectors.toList());
		for (PurchaseInboundOrderItem item : sortedItems) {
			int actualQuantity = item.getActualQuantity() == null ? 0 : item.getActualQuantity();
			int affected = baseMapper.settleInboundReservation(
					item.getShippingOrderItemId(), item.getExpectedQuantity(), actualQuantity);
			if (affected == 0) {
				throw new IllegalStateException("SKU[" + item.getSkuCode() + "]入库预占不足或已被其他单据处理");
			}
		}
	}

	/**
	 * 批量获取物流单的SKU种类数
	 * @param shippingOrderIds 物流单ID列表
	 * @return Map<物流单ID, SKU种类数>
	 */
	public Map<Long, Integer> getSkuCountMapByShippingOrderIds(List<Long> shippingOrderIds) {
		if (shippingOrderIds == null || shippingOrderIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<ShippingOrderSkuCountVO> results = baseMapper.selectSkuCountByShippingOrderIds(shippingOrderIds);
		return results.stream().collect(Collectors.toMap(
				ShippingOrderSkuCountVO::getShippingOrderId,
				ShippingOrderSkuCountVO::getSkuCount
		));
	}

	/**
	 * 批量获取物流单的发货/入库数量统计
	 * @param shippingOrderIds 物流单ID列表
	 * @return Map<物流单ID, 数量统计VO>
	 */
	public Map<Long, ShippingQuantityStatsVO> getQuantityStatsMapByShippingOrderIds(List<Long> shippingOrderIds) {
		if (shippingOrderIds == null || shippingOrderIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<ShippingQuantityStatsVO> results = baseMapper.selectQuantityStatsByShippingOrderIds(shippingOrderIds);
		return results.stream().collect(Collectors.toMap(
				ShippingQuantityStatsVO::getShippingOrderId,
				stats -> stats
		));
	}

	/**
	 * 批量增加已到货数量
	 * @param params 更新参数列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void batchIncreaseReceivedQuantity(List<ReceivedQuantityUpdateParam> params) {
		if (params == null || params.isEmpty()) {
			return;
		}
		int affected = baseMapper.batchIncreaseReceivedQuantity(params);
		if (affected < params.size()) {
			throw new IllegalStateException("批量更新到货数量失败，部分明细不存在");
		}
		log.info("Batch increased received quantity for {} items", params.size());
	}

	/**
	 * 获取物流单的到货统计
	 * @param shippingOrderId 物流单ID
	 * @return 到货统计VO
	 */
	public ShippingOrderArrivalStatsVO getArrivalStats(Long shippingOrderId) {
		return baseMapper.selectArrivalStats(shippingOrderId);
	}

	/**
	 * 增加已到货数量（入库单确认时）
	 * @param items 入库单明细实体列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void increaseReceivedQuantity(List<PurchaseInboundOrderItem> items) {
		if (CollectionUtils.isEmpty(items)) {
			return;
		}

		// 过滤有效数据并按 shippingOrderItemId 排序
		List<PurchaseInboundOrderItem> sortedItems = items.stream()
				.filter(item -> item.getActualQuantity() != null && item.getActualQuantity() > 0)
				.sorted(Comparator.comparing(PurchaseInboundOrderItem::getShippingOrderItemId))
				.collect(Collectors.toList());

		if (sortedItems.isEmpty()) {
			return;
		}

		for (PurchaseInboundOrderItem item : sortedItems) {
			int affected = baseMapper.increaseReceivedQuantity(
					item.getShippingOrderItemId(),
					item.getActualQuantity()
			);
			if (affected == 0) {
				throw new IllegalStateException("SKU[" + item.getSkuCode() + "]可入库数量不足");
			}
		}
		log.info("Increased received quantity for {} shipping order items", sortedItems.size());
	}

}
