package com.erp.admin.platform.yandex.model.request.shipment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Yandex 搜索发货单请求
 * <p>
 * PUT /v2/campaigns/{campaignId}/first-mile/shipments
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexSearchShipmentsRequest {

	/** 发货日期起始（yyyy-MM-dd），必填 */
	@JsonProperty("dateFrom")
	private String dateFrom;

	/** 发货日期截止（yyyy-MM-dd），必填 */
	@JsonProperty("dateTo")
	private String dateTo;

	/** 发货单状态过滤 */
	@JsonProperty("statuses")
	private Set<String> statuses;

	/** 订单 ID 过滤 */
	@JsonProperty("orderIds")
	private Set<Long> orderIds;

	/** 是否返回已取消的订单，默认 true */
	@JsonProperty("cancelledOrders")
	private Boolean cancelledOrders;
}
