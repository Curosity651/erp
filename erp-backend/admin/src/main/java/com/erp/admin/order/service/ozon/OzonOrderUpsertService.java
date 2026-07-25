package com.erp.admin.order.service.ozon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.dto.ozon.OzonPostingSyncDTO;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.order.service.OrderLifecycleService;
import com.erp.admin.order.service.common.OrderUpsertHelper;
import com.erp.admin.order.service.ozon.converter.OzonOrderStatusConverter;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.ozon.model.response.posting.OzonProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ozon 订单 Upsert 服务
 * <p>
 * 事务语义：insert/update + 库存过账在同一事务内。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OzonOrderUpsertService {

	private final ErpOrderMapper orderMapper;
	private final OrderUpsertHelper upsertHelper;
	private final OrderLifecycleService lifecycleService;

	/**
	 * Upsert 订单
	 */
	@Transactional(rollbackFor = Exception.class)
	public ErpOrder upsertOrder(Long shopId, OzonPostingSyncDTO dto) {
		// 1. 验证商品数量
		if (dto.getProducts() == null || dto.getProducts().isEmpty()) {
			throw new IllegalArgumentException(String.format(
					"[OZON] 订单商品列表为空: posting_number=%s", dto.getPostingNumber()));
		}
		// 2. 查询是否存在（按 店铺+平台+平台单号，与 WB/Yandex 一致，避免同租户多店同号 posting 互相覆盖归属）
		ErpOrder existing = orderMapper.selectOneByShopPlatformPlatformOrderId(
				shopId, PlatformEnum.Ozon.code(), dto.getPostingNumber());

		String oldErpStatus = existing != null ? existing.getErpStatus() : null;
		boolean isNew = (existing == null);

		// 2.5 状态映射（null=不可映射：新订单跳过落库，已存在订单保持原 ERP 状态）
		ErpOrderStatusEnum erpStatusEnum = OzonOrderStatusConverter.toErpStatus(dto.getStatus(), dto.getSubstatus());
		if (erpStatusEnum == null && isNew) {
			log.warn("[OZON] 新订单状态不可映射，跳过落库: posting_number={}, status={}, substatus={}",
					dto.getPostingNumber(), dto.getStatus(), dto.getSubstatus());
			return null;
		}

		ErpOrder order = isNew ? new ErpOrder() : existing;

		// 3. 基本信息
		order.setPlatform(PlatformEnum.Ozon.code());
		order.setPlatformOrderId(dto.getPostingNumber());
		order.setShipmentId(dto.getPostingNumber());
		order.setShopId(shopId);
		order.setFulfillmentType(dto.getFulfillmentType());

		// 4. 商品总价（遍历所有 products 计算 SUM，使用商品级独立单价）
		BigDecimal productTotalAmount = BigDecimal.ZERO;
		for (OzonProduct product : dto.getProducts()) {
			BigDecimal price = new BigDecimal(product.getPrice())
					.multiply(new BigDecimal("100"));
			BigDecimal amount = price.multiply(new BigDecimal(product.getQuantity()));
			productTotalAmount = productTotalAmount.add(amount);
		}
		order.setProductTotalAmount(productTotalAmount);
		order.setProductCurrencyCode(dto.getProductCurrencyCode());

		// 5. 订单总金额（posting 级别总价 × 100 转分）
		BigDecimal totalAmount = null;
		if (dto.getCustomerPrice() != null && !dto.getCustomerPrice().isEmpty()) {
			totalAmount = new BigDecimal(dto.getCustomerPrice())
					.multiply(new BigDecimal("100"));
		} else {
			log.warn("[OZON] 订单缺少 customer_price: posting_number={}", dto.getPostingNumber());
		}
		order.setTotalAmount(totalAmount);
		order.setCurrencyCode(dto.getCustomerPriceCurrencyCode());

		// 6. 汇率转换（inProcessAt 可能为空，兜底用当天）
		LocalDate rateDate = dto.getInProcessAt() != null ? dto.getInProcessAt().toLocalDate() : LocalDate.now();
		Set<String> currencies = new HashSet<>();
		currencies.add(order.getProductCurrencyCode());
		if (order.getCurrencyCode() != null) {
			currencies.add(order.getCurrencyCode());
		}
		currencies.add("RUB");

		Map<String, BigDecimal> rates = upsertHelper.batchGetRates(currencies, "CNY", rateDate);

		// 商品总价 → CNY
		BigDecimal productRateToCny = rates.get(order.getProductCurrencyCode());
		if (productRateToCny != null) {
			order.setProductAmountCny(upsertHelper.convertDirect(productTotalAmount, productRateToCny));
		}

		// 订单总金额
		if (totalAmount != null && order.getCurrencyCode() != null) {
			BigDecimal orderRateToCny = rates.get(order.getCurrencyCode());
			if (orderRateToCny != null) {
				order.setConvertedAmount(upsertHelper.convertDirect(totalAmount, orderRateToCny));
				order.setConvertedCurrencyCode("CNY");

				if ("RUB".equals(order.getCurrencyCode())) {
					order.setTotalAmountRub(totalAmount);
				} else {
					BigDecimal rubRateToCny = rates.get("RUB");
					if (rubRateToCny != null) {
						order.setTotalAmountRub(upsertHelper.convert(totalAmount, orderRateToCny, rubRateToCny));
					}
				}
			}
		}

		// 7. 状态（平台状态照抄；ERP 状态仅在可映射时更新，null 保持原状态）
		order.setPlatformStatus(dto.getStatus());
		order.setPlatformSubstatus(dto.getSubstatus());
		if (erpStatusEnum != null) {
			order.setErpStatus(erpStatusEnum.name());
		}

		// 8. 仓库和配送
		order.setWarehouseId(dto.getWarehouseId() != null ? dto.getWarehouseId().toString() : null);
		// 仅 FBS 有发货仓库名；用于大仓/小仓判定（含「大」字=大仓，只有大仓需生成运单）
		order.setWarehouseName(dto.getWarehouseName());
		order.setDeliveryMethodId(dto.getDeliveryMethodId());
		order.setDeliveryMethodName(dto.getDeliveryMethodName());
		order.setDestinationWarehouseId(
				dto.getDestinationWarehouseId() != null ? dto.getDestinationWarehouseId().toString() : null);

		// 9. 其他字段
		order.setComment(dto.getComment());
		order.setRawJson(dto.getRawJson());

		// 10. 时间（inProcessAt 可能为空）
		upsertHelper.setTimeFields(order,
				dto.getInProcessAt() != null ? dto.getInProcessAt().toLocalDateTime() : null);

		// 11. 持久化 + 副作用（同一事务）
		if (isNew) {
			upsertHelper.setAuditFields(order, true);
			orderMapper.insert(order);
			buildAndSaveItems(order, dto, order.getProductCurrencyCode(), rates);
			log.info("[OZON] 订单插入成功 orderId={} postingNumber={}", order.getId(), dto.getPostingNumber());
			lifecycleService.onOrderCreated(order);
		} else {
			upsertHelper.setAuditFields(order, false);
			orderMapper.updateById(order);
			buildAndSaveItems(order, dto, order.getProductCurrencyCode(), rates);
			log.info("[OZON] 订单更新成功 orderId={} postingNumber={}", order.getId(), dto.getPostingNumber());
			lifecycleService.onStatusChanged(order, oldErpStatus, order.getErpStatus());
		}

		return order;
	}

	/**
	 * 构建并持久化订单商品明细
	 * <p>
	 * 遍历 Ozon products 列表，使用商品级独立单价 product.getPrice()。
	 */
	private void buildAndSaveItems(ErpOrder order, OzonPostingSyncDTO dto,
								   String productCurrencyCode, Map<String, BigDecimal> rates) {
		List<ErpOrderItem> items = new ArrayList<>();
		for (OzonProduct product : dto.getProducts()) {
			ErpOrderItem item = new ErpOrderItem();
			item.setOrderId(order.getId());
			item.setPlatformItemId(product.getOfferId());
			item.setQuantity(product.getQuantity());

			BigDecimal price = new BigDecimal(product.getPrice())
					.multiply(new BigDecimal("100"));
			item.setItemPrice(price);
			item.setItemAmount(price.multiply(new BigDecimal(product.getQuantity())));

			// item_amount_rub：逐 item 独立换算（使用商品币种，非订单币种）
			if ("RUB".equals(productCurrencyCode)) {
				item.setItemAmountRub(item.getItemAmount());
			} else if (rates != null && !rates.isEmpty()) {
				BigDecimal fromRate = rates.get(productCurrencyCode);
				BigDecimal rubRate = rates.get("RUB");
				item.setItemAmountRub(upsertHelper.convert(item.getItemAmount(), fromRate, rubRate));
			}

			items.add(item);
		}

		upsertHelper.mergeItems(order.getId(), items);
		order.setItems(items);

		// 汇总字段
		order.setTotalQuantity(items.stream().mapToInt(ErpOrderItem::getQuantity).sum());
		order.setSkuCount(items.size());
		orderMapper.updateById(order);
	}

}
