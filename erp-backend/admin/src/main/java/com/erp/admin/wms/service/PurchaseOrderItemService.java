package com.erp.admin.wms.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.erp.admin.wms.converter.PurchaseOrderItemConverter;
import com.erp.admin.wms.mapper.PurchaseOrderItemMapper;
import com.erp.admin.wms.model.dto.PurchaseOrderItemDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import com.erp.admin.wms.model.entity.PurchaseOrderItem;
import com.erp.admin.wms.model.entity.ShippingOrderItem;
import com.erp.admin.wms.model.vo.PurchaseOrderItemVO;
import com.erp.admin.wms.model.vo.PurchaseOrderStatVO;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 采购单明细服务
 *
 * @author erp
 */
@Slf4j
@Service
public class PurchaseOrderItemService extends ExtendServiceImpl<PurchaseOrderItemMapper, PurchaseOrderItem> {

	/**
	 * 批量统计采购单明细数据
	 * @param orderIds 采购单ID列表
	 * @return 统计结果Map，key为采购单ID
	 */
	public Map<Long, PurchaseOrderStatVO> getStatByOrderIds(Collection<Long> orderIds) {
		if (CollectionUtils.isEmpty(orderIds)) {
			return Collections.emptyMap();
		}
		List<PurchaseOrderStatVO> statList = baseMapper.selectStatByOrderIds(orderIds);
		return statList.stream()
				.collect(Collectors.toMap(PurchaseOrderStatVO::getPurchaseOrderId, stat -> stat));
	}

	/**
	 * 获取单个采购单的统计数据
	 * @param purchaseOrderId 采购单ID
	 * @return 统计数据
	 */
	public PurchaseOrderStatVO getStatByOrderId(Long purchaseOrderId) {
		Map<Long, PurchaseOrderStatVO> statMap = getStatByOrderIds(Collections.singletonList(purchaseOrderId));
		return statMap.getOrDefault(purchaseOrderId, new PurchaseOrderStatVO());
	}

	/**
	 * 根据采购单ID查询明细列表
	 * @param purchaseOrderId 采购单ID
	 * @return 明细列表
	 */
	public List<PurchaseOrderItem> getByPurchaseOrderId(Long purchaseOrderId) {
		return baseMapper.selectByPurchaseOrderId(purchaseOrderId);
	}

	/**
	 * 根据采购单ID查询明细VO列表
	 * @param purchaseOrderId 采购单ID
	 * @return 明细VO列表
	 */
	public List<PurchaseOrderItemVO> getVoListByPurchaseOrderId(Long purchaseOrderId) {
		List<PurchaseOrderItem> items = baseMapper.selectByPurchaseOrderId(purchaseOrderId);
		return PurchaseOrderItemConverter.INSTANCE.entityListToVoList(items);
	}

	/**
	 * 批量保存明细
	 * @param purchaseOrderId 采购单ID
	 * @param itemDTOs 明细DTO列表
	 * @return 保存后的明细列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<PurchaseOrderItem> batchSave(Long purchaseOrderId, List<PurchaseOrderItemDTO> itemDTOs) {
		List<PurchaseOrderItem> items = PurchaseOrderItemConverter.INSTANCE.dtoListToEntityList(itemDTOs);

		for (PurchaseOrderItem item : items) {
			item.setPurchaseOrderId(purchaseOrderId);
			// 计算金额 = 数量 × 单价
			item.setAmount(calculateAmount(item.getQuantity(), item.getUnitPrice()));
			// 初始化发货和入库数量
			item.setShippedQuantity(0);
			item.setReceivedQuantity(0);
		}

		this.saveBatch(items);
		log.info("Batch saved {} items for purchaseOrderId={}", items.size(), purchaseOrderId);
		return items;
	}

	/**
	 * 批量更新明细（先删后增）
	 * @param purchaseOrderId 采购单ID
	 * @param itemDTOs 明细DTO列表
	 * @return 更新后的明细列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public List<PurchaseOrderItem> batchUpdate(Long purchaseOrderId, List<PurchaseOrderItemDTO> itemDTOs) {
		// 删除原有明细
		baseMapper.deleteByPurchaseOrderId(purchaseOrderId);

		// 重新保存
		return batchSave(purchaseOrderId, itemDTOs);
	}

	/**
	 * 根据采购单ID删除明细
	 * @param purchaseOrderId 采购单ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void deleteByPurchaseOrderId(Long purchaseOrderId) {
		int count = baseMapper.deleteByPurchaseOrderId(purchaseOrderId);
		log.info("Deleted {} items for purchaseOrderId={}", count, purchaseOrderId);
	}

	/**
	 * 计算金额
	 * @param quantity 数量
	 * @param unitPrice 单价
	 * @return 金额
	 */
	public BigDecimal calculateAmount(Integer quantity, BigDecimal unitPrice) {
		if (quantity == null || unitPrice == null) {
			return BigDecimal.ZERO;
		}
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	/**
	 * 计算明细列表的总金额
	 * @param items 明细列表
	 * @return 总金额
	 */
	public BigDecimal calculateTotalAmount(List<PurchaseOrderItem> items) {
		if (items == null || items.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return items.stream()
				.map(PurchaseOrderItem::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * 增加已发货数量（确认发货时）
	 * @param items 物流单明细实体列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void increaseShippedQuantity(List<ShippingOrderItem> items) {
		if (CollectionUtils.isEmpty(items)) {
			return;
		}

		// 按 purchaseOrderItemId 排序，避免死锁
		List<ShippingOrderItem> sortedItems = items.stream()
				.sorted(Comparator.comparing(ShippingOrderItem::getPurchaseOrderItemId))
				.collect(Collectors.toList());

		for (ShippingOrderItem item : sortedItems) {
			int affected = baseMapper.increaseShippedQuantity(
					item.getPurchaseOrderItemId(),
					item.getQuantity()
			);
			if (affected == 0) {
				throw new IllegalStateException("SKU[" + item.getSkuCode() + "]可发货数量不足");
			}
		}
		log.info("Increased shipped quantity for {} items", items.size());
	}

	/**
	 * 增加已入库数量（入库单确认时）
	 * @param items 入库单明细实体列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void increaseReceivedQuantity(List<PurchaseInboundOrderItem> items) {
		if (CollectionUtils.isEmpty(items)) {
			return;
		}

		// 过滤有效数据并按 purchaseOrderItemId 排序
		List<PurchaseInboundOrderItem> sortedItems = items.stream()
				.filter(item -> item.getActualQuantity() != null && item.getActualQuantity() > 0)
				.sorted(Comparator.comparing(PurchaseInboundOrderItem::getPurchaseOrderItemId))
				.collect(Collectors.toList());

		if (sortedItems.isEmpty()) {
			return;
		}

		// 逐个执行条件更新
		for (PurchaseInboundOrderItem item : sortedItems) {
			int affected = baseMapper.increaseReceivedQuantity(
					item.getPurchaseOrderItemId(),
					item.getActualQuantity()
			);
			if (affected == 0) {
				throw new IllegalStateException("SKU[" + item.getSkuCode() + "]入库数量超过已发货数量");
			}
		}
		log.info("Increased received quantity for {} purchase order items", sortedItems.size());
	}

}
