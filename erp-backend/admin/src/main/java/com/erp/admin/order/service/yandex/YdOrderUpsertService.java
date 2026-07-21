package com.erp.admin.order.service.yandex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.entity.ErpOrderItem;
import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.order.service.OrderLifecycleService;
import com.erp.admin.order.service.common.OrderUpsertHelper;
import com.erp.admin.order.service.yandex.converter.YandexOrderStatusConverter;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrdersResponse.CurrencyValue;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrdersResponse.OrderDelivery;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrdersResponse.OrderItem;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrdersResponse.YandexOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Yandex 订单 Upsert 服务
 * <p>
 * 事务语义：insert/update + 库存过账在同一事务内。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YdOrderUpsertService {

    private final ErpOrderMapper orderMapper;
    private final OrderUpsertHelper upsertHelper;
    private final OrderLifecycleService lifecycleService;
    private final ObjectMapper objectMapper;

    /** Yandex API 返回的金额以元为单位，erp_order 表统一以分为单位存储，需 ×100 */
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** 仓库 ID 未知时的兜底值 */
    private static final String UNKNOWN_WAREHOUSE = "0";

    /**
     * Upsert Yandex 订单
     *
     * @param shopId      店铺 ID
     * @param yandexOrder Yandex 平台订单对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void upsertOrder(Long shopId, YandexOrder yandexOrder) {
        String platformOrderId = String.valueOf(yandexOrder.getOrderId());

        // 1. 映射 ERP 状态
        ErpOrderStatusEnum erpStatusEnum = YandexOrderStatusConverter.toErpStatus(
                yandexOrder.getStatus(), yandexOrder.getSubstatus());
        if (erpStatusEnum == null) {
            log.debug("[YANDEX] 忽略未知状态订单: orderId={}, status={}, substatus={}",
                    platformOrderId, yandexOrder.getStatus(), yandexOrder.getSubstatus());
            return;
        }
        String erpStatus = erpStatusEnum.name();

        // 2. 查询是否已存在（使用 Mapper 封装方法）
        ErpOrder existing = orderMapper.selectOneByShopPlatformPlatformOrderId(
                shopId, PlatformEnum.Yandex.code(), platformOrderId);

        String oldErpStatus = existing != null ? existing.getErpStatus() : null;
        boolean isNew = (existing == null);
        ErpOrder order = isNew ? new ErpOrder() : existing;

        // 3. 基本信息
        order.setPlatform(PlatformEnum.Yandex.code());
        order.setPlatformOrderId(platformOrderId);
        order.setShopId(shopId);
        order.setFulfillmentType("FBS");

        // 4. 发货单和仓库
        OrderDelivery delivery = yandexOrder.getDelivery();
        if (delivery != null) {
            if (delivery.getShipment() != null && delivery.getShipment().getId() != null) {
                order.setShipmentId(String.valueOf(delivery.getShipment().getId()));
            }
            order.setWarehouseId(delivery.getWarehouseId());
        }
        if (order.getWarehouseId() == null) {
            order.setWarehouseId(UNKNOWN_WAREHOUSE);
        }
        order.setDestinationWarehouseId(order.getWarehouseId());

        // 5. 状态
        order.setPlatformStatus(yandexOrder.getStatus());
        order.setPlatformSubstatus(yandexOrder.getSubstatus());
        order.setErpStatus(erpStatus);

        // 6. 汇率准备（先收集所有币种，再获取汇率，最后计算金额）
        LocalDate rateDate = LocalDate.now();
        if (yandexOrder.getCreationDate() != null) {
            try {
                rateDate = OffsetDateTime.parse(yandexOrder.getCreationDate()).toLocalDate();
            } catch (Exception ignored) {
                // 使用默认 today
            }
        }

        // 6.1 收集所有价格分量中出现的币种
        Set<String> currencies = new HashSet<>();
        currencies.add("RUB");
        if (yandexOrder.getPrices() != null) {
            collectCurrency(currencies,
                    yandexOrder.getPrices().getPayment(),
                    yandexOrder.getPrices().getCashback(),
                    yandexOrder.getPrices().getSubsidy());
        }
        if (!CollectionUtils.isEmpty(yandexOrder.getItems())) {
            for (OrderItem item : yandexOrder.getItems()) {
                if (item.getPrices() != null) {
                    collectCurrency(currencies,
                            item.getPrices().getPayment(),
                            item.getPrices().getCashback(),
                            item.getPrices().getSubsidy());
                }
            }
        }

        Map<String, BigDecimal> rates = upsertHelper.batchGetRates(currencies, "CNY", rateDate);

        // 6.2 订单级 totalAmount = (payment + cashback + subsidy) × 100
        // Yandex API 返回的金额以元为单位，erp_order 表统一以分为单位存储，需 ×100
        BigDecimal totalAmount = null;
        String currencyCode = null;
        if (yandexOrder.getPrices() != null && yandexOrder.getPrices().getPayment() != null) {
            currencyCode = extractCurrency(yandexOrder.getPrices().getPayment());
            totalAmount = sumPriceComponents(
                    yandexOrder.getPrices().getPayment(),
                    yandexOrder.getPrices().getCashback(),
                    yandexOrder.getPrices().getSubsidy(),
                    rates).multiply(HUNDRED);
        }
        order.setTotalAmount(totalAmount);
        order.setCurrencyCode(currencyCode);

        // 6.3 商品总价 SUM of (item payment + cashback + subsidy) × 100
        BigDecimal productTotalAmount = BigDecimal.ZERO;
        String productCurrencyCode = currencyCode;
        if (!CollectionUtils.isEmpty(yandexOrder.getItems())) {
            for (OrderItem item : yandexOrder.getItems()) {
                if (item.getPrices() != null) {
                    productTotalAmount = productTotalAmount.add(
                            sumPriceComponents(
                                    item.getPrices().getPayment(),
                                    item.getPrices().getCashback(),
                                    item.getPrices().getSubsidy(),
                                    rates).multiply(HUNDRED));
                    if (productCurrencyCode == null && item.getPrices().getPayment() != null) {
                        productCurrencyCode = extractCurrency(item.getPrices().getPayment());
                    }
                }
            }
        }
        order.setProductTotalAmount(productTotalAmount);
        order.setProductCurrencyCode(productCurrencyCode);

        // 7. 汇率转换
        // 商品总价 → CNY
        BigDecimal productRateToCny = rates.get(productCurrencyCode);
        if (productRateToCny != null) {
            order.setProductAmountCny(upsertHelper.convertDirect(productTotalAmount, productRateToCny));
        }

        // 订单总金额 → CNY + RUB
        if (totalAmount != null && StringUtils.hasText(currencyCode)) {
            BigDecimal orderRateToCny = rates.get(currencyCode);
            if (orderRateToCny != null) {
                order.setConvertedAmount(upsertHelper.convertDirect(totalAmount, orderRateToCny));
                order.setConvertedCurrencyCode("CNY");

                if ("RUB".equals(currencyCode)) {
                    order.setTotalAmountRub(totalAmount);
                } else {
                    BigDecimal rubRateToCny = rates.get("RUB");
                    if (rubRateToCny != null) {
                        order.setTotalAmountRub(upsertHelper.convert(totalAmount, orderRateToCny, rubRateToCny));
                    }
                }
            }
        }

        // 8. rawJson
        try {
            order.setRawJson(objectMapper.writeValueAsString(yandexOrder));
        } catch (Exception e) {
            log.warn("[YANDEX] 序列化 rawJson 失败: orderId={}", platformOrderId, e);
        }

        // 9. 时间
        LocalDateTime platformCreatedAt = parseIsoDateTime(yandexOrder.getCreationDate());
        upsertHelper.setTimeFields(order, platformCreatedAt);

        // 10. 构建明细（不持久化）
        List<ErpOrderItem> items = buildItems(order, yandexOrder, productCurrencyCode, rates);
        order.setItems(items);
        order.setTotalQuantity(items.stream().mapToInt(ErpOrderItem::getQuantity).sum());
        order.setSkuCount(items.size());

        // 11. 持久化
        if (isNew) {
            upsertHelper.setAuditFields(order, true);
            orderMapper.insert(order);
            upsertHelper.mergeItems(order.getId(), items);
            log.info("[YANDEX] 订单插入成功 orderId={} platformOrderId={}", order.getId(), platformOrderId);
            lifecycleService.onOrderCreated(order);
        } else {
            upsertHelper.setAuditFields(order, false);
            orderMapper.updateById(order);
            upsertHelper.mergeItems(order.getId(), items);
            log.info("[YANDEX] 订单更新成功 orderId={} platformOrderId={}", order.getId(), platformOrderId);
            if (!Objects.equals(oldErpStatus, order.getErpStatus())) {
                lifecycleService.onStatusChanged(order, oldErpStatus, order.getErpStatus());
            }
        }
    }

    /**
     * 构建订单商品明细（不做持久化）
     */
    private List<ErpOrderItem> buildItems(ErpOrder order, YandexOrder yandexOrder,
                                           String productCurrencyCode, Map<String, BigDecimal> rates) {
        if (CollectionUtils.isEmpty(yandexOrder.getItems())) {
            return new ArrayList<>();
        }

        List<ErpOrderItem> items = new ArrayList<>();
        for (OrderItem ydItem : yandexOrder.getItems()) {
            ErpOrderItem item = new ErpOrderItem();
            item.setOrderId(order.getId());
            item.setPlatformItemId(ydItem.getOfferId());
            item.setQuantity(ydItem.getCount() != null ? ydItem.getCount() : 0);

            // Yandex 商品金额 = (payment + cashback + subsidy) × 100（每项均为该商品所有数量的总价）
            BigDecimal itemAmount = BigDecimal.ZERO;
            if (ydItem.getPrices() != null) {
                itemAmount = sumPriceComponents(
                        ydItem.getPrices().getPayment(),
                        ydItem.getPrices().getCashback(),
                        ydItem.getPrices().getSubsidy(),
                        rates).multiply(HUNDRED);
            }
            item.setItemAmount(itemAmount);

            // 单价 = 总价 / 数量
            if (item.getQuantity() > 0) {
                item.setItemPrice(itemAmount.divide(
                        new BigDecimal(item.getQuantity()), 2, RoundingMode.HALF_UP));
            } else {
                item.setItemPrice(itemAmount);
            }

            // item_amount_rub
            if ("RUB".equals(productCurrencyCode)) {
                item.setItemAmountRub(itemAmount);
            } else if (rates != null && !rates.isEmpty()) {
                BigDecimal fromRate = rates.get(productCurrencyCode);
                BigDecimal rubRate = rates.get("RUB");
                item.setItemAmountRub(upsertHelper.convert(itemAmount, fromRate, rubRate));
            }

            items.add(item);
        }
        return items;
    }

    /**
     * 币种标准化：RUR → RUB
     */
    private String normalizeCurrency(String currencyId) {
        if ("RUR".equals(currencyId)) return "RUB";
        return currencyId;
    }

    /**
     * 从 CurrencyValue 中提取标准化币种，null 安全
     */
    private String extractCurrency(CurrencyValue cv) {
        if (cv == null || cv.getCurrencyId() == null) return null;
        return normalizeCurrency(cv.getCurrencyId());
    }

    /**
     * 将 CurrencyValue 中的币种加入集合（null 安全）
     */
    private void collectCurrency(Set<String> currencies, CurrencyValue... values) {
        for (CurrencyValue cv : values) {
            String cur = extractCurrency(cv);
            if (StringUtils.hasText(cur)) {
                currencies.add(cur);
            }
        }
    }

    /**
     * 如果 cv 币种与 baseCurrency 一致，直接返回 value；
     * 不一致则通过 rates 做汇率转换；转换失败降级返回 ZERO。
     *
     * @param cv           价格分量
     * @param baseCurrency 基准币种
     * @param rates        汇率表（各币种→CNY）
     * @param label        分量标签（用于日志）
     * @return 转换后金额（与 baseCurrency 同币种）
     */
    private BigDecimal convertIfNeeded(CurrencyValue cv, String baseCurrency,
                                        Map<String, BigDecimal> rates, String label) {
        if (cv == null || cv.getValue() == null) {
            return BigDecimal.ZERO;
        }
        String cur = extractCurrency(cv);
        if (baseCurrency == null || baseCurrency.equals(cur) || cur == null) {
            return cv.getValue();
        }
        // 币种不一致 → 汇率转换
        log.warn("[YANDEX] 价格分量币种不一致: base={}, {}={}, 进行汇率转换", baseCurrency, label, cur);
        BigDecimal fromRate = rates.get(cur);
        BigDecimal toRate = rates.get(baseCurrency);
        BigDecimal converted = upsertHelper.convert(cv.getValue(), fromRate, toRate);
        if (converted == null) {
            log.error("[YANDEX] 汇率转换失败: {} {}→{}, amount={}, rates={}",
                    label, cur, baseCurrency, cv.getValue(), rates);
            return BigDecimal.ZERO;
        }
        return converted;
    }

    /**
     * 安全累加 payment + cashback + subsidy
     * <p>
     * 以 payment 的币种为基准，其他分量如果币种不同则通过汇率转换后再累加。
     *
     * @param payment  买家实付
     * @param cashback Plus 积分抵扣
     * @param subsidy  平台补贴
     * @param rates    汇率表
     * @return 累加金额（baseCurrency 币种）
     */
    private BigDecimal sumPriceComponents(CurrencyValue payment, CurrencyValue cashback,
                                           CurrencyValue subsidy, Map<String, BigDecimal> rates) {
        String baseCurrency = extractCurrency(payment);
        BigDecimal sum = BigDecimal.ZERO;
        if (payment != null && payment.getValue() != null) {
            sum = sum.add(payment.getValue());
        }
        sum = sum.add(convertIfNeeded(cashback, baseCurrency, rates, "cashback"));
        sum = sum.add(convertIfNeeded(subsidy, baseCurrency, rates, "subsidy"));
        return sum;
    }

    /**
     * 解析 ISO 8601 日期时间字符串
     */
    private LocalDateTime parseIsoDateTime(String isoString) {
        if (!StringUtils.hasText(isoString)) return null;
        try {
            return OffsetDateTime.parse(isoString).toLocalDateTime();
        } catch (Exception e) {
            log.warn("[YANDEX] 时间解析失败: {}", isoString);
            return null;
        }
    }
}
