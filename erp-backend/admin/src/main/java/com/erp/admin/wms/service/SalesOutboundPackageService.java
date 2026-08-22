package com.erp.admin.wms.service;

import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.service.ErpOrderService;
import com.erp.admin.order.service.ozon.OzonActService;
import com.erp.admin.product.service.SkuBarcodeService;
import com.erp.admin.product.service.WarehouseSkuCodeService;
import com.erp.admin.wms.mapper.SalesOutboundItemMapper;
import com.erp.admin.wms.mapper.WmsOutboundScanEventMapper;
import com.erp.admin.wms.mapper.WmsSalesOutboundPackageMapper;
import com.erp.admin.wms.model.dto.SalesOutboundItemDTO;
import com.erp.admin.wms.model.entity.WmsOutboundScanEvent;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.entity.WmsSalesOutboundPackage;
import com.erp.admin.wms.model.vo.OutboundPackageVO;
import com.erp.admin.wms.model.vo.PackShipItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesOutboundPackageService {

	public static final String SORT_PENDING = "PENDING";
	public static final String SORT_SORTED = "SORTED";
	public static final String SORT_NOT_REQUIRED = "NOT_REQUIRED";
	public static final String LABEL_NOT_READY = "NOT_READY";
	public static final String LABEL_READY = "READY";
	public static final String LABEL_EXTERNAL_CONFIRMED = "EXTERNAL_CONFIRMED";
	public static final String LABEL_ATTACHED_CONFIRMED = "ATTACHED_CONFIRMED";
	public static final String HANDOVER_NOT_REQUIRED = "NOT_REQUIRED";
	public static final String HANDOVER_PENDING = "PENDING";
	public static final String HANDOVER_READY = "READY";
	public static final String HANDOVER_EXTERNAL_CONFIRMED = "EXTERNAL_CONFIRMED";
	public static final String PACK_PENDING = "PENDING";
	public static final String PACK_PACKED = "PACKED";
	public static final String SHIP_PENDING = "PENDING";
	public static final String SHIP_SHIPPED = "SHIPPED";

	private final WmsSalesOutboundPackageMapper packageMapper;
	private final SalesOutboundItemMapper outboundItemMapper;
	private final ErpOrderService erpOrderService;
	private final OzonActService ozonActService;
	private final SkuBarcodeService skuBarcodeService;
	private final WarehouseSkuCodeService warehouseSkuCodeService;
	private final WmsOutboundScanEventMapper scanEventMapper;
	private final PlatformLabelVerificationService labelVerificationService;

	@Transactional(rollbackFor = Exception.class)
	public void replacePackages(Long outboundOrderId, Long erpTenantId, String platform,
			List<SalesOutboundItemDTO> items) {
		Assert.notNull(outboundOrderId, "出库单ID不能为空");
		List<Long> orderIds = items.stream().map(SalesOutboundItemDTO::getErpOrderId)
				.filter(Objects::nonNull).distinct().collect(Collectors.toList());
		Assert.notEmpty(orderIds, "销售出库单必须关联平台订单");
		packageMapper.deleteByOutboundOrderId(outboundOrderId);
		boolean sortRequired = orderIds.size() > 1;
		for (Long orderId : orderIds) {
			ErpOrder order = erpOrderService.getById(orderId);
			Assert.notNull(order, "平台订单不存在: " + orderId);
			WmsSalesOutboundPackage pack = new WmsSalesOutboundPackage();
			pack.setOutboundOrderId(outboundOrderId);
			pack.setErpOrderId(orderId);
			pack.setErpTenantId(erpTenantId);
			pack.setPlatform(platform);
			pack.setShopId(order.getShopId());
			pack.setPlatformOrderId(order.getPlatformOrderId());
			pack.setSortStatus(sortRequired ? SORT_PENDING : SORT_NOT_REQUIRED);
			pack.setLabelStatus(StringUtils.hasText(order.getLabelBase64()) ? LABEL_READY : LABEL_NOT_READY);
			boolean handoverRequired = "ozon".equalsIgnoreCase(platform) && ozonActService.isActRequired(order);
			pack.setHandoverRequired(handoverRequired ? 1 : 0);
			pack.setHandoverStatus(handoverRequired ? HANDOVER_PENDING : HANDOVER_NOT_REQUIRED);
			pack.setPackStatus(PACK_PENDING);
			pack.setShipStatus(SHIP_PENDING);
			packageMapper.insert(pack);
		}
	}

	public void deleteByOutboundOrderId(Long outboundOrderId) {
		packageMapper.deleteByOutboundOrderId(outboundOrderId);
	}

	public List<WmsSalesOutboundPackage> listEntities(Long outboundOrderId) {
		return packageMapper.selectByOutboundOrderId(outboundOrderId);
	}

	public List<OutboundPackageVO> listPackages(Long outboundOrderId) {
		List<SalesOutboundOrderItem> outboundItems = outboundItemMapper.selectByOutboundOrderId(outboundOrderId);
		Map<Long, List<SalesOutboundOrderItem>> itemMap = outboundItems.stream()
				.filter(item -> item.getErpOrderId() != null)
				.collect(Collectors.groupingBy(SalesOutboundOrderItem::getErpOrderId,
						LinkedHashMap::new, Collectors.toList()));
		List<OutboundPackageVO> result = new ArrayList<>();
		for (WmsSalesOutboundPackage pack : packageMapper.selectByOutboundOrderId(outboundOrderId)) {
			OutboundPackageVO vo = new OutboundPackageVO();
			vo.setId(pack.getId());
			vo.setOutboundOrderId(pack.getOutboundOrderId());
			vo.setErpOrderId(pack.getErpOrderId());
			vo.setPlatformOrderId(pack.getPlatformOrderId());
			vo.setShopId(pack.getShopId());
			vo.setSortSlotId(pack.getSortSlotId());
			vo.setSortSlotScanCode(pack.getSortSlotScanCode());
			vo.setSortCode(pack.getSortCode());
			vo.setSortStatus(pack.getSortStatus());
			vo.setLabelStatus(pack.getLabelStatus());
			vo.setHandoverRequired(Integer.valueOf(1).equals(pack.getHandoverRequired()));
			vo.setHandoverStatus(pack.getHandoverStatus());
			vo.setPackStatus(pack.getPackStatus());
			vo.setShipStatus(pack.getShipStatus());
			vo.setChannelCode(pack.getChannelCode());
			vo.setChannelName(pack.getChannelName());
			vo.setTrackingNo(pack.getTrackingNo());
			vo.setWeight(pack.getWeight());
			vo.setShippedBy(pack.getShippedBy());
			vo.setShippedByName(pack.getShippedByName());
			vo.setShippedTime(pack.getShippedTime());
			List<PackShipItemVO> items = new ArrayList<>();
			for (SalesOutboundOrderItem source : itemMap.getOrDefault(pack.getErpOrderId(), java.util.Collections.emptyList())) {
				PackShipItemVO item = new PackShipItemVO();
				item.setSkuCode(source.getSkuCode());
				item.setWarehouseSkuCode(warehouseSkuCodeService.build(pack.getErpTenantId(), source.getSkuCode()));
				item.setQty(source.getQuantity());
				item.setSortedQty(source.getSortedQuantity());
				item.setPackedQty(source.getPackedQuantity());
				item.setQuality("GOOD");
				items.add(item);
			}
			vo.setItems(items);
			result.add(vo);
		}
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public List<WmsSalesOutboundPackage> prepareForPicking(Long outboundOrderId, boolean sortRequired) {
		List<WmsSalesOutboundPackage> packages = packageMapper.selectByOutboundOrderId(outboundOrderId);
		for (WmsSalesOutboundPackage pack : packages) {
			pack.setSortSlotId(null);
			pack.setSortSlotScanCode(null);
			pack.setSortCode(null);
			pack.setSortStatus(sortRequired ? SORT_PENDING : SORT_NOT_REQUIRED);
			Assert.isTrue(packageMapper.updateById(pack) == 1, "包裹分货状态初始化冲突，请重试");
		}
		for (SalesOutboundOrderItem item : outboundItemMapper.selectByOutboundOrderId(outboundOrderId)) {
			item.setSortedQuantity(0);
			item.setPackedQuantity(0);
			Assert.isTrue(outboundItemMapper.updateById(item) == 1, "出库商品核对数量初始化失败");
		}
		return packages;
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsSalesOutboundPackage confirmSorted(Long packageId) {
		return confirmSorted(packageId, null);
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsSalesOutboundPackage confirmSorted(Long packageId, String operatorName) {
		return confirmSorted(packageId, null, operatorName);
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsSalesOutboundPackage confirmSorted(Long packageId, Long operatorId, String operatorName) {
		WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(packageId);
		Assert.notNull(pack, "平台订单包裹不存在");
		if (SORT_SORTED.equals(pack.getSortStatus()) || SORT_NOT_REQUIRED.equals(pack.getSortStatus())) {
			return pack;
		}
		Assert.isTrue(SORT_PENDING.equals(pack.getSortStatus()), "平台订单包裹当前不能确认分货");
		List<SalesOutboundOrderItem> items = outboundItemMapper
				.selectPackageItemsForUpdate(pack.getOutboundOrderId(), pack.getErpOrderId());
		Assert.notEmpty(items, "平台订单包裹没有商品明细");
		for (SalesOutboundOrderItem item : items) {
			Assert.isTrue(nvl(item.getSortedQuantity()) == nvl(item.getQuantity()),
					"SKU[" + item.getSkuCode() + "]尚未完成分货核对");
		}
		pack.setSortStatus(SORT_SORTED);
		pack.setSortById(operatorId);
		pack.setSortBy(operatorName);
		pack.setSortTime(LocalDateTime.now());
		Assert.isTrue(packageMapper.updateById(pack) == 1, "包裹分货状态并发冲突，请刷新重试");
		return pack;
	}

	@Transactional(rollbackFor = Exception.class)
	public void scanSort(Long packageId, String scanCode, Integer quantity, boolean manual,
			Long operatorId, String operatorName) {
		scanSort(packageId, scanCode, quantity, manual, null, operatorId, operatorName);
	}

	@Transactional(rollbackFor = Exception.class)
	public void scanSort(Long packageId, String scanCode, Integer quantity, boolean manual,
			String manualReason, Long operatorId, String operatorName) {
		WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(packageId);
		Assert.notNull(pack, "平台订单包裹不存在");
		Assert.isTrue(SORT_PENDING.equals(pack.getSortStatus()), "该包裹当前不需要继续分货");
		scanPackageItem(pack, scanCode, quantity, manual, manualReason, true, operatorId, operatorName);
	}

	@Transactional(rollbackFor = Exception.class)
	public void scanPack(Long packageId, String scanCode, Integer quantity, boolean manual,
			Long operatorId, String operatorName) {
		scanPack(packageId, scanCode, quantity, manual, null, operatorId, operatorName);
	}

	@Transactional(rollbackFor = Exception.class)
	public void scanPack(Long packageId, String scanCode, Integer quantity, boolean manual,
			String manualReason, Long operatorId, String operatorName) {
		WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(packageId);
		Assert.notNull(pack, "平台订单包裹不存在");
		Assert.isTrue(SORT_SORTED.equals(pack.getSortStatus()) || SORT_NOT_REQUIRED.equals(pack.getSortStatus()),
				"平台订单尚未完成分货");
		Assert.isTrue(!PACK_PACKED.equals(pack.getPackStatus()), "该平台订单包裹已经打包");
		scanPackageItem(pack, scanCode, quantity, manual, manualReason, false, operatorId, operatorName);
	}

	public boolean allSorted(Long outboundOrderId) {
		List<WmsSalesOutboundPackage> packages = packageMapper.selectByOutboundOrderId(outboundOrderId);
		return !packages.isEmpty() && packages.stream().allMatch(pack -> SORT_SORTED.equals(pack.getSortStatus())
				|| SORT_NOT_REQUIRED.equals(pack.getSortStatus()));
	}

	@Transactional(rollbackFor = Exception.class)
	public void skipUnstartedSorting(Long outboundOrderId) {
		for (SalesOutboundOrderItem item : outboundItemMapper.selectByOutboundOrderId(outboundOrderId)) {
			Assert.isTrue(nvl(item.getSortedQuantity()) == 0,
					"已经开始格口分货，不能切换为直接打包");
		}
		for (WmsSalesOutboundPackage source : packageMapper.selectByOutboundOrderId(outboundOrderId)) {
			WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(source.getId());
			Assert.notNull(pack, "平台订单包裹不存在");
			Assert.isTrue(SORT_PENDING.equals(pack.getSortStatus()), "平台订单当前不能跳过格口分货");
			pack.setSortCode(null);
			pack.setSortSlotId(null);
			pack.setSortSlotScanCode(null);
			pack.setSortStatus(SORT_NOT_REQUIRED);
			Assert.isTrue(packageMapper.updateById(pack) == 1, "跳过格口分货状态更新冲突，请刷新重试");
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void markLabelReady(Long outboundOrderId, List<Long> readyOrderIds) {
		for (WmsSalesOutboundPackage pack : packageMapper.selectByOutboundOrderId(outboundOrderId)) {
			if (readyOrderIds.contains(pack.getErpOrderId()) && LABEL_NOT_READY.equals(pack.getLabelStatus())) {
				pack.setLabelStatus(LABEL_READY);
				Assert.isTrue(packageMapper.updateById(pack) == 1, "面单状态更新冲突，请刷新重试");
			}
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void confirmExternalDocument(Long packageId) {
		WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(packageId);
		Assert.notNull(pack, "平台订单包裹不存在");
		if (LABEL_ATTACHED_CONFIRMED.equals(pack.getLabelStatus())) {
			return;
		}
		pack.setLabelStatus(LABEL_EXTERNAL_CONFIRMED);
		Assert.isTrue(packageMapper.updateById(pack) == 1, "外部资料确认冲突，请刷新重试");
	}

	@Transactional(rollbackFor = Exception.class)
	public void confirmLabelAttached(Long packageId, String scanCode, Long operatorId, String operatorName) {
		WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(packageId);
		Assert.notNull(pack, "平台订单包裹不存在");
		if (LABEL_ATTACHED_CONFIRMED.equals(pack.getLabelStatus())) {
			return;
		}
		Assert.isTrue(LABEL_READY.equals(pack.getLabelStatus())
						|| LABEL_EXTERNAL_CONFIRMED.equals(pack.getLabelStatus()),
				"平台面单尚未生成或核对");
		Assert.hasText(scanCode, "面单扫描码不能为空");
		Assert.isTrue(labelVerificationService.matches(pack, scanCode),
				"扫描面单与当前平台订单不匹配");
		pack.setLabelStatus(LABEL_ATTACHED_CONFIRMED);
		Assert.isTrue(packageMapper.updateById(pack) == 1, "面单贴附状态更新冲突，请刷新重试");

		WmsOutboundScanEvent event = new WmsOutboundScanEvent();
		event.setPackageId(pack.getId());
		event.setStage("LABEL");
		event.setEventType("SCAN");
		event.setScanCode(scanCode.trim());
		event.setOperatorId(operatorId);
		event.setOperatorName(operatorName);
		event.setRemark("平台面单已贴附并完成订单匹配");
		scanEventMapper.insert(event);
	}

	@Transactional(rollbackFor = Exception.class)
	public void confirmExternalHandover(Long packageId) {
		WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(packageId);
		Assert.notNull(pack, "平台订单包裹不存在");
		Assert.isTrue(Integer.valueOf(1).equals(pack.getHandoverRequired()), "该平台订单无需交接单");
		pack.setHandoverStatus(HANDOVER_EXTERNAL_CONFIRMED);
		Assert.isTrue(packageMapper.updateById(pack) == 1, "外部交接单确认冲突，请刷新重试");
	}

	@Transactional(rollbackFor = Exception.class)
	public void markHandoverReady(List<Long> readyOrderIds) {
		if (readyOrderIds == null || readyOrderIds.isEmpty()) {
			return;
		}
		for (WmsSalesOutboundPackage pack : packageMapper.selectList(
				com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(WmsSalesOutboundPackage.class)
						.in(WmsSalesOutboundPackage::getErpOrderId, readyOrderIds))) {
			if (Integer.valueOf(1).equals(pack.getHandoverRequired())
					&& !HANDOVER_READY.equals(pack.getHandoverStatus())) {
				pack.setHandoverStatus(HANDOVER_READY);
				Assert.isTrue(packageMapper.updateById(pack) == 1, "交接单状态更新冲突，请刷新重试");
			}
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsSalesOutboundPackage packPackage(Long packageId, String packerName, String documentMode) {
		return packPackage(packageId, null, packerName, documentMode);
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsSalesOutboundPackage packPackage(Long packageId, Long packerId, String packerName, String documentMode) {
		WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(packageId);
		Assert.notNull(pack, "平台订单包裹不存在");
		Assert.isTrue(SORT_SORTED.equals(pack.getSortStatus()) || SORT_NOT_REQUIRED.equals(pack.getSortStatus()),
				"平台订单尚未完成分货");
		if (PACK_PACKED.equals(pack.getPackStatus())) {
			return pack;
		}
		Assert.isTrue(LABEL_ATTACHED_CONFIRMED.equals(pack.getLabelStatus()),
				"请先扫描并确认当前平台订单面单已经贴到包裹");
		List<SalesOutboundOrderItem> items = outboundItemMapper
				.selectPackageItemsForUpdate(pack.getOutboundOrderId(), pack.getErpOrderId());
		Assert.notEmpty(items, "平台订单包裹没有商品明细");
		for (SalesOutboundOrderItem item : items) {
			Assert.isTrue(nvl(item.getPackedQuantity()) == nvl(item.getQuantity()),
					"SKU[" + item.getSkuCode() + "]尚未完成打包扫码复核");
		}
		pack.setPackStatus(PACK_PACKED);
		pack.setPackerId(packerId);
		pack.setPackerName(packerName);
		pack.setPackTime(LocalDateTime.now());
		Assert.isTrue(packageMapper.updateById(pack) == 1, "包裹打包状态并发冲突，请刷新重试");
		return pack;
	}

	private void scanPackageItem(WmsSalesOutboundPackage pack, String scanCode, Integer quantity,
			boolean manual, String manualReason, boolean sorting, Long operatorId, String operatorName) {
		Assert.hasText(scanCode, "扫描码不能为空");
		Assert.isTrue(quantity != null && quantity > 0, "扫描数量必须大于0");
		if (manual) {
			Assert.hasText(manualReason, "手工登记必须填写原因");
		}
		List<SalesOutboundOrderItem> items = outboundItemMapper
				.selectPackageItemsForUpdate(pack.getOutboundOrderId(), pack.getErpOrderId());
		Assert.notEmpty(items, "平台订单包裹没有商品明细");
		List<SalesOutboundOrderItem> matches = items.stream()
				.filter(item -> skuBarcodeService.matches(pack.getErpTenantId(), item.getSkuCode(), scanCode))
				.collect(Collectors.toList());
		Assert.notEmpty(matches, "扫描商品不属于当前平台订单包裹: " + scanCode);
		int remainingCapacity = matches.stream().mapToInt(item -> {
			int current = sorting ? nvl(item.getSortedQuantity()) : nvl(item.getPackedQuantity());
			return Math.max(0, nvl(item.getQuantity()) - current);
		}).sum();
		Assert.isTrue(quantity <= remainingCapacity, "SKU扫描数量超过当前包裹剩余应有数量");

		int remaining = quantity;
		for (SalesOutboundOrderItem target : matches) {
			int current = sorting ? nvl(target.getSortedQuantity()) : nvl(target.getPackedQuantity());
			int allocation = Math.min(remaining, Math.max(0, nvl(target.getQuantity()) - current));
			if (allocation <= 0) {
				continue;
			}
			if (sorting) {
				target.setSortedQuantity(current + allocation);
			}
			else {
				target.setPackedQuantity(current + allocation);
			}
			Assert.isTrue(outboundItemMapper.updateById(target) == 1, "包裹商品核对状态更新失败，请重试");
			recordScanEvent(pack, scanCode, allocation, manual, sorting, operatorId, operatorName,
					target.getSkuCode(), manualReason);
			remaining -= allocation;
			if (remaining == 0) {
				break;
			}
		}
	}

	private void recordScanEvent(WmsSalesOutboundPackage pack, String scanCode, int quantity,
			boolean manual, boolean sorting, Long operatorId, String operatorName, String skuCode,
			String manualReason) {
		WmsOutboundScanEvent event = new WmsOutboundScanEvent();
		event.setPackageId(pack.getId());
		event.setStage(sorting ? "SORT" : "PACK");
		event.setEventType(manual ? "MANUAL" : "SCAN");
		event.setScanCode(scanCode.trim());
		event.setSkuCode(skuCode);
		event.setQuantity(quantity);
		event.setOperatorId(operatorId);
		event.setOperatorName(operatorName);
		event.setRemark(manualReason);
		scanEventMapper.insert(event);
	}

	private static int nvl(Integer value) {
		return value == null ? 0 : value;
	}

	public boolean allPacked(Long outboundOrderId) {
		List<WmsSalesOutboundPackage> packages = packageMapper.selectByOutboundOrderId(outboundOrderId);
		return !packages.isEmpty() && packages.stream().allMatch(pack -> PACK_PACKED.equals(pack.getPackStatus()));
	}

	public WmsSalesOutboundPackage getForUpdate(Long packageId) {
		return packageMapper.selectByIdForUpdate(packageId);
	}

	@Transactional(rollbackFor = Exception.class)
	public void markShipped(Long packageId, String channelCode, String channelName, String trackingNo,
			java.math.BigDecimal weight, Long operatorId, String operatorName) {
		WmsSalesOutboundPackage pack = packageMapper.selectByIdForUpdate(packageId);
		Assert.notNull(pack, "平台订单包裹不存在");
		Assert.isTrue(!SHIP_SHIPPED.equals(pack.getShipStatus()), "该平台订单包裹已经签出，请勿重复操作");
		Assert.isTrue(PACK_PACKED.equals(pack.getPackStatus()), "仅已完成打包的包裹可以签出");
		pack.setShipStatus(SHIP_SHIPPED);
		pack.setChannelCode(channelCode);
		pack.setChannelName(channelName);
		pack.setTrackingNo(trackingNo);
		pack.setWeight(weight);
		pack.setShippedBy(operatorId);
		pack.setShippedByName(operatorName);
		pack.setShippedTime(LocalDateTime.now());
		Assert.isTrue(packageMapper.updateById(pack) == 1, "包裹签出状态更新冲突，请刷新重试");
	}

	public boolean allShipped(Long outboundOrderId) {
		List<WmsSalesOutboundPackage> packages = packageMapper.selectByOutboundOrderId(outboundOrderId);
		return !packages.isEmpty() && packages.stream()
				.allMatch(pack -> SHIP_SHIPPED.equals(pack.getShipStatus()));
	}

	public boolean anyShipped(Long outboundOrderId) {
		return packageMapper.selectByOutboundOrderId(outboundOrderId).stream()
				.anyMatch(pack -> SHIP_SHIPPED.equals(pack.getShipStatus()));
	}

}
