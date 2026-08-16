package com.erp.admin.wms.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.common.tenant.WmsTenantContext;
import com.erp.admin.product.mapper.SkuMapper;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.dto.FulfillmentCreateCommand;
import com.erp.admin.wms.model.dto.ManualFulfillmentDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentItem;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class ManualFulfillmentService {
	private final WmsFulfillmentOrderMapper orderMapper;
	private final WmsFulfillmentItemMapper itemMapper;
	private final SkuMapper skuMapper;
	private final WarehouseSkuCodeService warehouseSkuCodeService;
	private final WarehouseService warehouseService;
	private final FulfillmentReservationService reservationService;

	@Transactional(rollbackFor = Exception.class)
	public Long saveDraft(ManualFulfillmentDTO dto) {
		Assert.notNull(dto, "人工出库参数不能为空");
		validateItems(dto.getItems());
		warehouseService.validateOperableOwnWarehouse(dto.getWarehouseId());
		Long ownerId = requireOwner();
		Long wmsTenantId = requireWmsTenant();
		WmsFulfillmentOrder order;
		if (dto.getId() == null) {
			long sourceId = IdWorker.getId();
			order = new WmsFulfillmentOrder();
			order.setTenantId(TenantContext.BLOCK_TENANT_ID);
			order.setWmsTenantId(wmsTenantId);
			order.setErpTenantId(ownerId);
			order.setWarehouseId(dto.getWarehouseId());
			order.setFulfillmentNo("MO-" + sourceId);
			order.setSourceType("MANUAL");
			order.setSourceOrderId(sourceId);
			order.setSourceOrderNo("MO-" + sourceId);
			order.setFulfillmentStatus(FulfillmentStatus.DRAFT);
			order.setVersion(0);
			order.setDeleted(0L);
			copyRecipient(order, dto);
			Assert.isTrue(orderMapper.insert(order) == 1, "人工出库草稿创建失败");
		}
		else {
			order = requireOwnedDraft(dto.getId(), ownerId);
			WmsFulfillmentOrder update = new WmsFulfillmentOrder();
			update.setId(order.getId());
			update.setWarehouseId(dto.getWarehouseId());
			copyRecipient(update, dto);
			Assert.isTrue(orderMapper.updateById(update) == 1, "人工出库草稿更新失败");
			itemMapper.delete(Wrappers.<WmsFulfillmentItem>lambdaQuery()
					.eq(WmsFulfillmentItem::getFulfillmentOrderId, order.getId()));
			order.setWarehouseId(dto.getWarehouseId());
		}
		insertItems(order.getId(), ownerId, dto.getItems());
		return order.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	public void submit(Long id) {
		Long ownerId = requireOwner();
		WmsFulfillmentOrder order = requireOwnedDraft(id, ownerId);
		List<WmsFulfillmentItem> items = listItemsInternal(id);
		Assert.notEmpty(items, "人工出库没有商品");
		FulfillmentCreateCommand command = toCommand(order, items);
		FulfillmentOrderService.validate(command);
		reservationService.reserve(id, command);
		Assert.isTrue(orderMapper.transit(id, FulfillmentStatus.DRAFT, FulfillmentStatus.WAITING_SHELF) == 1,
				"草稿状态已变化，请刷新后重试");
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteDraft(Long id) {
		WmsFulfillmentOrder order = requireOwnedDraft(id, requireOwner());
		itemMapper.delete(Wrappers.<WmsFulfillmentItem>lambdaQuery()
				.eq(WmsFulfillmentItem::getFulfillmentOrderId, order.getId()));
		Assert.isTrue(orderMapper.deleteById(id) == 1, "草稿删除失败");
	}

	public List<WmsFulfillmentOrder> list() {
		return orderMapper.selectList(Wrappers.<WmsFulfillmentOrder>lambdaQuery()
				.eq(WmsFulfillmentOrder::getErpTenantId, requireOwner())
				.eq(WmsFulfillmentOrder::getSourceType, "MANUAL")
				.orderByDesc(WmsFulfillmentOrder::getCreateTime));
	}

	public WmsFulfillmentOrder detail(Long id) {
		WmsFulfillmentOrder order = orderMapper.selectById(id);
		Assert.notNull(order, "人工出库单不存在");
		Assert.isTrue(requireOwner().equals(order.getErpTenantId()) && "MANUAL".equals(order.getSourceType()),
				"无权查看该人工出库单");
		return order;
	}

	public List<WmsFulfillmentItem> listItems(Long id) {
		detail(id);
		return listItemsInternal(id);
	}

	private List<WmsFulfillmentItem> listItemsInternal(Long id) {
		return itemMapper.selectList(Wrappers.<WmsFulfillmentItem>lambdaQuery()
				.eq(WmsFulfillmentItem::getFulfillmentOrderId, id)
				.orderByAsc(WmsFulfillmentItem::getId));
	}

	private void validateItems(List<ManualFulfillmentDTO.Item> lines) {
		Assert.notEmpty(lines, "人工出库商品不能为空");
		Set<String> skuCodes = new HashSet<>();
		for (ManualFulfillmentDTO.Item line : lines) {
			Assert.notNull(line, "商品明细不能为空");
			Assert.hasText(line.getSkuCode(), "SKU不能为空");
			Assert.isTrue(line.getQuantity() != null && line.getQuantity() > 0, "数量必须大于0");
			String normalized = line.getSkuCode().trim().toUpperCase(Locale.ROOT);
			Assert.isTrue(skuCodes.add(normalized), "同一人工出库单内SKU不能重复：" + line.getSkuCode());
		}
	}

	private void insertItems(Long orderId, Long ownerId, List<ManualFulfillmentDTO.Item> lines) {
		Assert.notEmpty(lines, "人工出库商品不能为空");
		for (ManualFulfillmentDTO.Item line : lines) {
			Sku sku = skuMapper.selectBySkuCode(line.getSkuCode());
			Assert.notNull(sku, "SKU不存在：" + line.getSkuCode());
			WmsFulfillmentItem item = new WmsFulfillmentItem();
			item.setFulfillmentOrderId(orderId);
			item.setSkuCode(line.getSkuCode());
			item.setWarehouseSkuCode(warehouseSkuCodeService.build(ownerId, line.getSkuCode()));
			item.setSkuName(sku.getChineseName());
			item.setQuality("GOOD");
			item.setQuantity(line.getQuantity());
			item.setPickedQuantity(0);
			item.setOuterLengthMm(sku.getOuterLengthMm());
			item.setOuterWidthMm(sku.getOuterWidthMm());
			item.setOuterHeightMm(sku.getOuterHeightMm());
			item.setOuterGrossWeightG(sku.getOuterGrossWeightG());
			item.setDeleted(0L);
			Assert.isTrue(itemMapper.insert(item) == 1, "人工出库商品保存失败");
		}
	}

	private FulfillmentCreateCommand toCommand(WmsFulfillmentOrder order, List<WmsFulfillmentItem> items) {
		FulfillmentCreateCommand command = new FulfillmentCreateCommand();
		command.setTenantId(order.getTenantId());
		command.setWmsTenantId(order.getWmsTenantId());
		command.setErpTenantId(order.getErpTenantId());
		command.setWarehouseId(order.getWarehouseId());
		command.setSourceType("MANUAL");
		command.setSourceOrderId(order.getSourceOrderId());
		command.setSourceOrderNo(order.getSourceOrderNo());
		List<FulfillmentCreateCommand.Item> snapshots = new ArrayList<>();
		for (WmsFulfillmentItem row : items) {
			FulfillmentCreateCommand.Item item = new FulfillmentCreateCommand.Item();
			item.setSkuCode(row.getSkuCode());
			item.setWarehouseSkuCode(row.getWarehouseSkuCode());
			item.setSkuName(row.getSkuName());
			item.setQuality(row.getQuality());
			item.setQuantity(row.getQuantity());
			item.setOuterLengthMm(row.getOuterLengthMm());
			item.setOuterWidthMm(row.getOuterWidthMm());
			item.setOuterHeightMm(row.getOuterHeightMm());
			item.setOuterGrossWeightG(row.getOuterGrossWeightG());
			snapshots.add(item);
		}
		command.setItems(snapshots);
		return command;
	}

	private WmsFulfillmentOrder requireOwnedDraft(Long id, Long ownerId) {
		WmsFulfillmentOrder order = orderMapper.selectForUpdate(id);
		Assert.notNull(order, "人工出库草稿不存在");
		Assert.isTrue(ownerId.equals(order.getErpTenantId()) && "MANUAL".equals(order.getSourceType()), "无权操作该草稿");
		Assert.isTrue(order.getFulfillmentStatus() == FulfillmentStatus.DRAFT, "只有草稿可以修改或删除");
		return order;
	}

	private void copyRecipient(WmsFulfillmentOrder order, ManualFulfillmentDTO dto) {
		order.setRecipientName(dto.getRecipientName());
		order.setRecipientPhone(dto.getRecipientPhone());
		order.setRecipientAddress(dto.getRecipientAddress());
	}

	private Long requireOwner() {
		Long ownerId = TenantContext.getCurrentTenant();
		Assert.isTrue(ownerId != null && ownerId > 0, "当前货主身份无效");
		return ownerId;
	}

	private Long requireWmsTenant() {
		Long wmsTenantId = WmsTenantContext.getCurrentWmsTenant();
		Assert.notNull(wmsTenantId, "当前货主尚未绑定WMS服务商");
		return wmsTenantId;
	}
}
