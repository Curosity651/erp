package com.erp.admin.order.service.yandex;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.erp.admin.platform.yandex.YandexClient;
import com.erp.admin.platform.yandex.credential.YandexCredential;
import com.erp.admin.platform.yandex.model.request.order.YandexGetOrdersRequest;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrdersResponse;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrdersResponse.YandexOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Yandex 订单 API 业务封装
 * <p>
 * 封装翻页遍历、按 ID 批量查询等业务操作。
 * 不访问数据库，只调用 YandexClient。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YdPlatformApi {

    private final YandexClient yandexClient;

    private static final int PAGE_LIMIT = 50;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+00:00'");

    /**
     * 分页拉取指定时间段内的 FBS 订单
     * <p>
     * 自动处理 page_token 翻页，返回该时间段内全部订单。
     *
     * @param credential Yandex 凭证
     * @param from       更新时间起始（UTC）
     * @param to         更新时间截止（UTC）
     * @param statuses   订单状态过滤
     * @return 所有订单列表
     */
    public List<YandexOrder> fetchOrdersByUpdateTime(
            YandexCredential credential,
            LocalDateTime from,
            LocalDateTime to,
            Set<String> statuses) {

        YandexGetOrdersRequest request = YandexGetOrdersRequest.builder()
                .dates(YandexGetOrdersRequest.DatesFilter.builder()
                        .updateDateFrom(from.format(ISO_FORMATTER))
                        .updateDateTo(to.format(ISO_FORMATTER))
                        .build())
                .programTypes(Collections.singleton("FBS"))
                .statuses(statuses)
                .fake(false)
                .build();

        List<YandexOrder> allOrders = new ArrayList<>();
        String pageToken = null;

        do {
            YandexGetOrdersResponse response = yandexClient.getOrders(
                    credential, request, pageToken, PAGE_LIMIT);

            if (response == null || CollectionUtils.isEmpty(response.getOrders())) {
                break;
            }

            allOrders.addAll(response.getOrders());

            pageToken = (response.getPaging() != null) ? response.getPaging().getNextPageToken() : null;

            log.debug("[YANDEX] 拉取订单 {} 条，pageToken={}", response.getOrders().size(), pageToken);

        } while (pageToken != null);

        return allOrders;
    }

    /**
     * 按订单 ID 批量查询（用于未完结订单回溯）
     * <p>
     * orderIds 超过 50 个时自动分批。
     *
     * @param credential Yandex 凭证
     * @param orderIds   订单 ID 集合
     * @return 所有查询到的订单
     */
    public List<YandexOrder> fetchOrdersByIds(
            YandexCredential credential,
            Set<Long> orderIds) {

        List<YandexOrder> allOrders = new ArrayList<>();
        List<Long> idList = new ArrayList<>(orderIds);

        // 按 50 个一批分片
        for (int i = 0; i < idList.size(); i += PAGE_LIMIT) {
            int end = Math.min(i + PAGE_LIMIT, idList.size());
            Set<Long> batch = new HashSet<>(idList.subList(i, end));

            YandexGetOrdersRequest request = YandexGetOrdersRequest.builder()
                    .orderIds(batch)
                    .programTypes(Collections.singleton("FBS"))
                    .fake(false)
                    .build();

            String pageToken = null;
            do {
                YandexGetOrdersResponse response = yandexClient.getOrders(
                        credential, request, pageToken, PAGE_LIMIT);

                if (response == null || CollectionUtils.isEmpty(response.getOrders())) {
                    break;
                }

                allOrders.addAll(response.getOrders());
                pageToken = (response.getPaging() != null) ? response.getPaging().getNextPageToken() : null;

            } while (pageToken != null);
        }

        return allOrders;
    }
}
