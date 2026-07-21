package com.erp.admin.platform.yandex.model.request.shipment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Yandex 确认发货单请求
 * <p>
 * POST /v2/campaigns/{campaignId}/first-mile/shipments/{shipmentId}/confirm
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexConfirmShipmentRequest {

	/** 发货单在卖家系统中的外部 ID */
	@JsonProperty("externalShipmentId")
	private String externalShipmentId;

	/** 签名人的 Yandex ID 登录名（不含 @yandex.ru） */
	@JsonProperty("signatory")
	private String signatory;
}
