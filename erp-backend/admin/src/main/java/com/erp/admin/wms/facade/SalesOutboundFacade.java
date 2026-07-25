package com.erp.admin.wms.facade;

import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.product.service.SkuMappingService;
import com.erp.admin.wms.model.dto.SalesOutboundDTO;
import com.erp.admin.wms.model.dto.SalesOutboundItemDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrder;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.enums.OutboundOrderStatus;
import com.erp.admin.wms.model.vo.StockShortageVO;
import com.erp.admin.wms.service.WmsPhysicalInventoryService;
import com.erp.admin.wms.service.OutboundPickingService;
import com.erp.admin.wms.service.LocationTransferOrderService;
import com.erp.admin.wms.service.SalesOutboundItemService;
import com.erp.admin.wms.service.SalesOutboundPackageService;
import com.erp.admin.wms.service.SalesOutboundService;
import com.erp.admin.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销售出库单业务编排层
 * <p>
 * 负责跨服务的业务流程编排，持有事务。
 * Service 层专注于单一领域的 CRUD 和状态管理。
 * </p>
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesOutboundFacade {

    private final SalesOutboundService salesOutboundService;
    private final SalesOutboundItemService salesOutboundItemService;
    private final WmsPhysicalInventoryService physicalInventoryService;
    private final ErpOrderService erpOrderService;
    private final OutboundPickingService outboundPickingService;
    private final LocationTransferOrderService locationTransferOrderService;
    private final WarehouseService warehouseService;
	private final SalesOutboundPackageService outboundPackageService;
	private final ErpOrderItemMapper erpOrderItemMapper;
	private final SkuMappingService skuMappingService;

    /**
     * 创建出库单
     * 1. 校验订单状态和平台
     * 2. 创建出库单和明细
     * 3. 将 ALLOCATED 订单绑定到新建的销售出库单
     *
     * @param dto 出库单DTO
     * @return 出库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(SalesOutboundDTO dto) {
        warehouseService.validateOperableOwnWarehouse(dto.getWarehouseId());
		// 前端仅负责选择订单，SKU、数量、平台单号一律以后端订单快照为准。
		dto.setItems(buildCanonicalItems(dto, null));
        // 1. 校验订单（使用乐观锁）
        validateAndAllocateOrders(dto);

        // 2. 创建出库单
        Long orderId = salesOutboundService.saveOrder(dto);
		SalesOutboundOrder savedOrder = salesOutboundService.getByIdOrThrow(orderId);
		outboundPackageService.replacePackages(orderId, savedOrder.getErpTenantId(), dto.getPlatform(), dto.getItems());
        erpOrderService.bindOutboundAllocation(extractOrderIds(dto.getItems()), orderId);

        log.info("Created sales outbound order via facade, id={}, platform={}", orderId, dto.getPlatform());
        return orderId;
    }

    /**
     * 更新出库单
     * 1. 释放原订单占用
     * 2. 校验并占用新订单
     * 3. 更新出库单和明细
     *
     * @param dto 出库单DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SalesOutboundDTO dto) {
        warehouseService.validateOperableOwnWarehouse(dto.getWarehouseId());
		// 锁住草稿，防止两个编辑请求互相释放/覆盖订单占用。
        SalesOutboundOrder order = salesOutboundService.getByIdForUpdate(dto.getId());
		Assert.isTrue(!com.erp.admin.wms.model.enums.OutboundSourceType.CUSTOM.name().equals(order.getSourceType()),
				"该单据为自定义出库单，请在自定义出库单页面操作");
        Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(order.getOrderStatus()),
                "只有草稿状态的出库单可以编辑");
        Assert.isTrue(dto.getPlatform() != null && dto.getPlatform().equalsIgnoreCase(order.getPlatform()),
                "出库单平台不可修改");

        // 1. 获取原明细并释放订单占用
        List<SalesOutboundOrderItem> oldItems = salesOutboundItemService.getByOutboundOrderId(order.getId());
        List<Long> oldOrderIds = oldItems.stream()
                .map(SalesOutboundOrderItem::getErpOrderId)
                .distinct()
                .collect(Collectors.toList());
		dto.setItems(buildCanonicalItems(dto, order.getId()));
		List<Long> newOrderIds = extractOrderIds(dto.getItems());
		Set<Long> newIdSet = new HashSet<>(newOrderIds);
		Set<Long> oldIdSet = new HashSet<>(oldOrderIds);
		List<Long> removedOrderIds = oldOrderIds.stream().filter(id -> !newIdSet.contains(id))
				.collect(Collectors.toList());
		List<Long> addedOrderIds = newOrderIds.stream().filter(id -> !oldIdSet.contains(id))
				.collect(Collectors.toList());
		if (!removedOrderIds.isEmpty()) {
			erpOrderService.releaseOutboundAllocation(removedOrderIds, order.getId());
		}
		if (!addedOrderIds.isEmpty()) {
			erpOrderService.allocateForOutbound(addedOrderIds, dto.getPlatform());
		}

        // 3. 更新出库单
        salesOutboundService.updateOrder(dto);
		outboundPackageService.replacePackages(order.getId(), order.getErpTenantId(), order.getPlatform(), dto.getItems());
        erpOrderService.bindOutboundAllocation(newOrderIds, order.getId());

        log.info("Updated sales outbound order via facade, id={}", dto.getId());
    }

    /**
     * 删除出库单
     * 1. 释放订单占用
     * 2. 删除出库单和明细
     *
     * @param ids 出库单ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        for (Long id : ids) {
			SalesOutboundOrder order = salesOutboundService.getByIdForUpdate(id);
			Assert.isTrue(!com.erp.admin.wms.model.enums.OutboundSourceType.CUSTOM.name().equals(order.getSourceType()),
					"该单据为自定义出库单，请在自定义出库单页面操作");
            Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(order.getOrderStatus()),
                    "只有草稿状态的出库单可以删除");

            // 1. 释放订单占用
            List<SalesOutboundOrderItem> items = salesOutboundItemService.getByOutboundOrderId(id);
            List<Long> orderIds = items.stream()
                    .map(SalesOutboundOrderItem::getErpOrderId)
                    .distinct()
                    .collect(Collectors.toList());
            if (!orderIds.isEmpty()) {
                erpOrderService.releaseOutboundAllocation(orderIds, id);
            }

            // 2. 删除出库单
			outboundPackageService.deleteByOutboundOrderId(id);
            salesOutboundService.deleteOrder(id);

            log.info("Deleted sales outbound order via facade, id={}", id);
        }
    }

    /**
     * 取消出库单
     * 1. 释放订单占用
     * 2. 更新出库单状态
     *
     * @param id 出库单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        SalesOutboundOrder order = salesOutboundService.getByIdForUpdate(id);
        String status = order.getOrderStatus();
        // 方案A：允许 草稿(DRAFT) 或 已确认待下架(CONFIRMED) 取消；平台一旦下架(PICKING)及之后拒绝取消
        Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(status)
                        || OutboundOrderStatus.WAITING_TRANSFER.name().equals(status)
                        || OutboundOrderStatus.CONFIRMED.name().equals(status),
                "仅待下架前(草稿/已确认)的出库单可以取消");

        List<SalesOutboundOrderItem> items = salesOutboundItemService.getByOutboundOrderId(id);

        // 1. 已确认单：释放海外仓物理批次预留（可用恢复）
        if (OutboundOrderStatus.WAITING_TRANSFER.name().equals(status)) {
            locationTransferOrderService.cancelOutboundPlan(id);
        }
        else if (OutboundOrderStatus.CONFIRMED.name().equals(status)) {
            outboundPickingService.releaseForOrder(order);
        }

        // 2. 释放 ERP 订单占用
        List<Long> orderIds = items.stream()
                .map(SalesOutboundOrderItem::getErpOrderId)
                .distinct()
                .collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            erpOrderService.releaseOutboundAllocation(orderIds, id);
        }

        // 3. 更新出库单状态
        salesOutboundService.updateToCancelled(id);

        log.info("Cancelled sales outbound order via facade, id={}, fromStatus={}", id, status);
    }

    /**
     * 确认出库
     * 1. 校验订单仍由当前出库单占用
     * 2. 校验库存是否充足
     * 3. 执行库存过账
     * 4. 更新出库单状态；ERP 订单在海外仓签出时才转 COMPLETED
     *
     * @param id 出库单ID
     * @return 库存不足明细列表（空列表表示成功）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<StockShortageVO> confirm(Long id) {
        SalesOutboundOrder order = salesOutboundService.getByIdForUpdate(id);
        Assert.isTrue(OutboundOrderStatus.DRAFT.name().equals(order.getOrderStatus()),
                "只有草稿状态的出库单可以确认");

          List<SalesOutboundOrderItem> items = salesOutboundItemService.getByOutboundOrderId(id);
          Assert.notEmpty(items, "出库明细不能为空");

          // 正式提交时重新读取平台订单和订单商品，避免草稿保存后平台状态、SKU映射或数量变化。
          SalesOutboundDTO snapshotDTO = new SalesOutboundDTO();
          snapshotDTO.setPlatform(order.getPlatform());
          snapshotDTO.setItems(items.stream().map(item -> {
              SalesOutboundItemDTO selected = new SalesOutboundItemDTO();
              selected.setErpOrderId(item.getErpOrderId());
              return selected;
          }).collect(Collectors.toList()));
          List<SalesOutboundItemDTO> currentSnapshot = buildCanonicalItems(snapshotDTO, id);
          Assert.isTrue(sameOutboundSnapshot(items, currentSnapshot),
                  "关联订单内容已发生变化，请重新编辑并保存草稿后再提交");

        // 1. 库存充足性预检（快照，快速失败）
        List<StockShortageVO> shortages = checkStockSufficiency(order, items);
        if (!shortages.isEmpty()) {
            return shortages;
        }

        // 2. 批次预留（方案A）：确认时就 FIFO 锁批次 + reserved_qty += + 建拣货分配 + 刷新快照，
        //    可用数当场从批次算着下降。缺货则返回明细，事务回滚、单据保持 DRAFT。
        List<StockShortageVO> reserveShortages = outboundPickingService.reserveAvailableForOrder(order, items);
        if (!reserveShortages.isEmpty()) {
            locationTransferOrderService.createOutboundPlan(order, reserveShortages);

            List<Long> orderIds = items.stream()
                    .map(SalesOutboundOrderItem::getErpOrderId)
                    .distinct()
                    .collect(Collectors.toList());
            erpOrderService.validateOutboundAllocation(orderIds, id);
            salesOutboundService.updateToWaitingTransfer(id);
            log.info("Sales outbound order is waiting for location transfer, id={}", id);
            return Collections.emptyList();
        }

        // 3. 校验 ERP 订单仍由当前出库单占用；保持 ALLOCATED，防止重复关联
        List<Long> orderIds = items.stream()
                .map(SalesOutboundOrderItem::getErpOrderId)
                .distinct()
                .collect(Collectors.toList());
        erpOrderService.validateOutboundAllocation(orderIds, id);

        // 4. 更新出库单状态。库存预占已经由物理批次分配落地，不再依赖平台订单区域账。
        salesOutboundService.updateToConfirmed(id, null);

        log.info("Confirmed sales outbound order via facade, id={}, physical batches reserved", id);
        return Collections.emptyList();
    }

    /** 校验具体海外仓的物理可分配库存是否充足。 */
    private List<StockShortageVO> checkStockSufficiency(SalesOutboundOrder order, List<SalesOutboundOrderItem> items) {
        // 按 SKU 汇总需求数量
        Map<String, Integer> requiredMap = items.stream()
                .collect(Collectors.groupingBy(
                        SalesOutboundOrderItem::getSkuCode,
                        Collectors.summingInt(SalesOutboundOrderItem::getQuantity)
                ));

        // 查询仓库库存
        List<String> skuCodes = new ArrayList<>(requiredMap.keySet());
        Map<String, Integer> stockMap = physicalInventoryService.getOwnerAvailableQuantityMap(
                order.getErpTenantId(), order.getWarehouseId(), skuCodes);

        // 检查不足的 SKU
        List<StockShortageVO> shortages = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : requiredMap.entrySet()) {
            String skuCode = entry.getKey();
            int required = entry.getValue();

            // 检查具体海外仓可用库存；最终并发安全校验由 reserveForOrder 的行锁完成。
            int available = stockMap.getOrDefault(skuCode, 0);
            if (available < required) {
                StockShortageVO shortage = new StockShortageVO();
                shortage.setSkuCode(skuCode);
                shortage.setSkuName(skuCode);
                shortage.setRequiredQty(required);
                shortage.setAvailableQty(available);
                shortage.setShortage(required - available);
                shortages.add(shortage);
            }
        }

        return shortages;
    }

    /**
     * 校验并占用订单
     */
    private void validateAndAllocateOrders(SalesOutboundDTO dto) {
        List<Long> orderIds = extractOrderIds(dto.getItems());

        if (!orderIds.isEmpty()) {
            // 使用乐观锁占用订单
            erpOrderService.allocateForOutbound(orderIds, dto.getPlatform());
        }
    }

	/**
	 * 根据 ERP 订单主数据重建出库明细。前端传来的 SKU、数量和平台单号不参与落库。
	 */
	private List<SalesOutboundItemDTO> buildCanonicalItems(SalesOutboundDTO dto, Long currentOutboundId) {
		List<Long> orderIds = extractOrderIds(dto.getItems());
		Assert.notEmpty(orderIds, "请选择待出库订单");
		Map<Long, ErpOrder> orderMap = new LinkedHashMap<>();
		for (Long orderId : orderIds) {
			ErpOrder order = erpOrderService.getById(orderId);
			Assert.notNull(order, "订单不存在，订单ID: " + orderId);
			Assert.isTrue(dto.getPlatform() != null && dto.getPlatform().equalsIgnoreCase(order.getPlatform()),
					"订单平台不匹配，订单ID: " + orderId);
			Assert.isTrue("SHIPPED".equals(order.getErpStatus()),
					"订单尚未在平台确认，订单ID: " + orderId);
			Assert.isTrue("FBS".equalsIgnoreCase(order.getFulfillmentType()),
					"仅FBS订单可以创建销售出库单，订单ID: " + orderId);
			Assert.isTrue(order.getLocked() == null || order.getLocked() == 0,
					"订单已锁定，订单ID: " + orderId);
			boolean available = "NONE".equals(order.getOutboundStatus())
					|| (currentOutboundId != null && "ALLOCATED".equals(order.getOutboundStatus())
					&& java.util.Objects.equals(currentOutboundId, order.getOutboundOrderId()));
			Assert.isTrue(available, "订单已被其他出库单占用，订单ID: " + orderId);
			orderMap.put(orderId, order);
		}

		List<ErpOrderItem> sourceItems = erpOrderItemMapper.selectByOrderIds(orderIds);
		Assert.notEmpty(sourceItems, "所选订单没有商品明细");
		Set<String> platformItemIds = sourceItems.stream().map(ErpOrderItem::getPlatformItemId)
				.filter(StringUtils::hasText).collect(Collectors.toSet());
		Map<String, String> skuCodeMap = platformItemIds.isEmpty() ? Collections.emptyMap()
				: skuMappingService.getSkuCodeMapByPlatformItemIds(platformItemIds);

		List<SalesOutboundItemDTO> canonicalItems = new ArrayList<>();
		for (ErpOrderItem sourceItem : sourceItems) {
			ErpOrder order = orderMap.get(sourceItem.getOrderId());
			if (order == null) {
				continue;
			}
			String skuCode = StringUtils.hasText(sourceItem.getSkuCode())
					? sourceItem.getSkuCode() : skuCodeMap.get(sourceItem.getPlatformItemId());
			Assert.hasText(skuCode, "订单SKU映射缺失，订单ID: " + order.getId()
					+ "，平台商品ID: " + sourceItem.getPlatformItemId());
			Assert.isTrue(sourceItem.getQuantity() != null && sourceItem.getQuantity() > 0,
					"订单商品数量无效，订单ID: " + order.getId());
			SalesOutboundItemDTO item = new SalesOutboundItemDTO();
			item.setErpOrderId(order.getId());
			item.setPlatformOrderId(order.getPlatformOrderId());
			item.setSkuCode(skuCode);
			item.setQuantity(sourceItem.getQuantity());
			canonicalItems.add(item);
		}
		Assert.notEmpty(canonicalItems, "所选订单没有可出库商品明细");
		return canonicalItems;
	}

      private List<Long> extractOrderIds(List<SalesOutboundItemDTO> items) {
        return items.stream()
                .map(SalesOutboundItemDTO::getErpOrderId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
      }

	private boolean sameOutboundSnapshot(List<SalesOutboundOrderItem> saved,
			List<SalesOutboundItemDTO> current) {
		Map<String, Integer> savedMap = saved.stream().collect(Collectors.groupingBy(
				item -> item.getErpOrderId() + "|" + item.getSkuCode() + "|" + item.getPlatformOrderId(),
				LinkedHashMap::new, Collectors.summingInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())));
		Map<String, Integer> currentMap = current.stream().collect(Collectors.groupingBy(
				item -> item.getErpOrderId() + "|" + item.getSkuCode() + "|" + item.getPlatformOrderId(),
				LinkedHashMap::new, Collectors.summingInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())));
		return savedMap.equals(currentMap);
	}

  }
