package com.erp.admin.order.service.label.strategy;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.service.LabelConstants;
import com.erp.admin.order.service.LabelService;
import com.erp.admin.order.service.label.LabelPdfGenerator;
import com.erp.admin.order.service.label.LabelPrintStrategy;
import com.erp.admin.order.service.label.OrderGrouper;
import com.erp.admin.order.service.label.model.OrderFailureInfo;
import com.erp.admin.order.service.label.model.PdfGenerationRequest;
import com.erp.admin.order.service.label.model.PlatformContext;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.yandex.YandexClient;
import com.erp.admin.platform.yandex.credential.YandexCredential;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Yandex 面单打印策略
 * <p>
 * - 面单来源：Yandex API 返回 PDF（byte[]）
 * - 同步方式：逐单从 API 获取 PDF → Base64 → 缓存
 * - PDF 生成：合并多个订单 PDF 为一个文件（与 Ozon 一致）
 * - 校验规则：SKU 映射完整 + 面单已缓存
 * - 仓库名称：从 rawJson 的 delivery.serviceName 解析
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YdLabelPrintStrategy implements LabelPrintStrategy {

    private final YandexClient yandexClient;
    private final ShopService shopService;
    private final LabelPdfGenerator pdfGenerator;
    private final LabelService labelService;
	private final CredentialService credentialService;

    @Override
    public String getPlatformCode() {
        return PlatformEnum.Yandex.code();
    }

    @Override
    public PlatformContext syncLabels(List<ErpOrder> orders) {
        log.info("[YANDEX][LABEL] 开始同步面单: orderCount={}", orders.size());

        // 过滤出缺失面单的订单，按 shopId 分组
        Map<Long, List<ErpOrder>> ordersByShop = orders.stream()
                .filter(order -> !StringUtils.hasText(order.getLabelBase64()))
                .collect(Collectors.groupingBy(ErpOrder::getShopId));

        for (Map.Entry<Long, List<ErpOrder>> entry : ordersByShop.entrySet()) {
            syncShopLabels(entry.getKey(), entry.getValue());
        }

        return PlatformContext.empty();
    }

    @Override
    public OrderFailureInfo checkPrintability(ErpOrder order,
                                               Map<String, Sku> skuMap,
                                               Map<String, String> skuCodeMap,
                                               PlatformContext context) {
        // 规则1：所有 items 必须有 skuCode
        if (order.getItems() == null || order.getItems().isEmpty()
                || order.getItems().stream()
                       .anyMatch(item -> !skuCodeMap.containsKey(item.getPlatformItemId()))) {
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_SKU_MAPPING_MISSING,
                    "SKU 映射缺失");
        }

        // 规则2：面单已缓存
        if (!StringUtils.hasText(order.getLabelBase64())) {
            return new OrderFailureInfo(order,
                    LabelConstants.ITEM_FAIL_ORDER_LABEL_MISSING,
                    "面单未获取");
        }

        return null;
    }

    @Override
    public void enrichWarehouseNames(Map<String, OrderGrouper.OrderGroup> groups) {
        for (OrderGrouper.OrderGroup group : groups.values()) {
            if (group.getOrders().isEmpty()) {
                continue;
            }
            ErpOrder firstOrder = group.getOrders().get(0);
            String warehouseName = extractWarehouseNameFromRawJson(firstOrder);
            group.setDestinationWarehouseName(warehouseName);
        }
    }

    @Override
    public void generatePdfFiles(PdfGenerationRequest request) {
        for (Map.Entry<String, OrderGrouper.OrderGroup> entry : request.getGroups().entrySet()) {
            LabelPdfGenerator.PdfGenerationContext context = new LabelPdfGenerator.PdfGenerationContext(
                    request.getBatchId(),
                    request.getBatchNo(),
                    getPlatformCode(),
                    entry.getKey(),
                    entry.getValue(),
                    "YANDEX",
                    LabelConstants.FILE_TYPE_YD_PDF
            );

            pdfGenerator.mergePdfs(context, orders -> orders.stream()
                    .map(ErpOrder::getLabelBase64)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList()));
        }
    }

    // ========== 私有方法 ==========

    private void syncShopLabels(Long shopId, List<ErpOrder> orders) {
        try {
            Shop shop = shopService.getById(shopId);
            if (shop == null) {
                log.error("[YANDEX][LABEL] 店铺不存在: shopId={}", shopId);
                return;
            }
            YandexCredential credential = credentialService.parseCredential(shop);

            for (ErpOrder order : orders) {
                syncSingleOrderLabel(order, credential, shopId);
            }
        } catch (Exception e) {
            log.error("[YANDEX][LABEL] 同步店铺面单失败: shopId={}, error={}",
                    shopId, e.getMessage(), e);
        }
    }

    private void syncSingleOrderLabel(ErpOrder order, YandexCredential credential, Long shopId) {
        try {
            String platformOrderId = order.getPlatformOrderId();
            if (!StringUtils.hasText(platformOrderId)) {
                log.warn("[YANDEX][LABEL] 订单缺少 platformOrderId: orderId={}", order.getId());
                return;
            }

            long ydOrderId = Long.parseLong(platformOrderId);
            byte[] pdfBytes = yandexClient.getOrderLabelsPdf(credential, ydOrderId);
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.warn("[YANDEX][LABEL] 获取面单失败，返回数据为空: orderId={}", order.getId());
                return;
            }

            String labelBase64 = Base64.getEncoder().encodeToString(pdfBytes);
            labelService.updateOrderLabel(order.getId(), labelBase64);
            order.setLabelBase64(labelBase64);

            log.info("[YANDEX][LABEL] 更新面单成功: shopId={}, orderId={}", shopId, order.getId());
        } catch (Exception e) {
            log.error("[YANDEX][LABEL] 同步面单失败: orderId={}, error={}",
                    order.getId(), e.getMessage(), e);
        }
    }

    private String extractWarehouseNameFromRawJson(ErpOrder order) {
        if (!StringUtils.hasText(order.getRawJson())) {
            return "{warehouseName}";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(order.getRawJson());
            JsonNode delivery = root.path("delivery");

            // 优先用 serviceName（配送服务名称）
            if (delivery.has("serviceName") && !delivery.get("serviceName").isNull()) {
                String name = delivery.get("serviceName").asText();
                if (StringUtils.hasText(name)) {
                    return name;
                }
            }

            // 兜底用 warehouseId
            if (delivery.has("warehouseId") && !delivery.get("warehouseId").isNull()) {
                return String.valueOf(delivery.get("warehouseId").asLong());
            }
        } catch (Exception e) {
            log.warn("[YANDEX][LABEL] 解析仓库名称失败: orderId={}, error={}",
                    order.getId(), e.getMessage());
        }

        return "{warehouseName}";
    }
}
