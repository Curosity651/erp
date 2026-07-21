package com.erp.admin.order.service.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.erp.admin.common.util.TimeZoneUtils;
import com.erp.admin.order.mapper.ErpOrderItemMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.system.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单 Upsert 通用工具
 * <p>
 * 提供汇率批量获取、单币种转换、时间字段设置等细粒度原子方法。
 * 各平台 UpsertService 按需组合调用。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderUpsertHelper {

	private final ExchangeRateService exchangeRateService;
	private final ErpOrderItemMapper orderItemMapper;

	/**
	 * 批量获取汇率
	 *
	 * @param currencies   需要的币种集合
	 * @param baseCurrency 基准币种（通常 "CNY"）
	 * @param rateDate     汇率日期
	 * @return Map&lt;币种, 对基准币种的汇率&gt;，空参数返回空 Map
	 */
	public Map<String, BigDecimal> batchGetRates(Set<String> currencies, String baseCurrency, LocalDate rateDate) {
		if (currencies == null || currencies.isEmpty() || rateDate == null) {
			return Collections.emptyMap();
		}
		return exchangeRateService.batchGetOrSyncRates(currencies, baseCurrency, rateDate);
	}

	/**
	 * 跨币种转换（amount * fromRate / toRate）
	 *
	 * @param amount   原始金额
	 * @param fromRate 原始币种对基准币种的汇率
	 * @param toRate   目标币种对基准币种的汇率
	 * @return 转换后金额，任一参数无效返回 null
	 */
	public BigDecimal convert(BigDecimal amount, BigDecimal fromRate, BigDecimal toRate) {
		if (amount == null || fromRate == null || toRate == null || toRate.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		return amount.multiply(fromRate).divide(toRate, 6, RoundingMode.HALF_UP);
	}

	/**
	 * 直接乘以汇率（amount * rate）
	 */
	public BigDecimal convertDirect(BigDecimal amount, BigDecimal rate) {
		if (amount == null || rate == null) {
			return null;
		}
		return amount.multiply(rate);
	}

	/**
	 * 设置订单时间字段（平台创建时间 + 莫斯科时间 + 同步时间）
	 */
	public void setTimeFields(ErpOrder order, LocalDateTime platformCreatedAt) {
		order.setPlatformCreatedAt(platformCreatedAt);
		if (platformCreatedAt != null) {
			order.setPlatformCreatedAtMoscow(TimeZoneUtils.toMoscowTime(platformCreatedAt));
		}
		order.setSyncedAt(LocalDateTime.now());
	}

	/**
	 * 设置审计字段（createTime / updateTime）
	 */
	public void setAuditFields(ErpOrder order, boolean isNew) {
		LocalDateTime now = LocalDateTime.now();
		if (isNew) {
			order.setCreateTime(now);
		}
		order.setUpdateTime(now);
	}

	/**
	 * 智能合并订单明细（Merge Upsert）
	 * <p>
	 * 以 orderId + platformItemId 为匹配键，对比 DB 现有与新同步的 items，
	 * 精确执行 INSERT / UPDATE / DELETE，保留 ERP 内部字段（如 returnedQuantity）。
	 *
	 * @param orderId       订单 ID
	 * @param incomingItems 平台同步的最新 items（orderId 可未设置，方法内会统一设置）
	 */
	public void mergeItems(Long orderId, List<ErpOrderItem> incomingItems) {
		if (incomingItems == null) {
			incomingItems = Collections.emptyList();
		}

		// 1. 加载 DB 现有 items，以 platformItemId 为 key 构建索引（重复 key 清理冗余）
		List<ErpOrderItem> existingItems = orderItemMapper.selectByOrderId(orderId);
		HashMap<String, ErpOrderItem> existingMap = new HashMap<>();
		for (ErpOrderItem item : existingItems) {
			ErpOrderItem prev = existingMap.put(item.getPlatformItemId(), item);
			if (prev != null) {
				log.warn("[MERGE] orderId={} 存在重复 platformItemId={}，删除冗余 id={}",
						orderId, item.getPlatformItemId(), prev.getId());
				orderItemMapper.deleteById(prev.getId());
			}
		}

		// 2. 遍历 incoming，匹配后 INSERT 或 UPDATE
		LocalDateTime now = LocalDateTime.now();
		for (ErpOrderItem incoming : incomingItems) {
			if (incoming.getPlatformItemId() == null) {
				log.error("[MERGE] orderId={} incoming item 缺少 platformItemId，跳过", orderId);
				continue;
			}
			incoming.setOrderId(orderId);
			ErpOrderItem existing = existingMap.remove(incoming.getPlatformItemId());

			if (existing == null) {
				// 新增
				incoming.setCreateTime(now);
				incoming.setUpdateTime(now);
				orderItemMapper.insert(incoming);
			} else {
				// 已存在 → 检测是否有变化
				incoming.setId(existing.getId());
				if (isPlatformFieldsChanged(existing, incoming)) {
					incoming.setUpdateTime(now);
					orderItemMapper.updatePlatformFields(incoming);
				}
			}
		}

		// 3. 删除 DB 中有但 incoming 中没有的（平台侧已移除的 item）
		for (ErpOrderItem orphan : existingMap.values()) {
			if (orphan.getReturnedQuantity() != null && orphan.getReturnedQuantity() > 0) {
				log.warn("[MERGE] orderId={} 删除含退货数量的 item: id={}, platformItemId={}, returnedQty={}",
						orderId, orphan.getId(), orphan.getPlatformItemId(), orphan.getReturnedQuantity());
			}
			orderItemMapper.deleteById(orphan.getId());
		}
	}

	/**
	 * 比对平台同步字段是否发生变化
	 */
	private boolean isPlatformFieldsChanged(ErpOrderItem existing, ErpOrderItem incoming) {
		if (!Objects.equals(existing.getQuantity(), incoming.getQuantity())) return true;
		if (!bdEquals(existing.getItemPrice(), incoming.getItemPrice())) return true;
		if (!bdEquals(existing.getItemAmount(), incoming.getItemAmount())) return true;
		if (!bdEquals(existing.getItemAmountRub(), incoming.getItemAmountRub())) return true;
		return false;
	}

	/**
	 * BigDecimal 安全比较（null 安全，使用 compareTo 而非 equals 避免 scale 差异误判）
	 */
	private boolean bdEquals(BigDecimal a, BigDecimal b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;
		return a.compareTo(b) == 0;
	}

}
