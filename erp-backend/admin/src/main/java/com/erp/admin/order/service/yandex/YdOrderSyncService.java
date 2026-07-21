package com.erp.admin.order.service.yandex;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.erp.admin.order.model.vo.SyncSummaryVO;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.sync.service.SyncCursorService;
import com.erp.admin.sync.model.enums.SyncTaskTypeEnum;
import com.erp.admin.platform.yandex.credential.YandexCredential;
import com.erp.admin.platform.yandex.enums.YandexOrderStatusEnum;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrdersResponse.YandexOrder;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Yandex 订单同步编排服务
 * <p>
 * 职责：
 * - 时间窗口管理（增量/全量）
 * - 遍历所有启用的 Yandex 店铺
 * - 未完结订单回溯
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YdOrderSyncService {

    private final ShopService shopService;
    private final YdPlatformApi ydPlatformApi;
    private final YdOrderUpsertService ydOrderUpsertService;
    private final ErpOrderMapper orderMapper;
	private final CredentialService credentialService;
	private final SyncCursorService syncCursorService;

    private static final int MAX_WINDOW_DAYS = 30;
    private static final int LOOKBACK_MINUTES = 30;
    private static final LocalDateTime DEFAULT_START_DATE = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    /**
     * 增量拉单的服务端状态过滤白名单。
     * <p>
     * 业务决策（2026-07-02，与运营确认）：本店铺仅经营预付款订单，不做货到付款。
     * 预付款订单直接以 PROCESSING 生效，不经过 PENDING（待卖家接单）状态，因此无需拉取 PENDING。
     * 另：该接口的 statuses 过滤本身不接受 PENDING/PARTIALLY_RETURNED
     * （传入报 400 "Statuses [...] are not allowed"）。
     * 若未来开通货到付款业务，需改为不传 statuses 拉全量 + Converter 本地过滤，
     * 并在订单页补充平台侧"接单"操作。
     */
    private static final Set<String> SYNC_STATUSES = new HashSet<>();
    static {
        SYNC_STATUSES.add(YandexOrderStatusEnum.PROCESSING.name());
        SYNC_STATUSES.add(YandexOrderStatusEnum.DELIVERY.name());
        SYNC_STATUSES.add(YandexOrderStatusEnum.PICKUP.name());
        SYNC_STATUSES.add(YandexOrderStatusEnum.DELIVERED.name());
        SYNC_STATUSES.add(YandexOrderStatusEnum.CANCELLED.name());
        SYNC_STATUSES.add(YandexOrderStatusEnum.RETURNED.name());
    }


    /**
     * 同步所有启用的 Yandex 店铺
     *
     * @param fetchAll true=全量同步，false=增量同步
     */
    public void syncAllShops(boolean fetchAll) {
        List<Shop> shops = shopService.listEnabledByPlatform(PlatformEnum.Yandex.code());
        if (CollectionUtils.isEmpty(shops)) {
            log.debug("[YANDEX][SYNC] 没有启用的 Yandex 店铺");
            return;
        }

        for (Shop shop : shops) {
            try {
                TenantContext.runAs(shop.getTenantId(), () -> {
                    syncShopOrders(shop, fetchAll);
                    return null;
                });
            } catch (Exception e) {
                log.error("[YANDEX][SYNC] 店铺 {} 同步失败: {}", shop.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 同步单个店铺订单
     */
    public void syncShopOrders(Shop shop, boolean fetchAll) {
        // 1. 解析凭证
        YandexCredential credential;
        try {
            credential = credentialService.parseCredential(shop);
        } catch (Exception e) {
            log.error("[YANDEX][SYNC] 店铺 {} 凭证解析失败: {}", shop.getId(), e.getMessage());
            return;
        }

        // 2. 确定时间窗口
        LocalDateTime windowStart = resolveWindowStart(shop.getId(), fetchAll);
        LocalDateTime windowEnd = LocalDateTime.now(ZoneOffset.UTC);

        if (windowStart.isAfter(windowEnd)) {
            windowStart = windowEnd.minusDays(1);
        }

        log.info("[YANDEX][SYNC] 店铺 {} 同步窗口: {} ~ {}", shop.getId(), windowStart, windowEnd);

        // 3. 按 30 天分段拉取
        LocalDateTime cursor = windowStart;
        int totalCount = 0;

        while (!cursor.isAfter(windowEnd)) {
            LocalDateTime segmentEnd = cursor.plusDays(MAX_WINDOW_DAYS);
            if (segmentEnd.isAfter(windowEnd)) {
                segmentEnd = windowEnd;
            }

            log.debug("[YANDEX][SYNC] 店铺 {} 同步片段: {} ~ {}", shop.getId(), cursor, segmentEnd);

            List<YandexOrder> orders = ydPlatformApi.fetchOrdersByUpdateTime(
                    credential, cursor, segmentEnd, SYNC_STATUSES);

            boolean segmentFailed = false;

            for (YandexOrder ydOrder : orders) {
                // 过滤测试订单
                if (Boolean.TRUE.equals(ydOrder.getFake())) {
                    continue;
                }
                try {
                    ydOrderUpsertService.upsertOrder(shop.getId(), ydOrder);
                    totalCount++;
                } catch (Exception e) {
                    segmentFailed = true;
                    log.error("[YANDEX][SYNC] 订单 upsert 失败: orderId={}, error={}",
                            ydOrder.getOrderId(), e.getMessage(), e);
                }
            }

            if (segmentFailed) {
                log.warn("[YANDEX][SYNC] 店铺 {} 片段 {}~{} 存在订单入库失败，保留游标不推进，下轮重试（若持续失败请人工排查坏单）",
                        shop.getId(), cursor, segmentEnd);
                break;
            }

            cursor = segmentEnd.plusSeconds(1);

			// 阶段性同步完成，推进游标
			syncCursorService.advanceCursor(
					shop.getId(), PlatformEnum.Yandex.code(), SyncTaskTypeEnum.ORDER_INCREMENTAL, cursor);
        }

        log.info("[YANDEX][SYNC] 店铺 {} 同步完成，共处理 {} 条订单", shop.getId(), totalCount);
    }

    /**
     * 按 ERP 订单 ID 同步指定订单
     *
     * @param orderIds ERP 内部订单 ID 列表
     * @return 同步结果摘要
     */
    public SyncSummaryVO syncOrdersByIds(List<Long> orderIds) {
        SyncSummaryVO summary = new SyncSummaryVO();

        if (CollectionUtils.isEmpty(orderIds)) {
            return summary;
        }

        // 1. 批量查询 ERP 订单
        List<ErpOrder> orders = orderMapper.selectBatchIds(orderIds);
        if (CollectionUtils.isEmpty(orders)) {
            log.warn("[YANDEX][SYNC_BY_ID] 未查询到任何订单");
            return summary;
        }

        summary.setTotal(orders.size());

        // 2. 按 shopId 分组（只处理 Yandex 平台）
        Map<Long, List<ErpOrder>> groupByShop = new HashMap<>();
        for (ErpOrder order : orders) {
            if (!PlatformEnum.Yandex.code().equalsIgnoreCase(order.getPlatform())) {
                log.debug("[YANDEX][SYNC_BY_ID] 跳过非 Yandex 订单 orderId={} platform={}",
                        order.getId(), order.getPlatform());
                summary.incrSkipped();
                continue;
            }
            groupByShop.computeIfAbsent(order.getShopId(), k -> new ArrayList<>()).add(order);
        }

        // 3. 按店铺同步
        for (Map.Entry<Long, List<ErpOrder>> entry : groupByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<ErpOrder> shopOrders = entry.getValue();

            try {
                syncShopOrdersByIds(shopId, shopOrders, summary);
            } catch (Exception e) {
                log.error("[YANDEX][SYNC_BY_ID] 店铺 {} 同步失败: {}", shopId, e.getMessage(), e);
                summary.addFailed(shopOrders.size());
            }
        }

        log.info("[YANDEX][SYNC_BY_ID] 完成: total={}, success={}, failed={}, skipped={}",
                summary.getTotal(), summary.getSuccess(), summary.getFailed(), summary.getSkipped());
        return summary;
    }

    /**
     * 按店铺同步指定订单
     */
    private void syncShopOrdersByIds(Long shopId, List<ErpOrder> orders, SyncSummaryVO summary) {
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            log.warn("[YANDEX][SYNC_BY_ID] 店铺不存在: shopId={}", shopId);
            summary.addFailed(orders.size());
            return;
        }

        YandexCredential credential;
        try {
            credential = credentialService.parseCredential(shop);
        } catch (Exception e) {
            log.error("[YANDEX][SYNC_BY_ID] 店铺 {} 凭证解析失败: {}", shopId, e.getMessage());
            summary.addFailed(orders.size());
            return;
        }

        // 提取 platformOrderId → Long
        Set<Long> platformOrderIds = orders.stream()
                .map(o -> {
                    try {
                        return Long.parseLong(o.getPlatformOrderId());
                    } catch (NumberFormatException e) {
                        log.warn("[YANDEX][SYNC_BY_ID] 无法解析 platformOrderId: {}", o.getPlatformOrderId());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (platformOrderIds.isEmpty()) {
            summary.addFailed(orders.size());
            return;
        }

        // 调用平台 API 拉取最新数据
        List<YandexOrder> latestOrders = ydPlatformApi.fetchOrdersByIds(credential, platformOrderIds);

        for (YandexOrder ydOrder : latestOrders) {
            try {
                ydOrderUpsertService.upsertOrder(shopId, ydOrder);
                summary.incrSuccess();
            } catch (Exception e) {
                log.error("[YANDEX][SYNC_BY_ID] 订单 upsert 失败: orderId={}, error={}",
                        ydOrder.getOrderId(), e.getMessage(), e);
                summary.incrFailed();
            }
        }

        // 平台未返回的订单计为 failed
        int notFound = platformOrderIds.size() - latestOrders.size();
        if (notFound > 0) {
            summary.addFailed(notFound);
        }
    }

    /**
     * 未完结订单回溯
     * <p>
     * 查询 DB 中 erpStatus 为 READY_TO_SHIP/SHIPPED 的 Yandex 订单，
     * 按 orderIds 直接查询平台最新状态。
     */
    public void syncUnfinishedOrders() {
        // 1. 查询未完结订单（使用 Mapper 封装方法）
        List<ErpOrder> unfinishedOrders = orderMapper.selectUnfinishedByPlatform(PlatformEnum.Yandex.code());
        if (CollectionUtils.isEmpty(unfinishedOrders)) {
            log.debug("[YANDEX][STATUS_SYNC] 没有未完结的 Yandex 订单");
            return;
        }

        log.info("[YANDEX][STATUS_SYNC] 查询到 {} 条未完结订单", unfinishedOrders.size());

        // 2. 按 shopId 分组
        Map<Long, List<ErpOrder>> groupByShop = unfinishedOrders.stream()
                .collect(Collectors.groupingBy(ErpOrder::getShopId));

        for (Map.Entry<Long, List<ErpOrder>> entry : groupByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<ErpOrder> orders = entry.getValue();

            try {
                syncShopUnfinishedOrders(shopId, orders);
            } catch (Exception e) {
                log.error("[YANDEX][STATUS_SYNC] 店铺 {} 未完结订单回溯失败: {}",
                        shopId, e.getMessage(), e);
            }
        }
    }

    /**
     * 同步单个店铺的未完结订单
     */
    private void syncShopUnfinishedOrders(Long shopId, List<ErpOrder> orders) {
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            log.warn("[YANDEX][STATUS_SYNC] 店铺不存在: shopId={}", shopId);
            return;
        }

        YandexCredential credential;
        try {
            credential = credentialService.parseCredential(shop);
        } catch (Exception e) {
            log.error("[YANDEX][STATUS_SYNC] 店铺 {} 凭证解析失败: {}", shopId, e.getMessage());
            return;
        }

        // 收集 platformOrderId → long
        Set<Long> orderIds = orders.stream()
                .map(o -> {
                    try {
                        return Long.parseLong(o.getPlatformOrderId());
                    } catch (NumberFormatException e) {
                        log.warn("[YANDEX][STATUS_SYNC] 无法解析 platformOrderId: {}", o.getPlatformOrderId());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (orderIds.isEmpty()) {
            return;
        }

        List<YandexOrder> latestOrders = ydPlatformApi.fetchOrdersByIds(credential, orderIds);

        int updateCount = 0;
        for (YandexOrder ydOrder : latestOrders) {
            try {
                ydOrderUpsertService.upsertOrder(shopId, ydOrder);
                updateCount++;
            } catch (Exception e) {
                log.error("[YANDEX][STATUS_SYNC] 订单状态更新失败: orderId={}, error={}",
                        ydOrder.getOrderId(), e.getMessage(), e);
            }
        }

        log.info("[YANDEX][STATUS_SYNC] 店铺 {} 未完结订单回溯完成，更新 {} 条", shopId, updateCount);
    }

    /**
     * 计算同步窗口起始时间
     */
    private LocalDateTime resolveWindowStart(Long shopId, boolean fetchAll) {
        if (fetchAll) {
            return DEFAULT_START_DATE;
        }

        try {
            LocalDateTime cursor = syncCursorService.getCursor(
                    shopId, PlatformEnum.Yandex.code(), SyncTaskTypeEnum.ORDER_INCREMENTAL);

            if (cursor == null) {
                log.info("[YANDEX][SYNC] 店铺 {} 首次同步，从 {} 开始", shopId, DEFAULT_START_DATE);
                return DEFAULT_START_DATE;
            }

            LocalDateTime startTime = cursor.minusMinutes(LOOKBACK_MINUTES);

            log.info("[YANDEX][SYNC] 店铺 {} 增量同步，游标={}, 回溯 {}min, 起点={}",
                    shopId, cursor, LOOKBACK_MINUTES, startTime);
            return startTime;

        } catch (Exception e) {
            log.warn("[YANDEX][SYNC] 获取游标失败，使用默认回溯: {}", e.getMessage());
            return LocalDateTime.now(ZoneOffset.UTC).minusDays(MAX_WINDOW_DAYS);
        }
    }
}
