package com.erp.admin.order.service.wildberries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.dto.wildberries.WbOrderSyncDTO;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.service.OrderLifecycleService;
import com.erp.admin.order.service.common.OrderUpsertHelper;
import org.springframework.dao.DuplicateKeyException;
import com.erp.admin.order.service.wildberries.converter.WildberriesOrderStatusConverter;
import com.erp.admin.order.util.CurrencyCodeConverter;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.wildberries.enums.WildberriesWbStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Wildberries 订单 Upsert 服务
 * <p>
 * 负责订单的插入和更新持久化。
 * 事务语义：insert/update + 库存过账在同一事务内，原子性保证。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbOrderUpsertService {

	private final ErpOrderMapper orderMapper;
	private final OrderUpsertHelper upsertHelper;
	private final OrderLifecycleService lifecycleService;

	/**
	 * Upsert 订单（插入或更新）
	 * <p>
	 * 事务范围覆盖：字段映射 → insert/update → 预占/释放过账
	 */
	@Transactional(rollbackFor = Exception.class)
	public ErpOrder upsertOrder(Long shopId, WbOrderSyncDTO dto) {
		// 查询是否存在
		ErpOrder existing = orderMapper.selectOneByShopPlatformPlatformOrderId(
				shopId, PlatformEnum.Wildberries.code(), dto.getOrderId());

		String oldErpStatus = existing != null ? existing.getErpStatus() : null;
		boolean isNew = (existing == null);

		ErpOrder po = isNew ? new ErpOrder() : existing;

		// 基本信息
		if (isNew) {
			po.setShopId(shopId);
			po.setPlatform(PlatformEnum.Wildberries.code());
			po.setPlatformOrderId(dto.getOrderId());
		}

		// 状态
		String wbStatus = dto.getWbStatus();
		String supplierStatus = dto.getSupplierStatus();
		if (!StringUtils.hasText(wbStatus)) {
			wbStatus = existing != null && StringUtils.hasText(existing.getPlatformStatus())
					? existing.getPlatformStatus()
					: WildberriesWbStatusEnum.WAITING.getCode();
		}
		po.setPlatformStatus(wbStatus);
		if (StringUtils.hasText(supplierStatus)) {
			po.setPlatformSubstatus(supplierStatus);
		} else if (existing != null && StringUtils.hasText(existing.getPlatformSubstatus())) {
			po.setPlatformSubstatus(existing.getPlatformSubstatus());
		}
		po.setErpStatus(WildberriesOrderStatusConverter.toErpStatus(po.getPlatformSubstatus(), wbStatus));

		// 字段映射
		po.setShipmentId(dto.getSupplyId());
		po.setWarehouseId(dto.getWarehouseId());
		po.setDestinationWarehouseId(dto.getOfficeId());
		po.setFulfillmentType(dto.getDeliveryType());
		upsertHelper.setTimeFields(po, dto.getCreatedAt());

		// 货币代码转换
		String currencyCode = CurrencyCodeConverter.toAlphaCode(Integer.valueOf(dto.getCurrencyCode()));
		String convertedCurrencyCode = CurrencyCodeConverter.toAlphaCode(Integer.valueOf(dto.getConvertedCurrencyCode()));
		po.setCurrencyCode(currencyCode);
		po.setConvertedCurrencyCode(convertedCurrencyCode);

		BigDecimal totalAmount = safeAmount(dto.getPrice());
		BigDecimal convertedAmount = safeAmount(dto.getConvertedPrice());
		po.setTotalAmount(totalAmount);
		po.setProductTotalAmount(totalAmount);
		po.setProductCurrencyCode(currencyCode);
		po.setConvertedAmount(convertedAmount);

		// 汇率换算（使用 OrderUpsertHelper 细粒度方法）
		Map<String, BigDecimal> rates = Collections.emptyMap();
		if (dto.getCreatedAt() != null && totalAmount != null) {
			LocalDate rateDate = dto.getCreatedAt().toLocalDate();
			Set<String> needRates = new HashSet<>();

			if ("RUB".equals(currencyCode)) {
				po.setTotalAmountRub(totalAmount);
			} else {
				needRates.add(currencyCode);
				needRates.add("RUB");
			}

			if (convertedAmount != null && convertedCurrencyCode != null) {
				if ("CNY".equals(convertedCurrencyCode)) {
					po.setConvertedAmount(convertedAmount);
					po.setConvertedCurrencyCode("CNY");
				} else {
					needRates.add(convertedCurrencyCode);
				}
			}

			if (!needRates.isEmpty()) {
				needRates.add("CNY");
				rates = upsertHelper.batchGetRates(needRates, "CNY", rateDate);

				if (convertedAmount != null && !"CNY".equals(convertedCurrencyCode)) {
					BigDecimal rate = rates.get(convertedCurrencyCode);
					if (rate != null) {
						po.setConvertedAmount(upsertHelper.convertDirect(convertedAmount, rate));
						po.setConvertedCurrencyCode("CNY");
					} else {
						log.warn("[WB] 未找到汇率 {} -> CNY，orderId: {}", convertedCurrencyCode, dto.getOrderId());
					}
				}

				if (!"RUB".equals(currencyCode)) {
					BigDecimal orderRate = rates.get(currencyCode);
					BigDecimal rubRate = rates.get("RUB");
					if (orderRate != null && rubRate != null && rubRate.compareTo(BigDecimal.ZERO) > 0) {
						po.setTotalAmountRub(upsertHelper.convert(totalAmount, orderRate, rubRate));
					} else {
						log.warn("[WB] 无法计算 totalAmountRub，orderId: {}", dto.getOrderId());
					}
				}
			}
		}

		po.setComment(dto.getComment());
		po.setRawJson(dto.getRawJson());

		// 持久化 + 副作用（同一事务）
		if (isNew) {
			upsertHelper.setAuditFields(po, true);
			try {
				orderMapper.insert(po);
				buildAndSaveItems(po, dto.getArticle(), currencyCode, rates);
				log.info("[WB] 订单插入成功 orderId={} platformOrderId={}", po.getId(), dto.getOrderId());
				lifecycleService.onOrderCreated(po);
			} catch (DuplicateKeyException e) {
				// 并发插入冲突，回退为更新（参考 WbSupplyService.insertSupplyKnownAbsent）
				log.info("[WB] 订单并发插入冲突，回退更新: platformOrderId={}", dto.getOrderId());
				ErpOrder conflict = orderMapper.selectOneByShopPlatformPlatformOrderId(
						shopId, PlatformEnum.Wildberries.code(), dto.getOrderId());
				if (conflict != null) {
					po.setId(conflict.getId());
					upsertHelper.setAuditFields(po, false);
					orderMapper.updateById(po);
					buildAndSaveItems(po, dto.getArticle(), currencyCode, rates);
					lifecycleService.onStatusChanged(po, conflict.getErpStatus(), po.getErpStatus());
				} else {
					throw e;
				}
			}
		} else {
			upsertHelper.setAuditFields(po, false);
			orderMapper.updateById(po);
			buildAndSaveItems(po, dto.getArticle(), currencyCode, rates);
			log.debug("[WB] 订单更新成功 orderId={} platformOrderId={}", po.getId(), dto.getOrderId());
			lifecycleService.onStatusChanged(po, oldErpStatus, po.getErpStatus());
		}

		return po;
	}

	/**
	 * 构建并持久化订单商品明细
	 * <p>
	 * WB 订单始终单商品，quantity 固定为 1。
	 */
	private void buildAndSaveItems(ErpOrder po, String article,
								   String currencyCode, Map<String, BigDecimal> rates) {
		ErpOrderItem item = new ErpOrderItem();
		item.setOrderId(po.getId());
		item.setPlatformItemId(article);
		item.setQuantity(1);
		item.setItemPrice(po.getTotalAmount());
		item.setItemAmount(po.getTotalAmount());

		// item_amount_rub：逐 item 独立换算
		if ("RUB".equals(currencyCode)) {
			item.setItemAmountRub(po.getTotalAmount());
		} else if (rates != null && !rates.isEmpty()) {
			BigDecimal fromRate = rates.get(currencyCode);
			BigDecimal rubRate = rates.get("RUB");
			item.setItemAmountRub(upsertHelper.convert(po.getTotalAmount(), fromRate, rubRate));
		}

		upsertHelper.mergeItems(po.getId(), Collections.singletonList(item));
		po.setItems(Collections.singletonList(item));

		// 汇总字段
		po.setTotalQuantity(1);
		po.setSkuCount(1);
		orderMapper.updateById(po);
	}

	private BigDecimal safeAmount(Integer fen) {
		if (fen == null) return null;
		return new BigDecimal(fen);
	}

}
