package com.erp.admin.wms.pickpackage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskOrder;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskDetailVO;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskOrderVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.Assert;

/** Immutable, canonical input used by both exported packages. */
@Data
public class PickTaskPackageSnapshot {

	private static final List<String> SUPPORTED_PLATFORMS =
			Arrays.asList("OZON", "WB", "YANDEX", "MANUAL");

	private final Long taskId;
	private final String taskNo;
	private final Long warehouseId;
	private final Long operatorId;
	private final List<Order> orders;
	private final List<ExcludedOrder> excludedOrders;
	private final int totalQuantity;
	private final String snapshotHash;

	public static PickTaskPackageSnapshot from(FulfillmentPickTaskDetailVO detail) {
		Assert.notNull(detail, "拣货任务详情不能为空");
		WmsFulfillmentPickTask task = detail.getTask();
		Assert.notNull(task, "拣货任务不存在");
		Assert.hasText(task.getTaskNo(), "拣货任务号不能为空");
		List<Order> included = new ArrayList<>();
		List<ExcludedOrder> excluded = new ArrayList<>();
		for (FulfillmentPickTaskOrderVO row : safe(detail.getOrderQueue())) {
			Assert.notNull(row.getTaskOrder(), "任务订单关系不能为空");
			WmsFulfillmentPickTaskOrder taskOrder = row.getTaskOrder();
			WmsFulfillmentOrder source = row.getFulfillmentOrder();
			if ("CANCELLED".equalsIgnoreCase(taskOrder.getOrderStatus())) {
				excluded.add(new ExcludedOrder(taskOrder.getFulfillmentOrderId(),
						source == null ? null : source.getSourceOrderNo(), "CANCELLED"));
				continue;
			}
			Assert.notNull(source, "履约订单不存在");
			String platform = upper(source.getSourceType());
			Assert.isTrue(SUPPORTED_PLATFORMS.contains(platform),
					"暂不支持的平台：" + platform);
			List<Line> lines = new ArrayList<>();
			for (WmsFulfillmentPickTaskLine line : safe(row.getRouteLines())) {
				int quantity = line.getPlannedQuantity() == null ? 0 : line.getPlannedQuantity();
				Assert.isTrue(quantity > 0, "拣货明细数量必须大于0");
				lines.add(new Line(value(line.getLocationCode()), value(line.getWarehouseSkuCode()),
						value(line.getSkuCode()), quantity));
			}
			Assert.notEmpty(lines, "订单没有可导出的拣货明细：" + source.getSourceOrderNo());
			lines.sort(Comparator.comparing(Line::getLocationCode)
					.thenComparing(Line::getWarehouseSkuCode).thenComparing(Line::getSkuCode));
			String shopCode = source.getShopId() == null ? "SHOP-UNKNOWN" : "SHOP-" + source.getShopId();
			included.add(new Order(source.getId(), value(source.getSourceOrderNo()), platform,
					source.getShopId(), shopCode, lines));
		}
		Assert.notEmpty(included, "拣货任务没有可导出的订单");
		included.sort(Comparator.comparing(Order::getPlatform).thenComparing(Order::getShopCode)
				.thenComparing(Order::getSourceOrderNo).thenComparing(Order::getFulfillmentOrderId));
		excluded.sort(Comparator.comparing(ExcludedOrder::getFulfillmentOrderId,
				Comparator.nullsLast(Long::compareTo)));
		int quantity = included.stream().flatMap(order -> order.getLines().stream())
				.mapToInt(Line::getQuantity).sum();
		String canonical = canonical(task, included, excluded);
		return new PickTaskPackageSnapshot(task.getId(), task.getTaskNo(), task.getWarehouseId(),
				task.getOperatorId(), Collections.unmodifiableList(included),
				Collections.unmodifiableList(excluded), quantity, sha256(canonical));
	}

	private static String canonical(WmsFulfillmentPickTask task, List<Order> orders,
			List<ExcludedOrder> excluded) {
		StringBuilder text = new StringBuilder().append(task.getId()).append('|')
				.append(task.getTaskNo()).append('|').append(task.getWarehouseId()).append('|');
		for (Order order : orders) {
			text.append(order.fulfillmentOrderId).append('|').append(order.platform).append('|')
					.append(order.shopCode).append('|').append(order.sourceOrderNo).append('|');
			for (Line line : order.lines) {
				text.append(line.locationCode).append('|').append(line.warehouseSkuCode).append('|')
						.append(line.skuCode).append('|').append(line.quantity).append(';');
			}
		}
		for (ExcludedOrder order : excluded) {
			text.append("EX|").append(order.fulfillmentOrderId).append('|')
					.append(order.sourceOrderNo).append('|').append(order.reason).append(';');
		}
		return text.toString();
	}

	private static String sha256(String text) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder value = new StringBuilder();
			for (byte item : digest) value.append(String.format("%02x", item));
			return value.toString();
		}
		catch (Exception ex) {
			throw new IllegalStateException("无法生成任务快照摘要", ex);
		}
	}

	private static String upper(String value) {
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}

	private static String value(String value) {
		return value == null ? "" : value.trim();
	}

	private static <T> List<T> safe(List<T> value) {
		return value == null ? Collections.emptyList() : value;
	}

	@Data
	@AllArgsConstructor
	public static class Order {
		private Long fulfillmentOrderId;
		private String sourceOrderNo;
		private String platform;
		private Long shopId;
		private String shopCode;
		private List<Line> lines;
	}

	@Data
	@AllArgsConstructor
	public static class Line {
		private String locationCode;
		private String warehouseSkuCode;
		private String skuCode;
		private int quantity;
	}

	@Data
	@AllArgsConstructor
	public static class ExcludedOrder {
		private Long fulfillmentOrderId;
		private String sourceOrderNo;
		private String reason;
	}
}
