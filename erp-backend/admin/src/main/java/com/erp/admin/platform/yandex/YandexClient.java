package com.erp.admin.platform.yandex;

import com.erp.admin.platform.PlatformApiException;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.PlatformHttpExecutor;
import com.erp.admin.platform.yandex.credential.YandexCredential;
import com.erp.admin.platform.yandex.model.request.order.YandexGetOrdersRequest;
import com.erp.admin.platform.yandex.model.request.order.YandexSetBoxLayoutRequest;
import com.erp.admin.platform.yandex.model.request.order.YandexUpdateOrderStatusRequest;
import com.erp.admin.platform.yandex.model.request.order.YandexUpdateOrderStatusesRequest;
import com.erp.admin.platform.yandex.model.request.shipment.YandexConfirmShipmentRequest;
import com.erp.admin.platform.yandex.model.request.shipment.YandexSearchShipmentsRequest;
import com.erp.admin.platform.yandex.model.response.campaign.YandexGetCampaignsResponse;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrderLabelsDataResponse;
import com.erp.admin.platform.yandex.model.response.order.YandexGetOrdersResponse;
import com.erp.admin.platform.yandex.model.response.order.YandexSetBoxLayoutResponse;
import com.erp.admin.platform.yandex.model.response.order.YandexUpdateOrderStatusResponse;
import com.erp.admin.platform.yandex.model.response.order.YandexUpdateOrderStatusesResponse;
import com.erp.admin.platform.yandex.model.response.shipment.YandexGetShipmentResponse;
import com.erp.admin.platform.yandex.model.response.shipment.YandexSearchShipmentsResponse;
import com.erp.admin.platform.yandex.model.response.warehouse.YandexGetWarehousesResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Yandex Market API 客户端
 * <p>
 * 职责：
 * 1. 封装 Yandex Market 所有 FBS 相关 API 接口
 * 2. 组装请求参数（URL 路径参数 + 查询参数 + 请求体）
 * 3. 委托 PlatformHttpExecutor 处理 HTTP 请求
 * <p>
 * API 文档：https://yandex.ru/dev/market/partner-api/doc/ru/reference/orders
 */
@Component
@Slf4j
public class YandexClient {

	private static final String PLATFORM = PlatformEnum.Yandex.code();
	private static final String BASE_URL = "https://api.partner.market.yandex.ru";

	private final PlatformHttpExecutor httpExecutor;
	private final ObjectMapper objectMapper;

	public YandexClient(
			@Qualifier("yandexHttpExecutor") PlatformHttpExecutor httpExecutor,
			@Qualifier("platformObjectMapper") ObjectMapper objectMapper) {
		this.httpExecutor = httpExecutor;
		this.objectMapper = objectMapper;
	}

	// ============================================================================
	// 店铺信息 API
	// ============================================================================

	/**
	 * 获取店铺列表
	 * <p>
	 * API: GET /v2/campaigns
	 * <p>
	 * 限流：1,000 次/小时
	 *
	 * @param credential Yandex 凭证
	 * @return 店铺列表响应
	 */
	public YandexGetCampaignsResponse getCampaigns(YandexCredential credential) {
		String endpoint = BASE_URL + "/v2/campaigns";
		HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());

		return httpExecutor.execute(
				credential.getApiKey(),
				HttpMethod.GET,
				YandexApiUtils.createUri(endpoint),
				headers,
				null,
				YandexGetCampaignsResponse.class);
	}

	// ============================================================================
	// 订单查询 API
	// ============================================================================

	/**
	 * 获取订单列表（business 级别）
	 * <p>
	 * API: POST /v1/businesses/{businessId}/orders
	 * <p>
	 * 限流：10,000 次/小时
	 * limit 最大值：50
	 * 日期范围最大：30 天
	 *
	 * @param credential Yandex 凭证
	 * @param request    订单查询请求
	 * @param pageToken  分页 token（首页传 null）
	 * @param limit      每页数量（最大50）
	 * @return 订单列表响应
	 */
	public YandexGetOrdersResponse getOrders(YandexCredential credential, YandexGetOrdersRequest request,
											 String pageToken, Integer limit) {
		try {
			String basePath = BASE_URL + "/v1/businesses/" + credential.getBusinessId() + "/orders";

			Map<String, Object> params = new LinkedHashMap<>();
			if (pageToken != null) {
				params.put("page_token", pageToken);
			}
			if (limit != null) {
				params.put("limit", limit);
			}

			URI uri = YandexApiUtils.buildUri(basePath, params);
			HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());
			String body = objectMapper.writeValueAsString(request);

			return httpExecutor.execute(
					credential.getApiKey(),
					HttpMethod.POST,
					uri,
					headers,
					body,
					YandexGetOrdersResponse.class);
		} catch (PlatformApiException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
					"序列化请求失败: " + e.getMessage(), 0, e);
		}
	}

	// ============================================================================
	// 订单状态变更 API
	// ============================================================================

	/**
	 * 更新单个订单状态
	 * <p>
	 * API: PUT /v2/campaigns/{campaignId}/orders/{orderId}/status
	 * <p>
	 * 限流：100,000 次/小时
	 * <p>
	 * 可用状态变更：
	 * - PROCESSING/STARTED → PROCESSING/READY_TO_SHIP（确认备货完成）
	 * - PROCESSING/STARTED → CANCELLED/SHOP_FAILED（商家无法履约）
	 * - PROCESSING/READY_TO_SHIP → CANCELLED/SHOP_FAILED（备货后无法履约）
	 *
	 * @param credential Yandex 凭证
	 * @param orderId    订单 ID
	 * @param request    状态变更请求
	 * @return 更新后的订单信息
	 */
	public YandexUpdateOrderStatusResponse updateOrderStatus(YandexCredential credential,
															 long orderId,
															 YandexUpdateOrderStatusRequest request) {
		try {
			String endpoint = BASE_URL + "/v2/campaigns/" + credential.getCampaignId()
					+ "/orders/" + orderId + "/status";
			HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());
			String body = objectMapper.writeValueAsString(request);

			return httpExecutor.execute(
					credential.getApiKey(),
					HttpMethod.PUT,
					YandexApiUtils.createUri(endpoint),
					headers,
					body,
					YandexUpdateOrderStatusResponse.class);
		} catch (PlatformApiException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
					"序列化请求失败: " + e.getMessage(), 0, e);
		}
	}

	/**
	 * 批量更新订单状态
	 * <p>
	 * API: POST /v2/campaigns/{campaignId}/orders/status-update
	 * <p>
	 * 限流：100,000 订单/小时
	 * 每次最多 30 个订单
	 *
	 * @param credential Yandex 凭证
	 * @param request    批量状态变更请求
	 * @return 批量更新结果
	 */
	public YandexUpdateOrderStatusesResponse updateOrderStatuses(YandexCredential credential,
																 YandexUpdateOrderStatusesRequest request) {
		try {
			String endpoint = BASE_URL + "/v2/campaigns/" + credential.getCampaignId()
					+ "/orders/status-update";
			HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());
			String body = objectMapper.writeValueAsString(request);

			return httpExecutor.execute(
					credential.getApiKey(),
					HttpMethod.POST,
					YandexApiUtils.createUri(endpoint),
					headers,
					body,
					YandexUpdateOrderStatusesResponse.class);
		} catch (PlatformApiException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
					"序列化请求失败: " + e.getMessage(), 0, e);
		}
	}

	// ============================================================================
	// 装箱 API
	// ============================================================================

	/**
	 * 设置订单装箱布局（准备发货）
	 * <p>
	 * API: PUT /v2/campaigns/{campaignId}/orders/{orderId}/boxes
	 * <p>
	 * 限流：100,000 次/小时
	 * <p>
	 * 在转为 READY_TO_SHIP 之前必须调用此接口传递装箱信息。
	 * 可以多次调用（覆盖），直到订单转为 READY_TO_SHIP。
	 *
	 * @param credential Yandex 凭证
	 * @param orderId    订单 ID
	 * @param request    装箱布局请求
	 * @return 装箱布局响应（含平台分配的箱子 ID）
	 */
	public YandexSetBoxLayoutResponse setOrderBoxLayout(YandexCredential credential,
														long orderId,
														YandexSetBoxLayoutRequest request) {
		try {
			String endpoint = BASE_URL + "/v2/campaigns/" + credential.getCampaignId()
					+ "/orders/" + orderId + "/boxes";
			HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());
			String body = objectMapper.writeValueAsString(request);

			return httpExecutor.execute(
					credential.getApiKey(),
					HttpMethod.PUT,
					YandexApiUtils.createUri(endpoint),
					headers,
					body,
					YandexSetBoxLayoutResponse.class);
		} catch (PlatformApiException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
					"序列化请求失败: " + e.getMessage(), 0, e);
		}
	}

	// ============================================================================
	// 面单 API
	// ============================================================================

	/**
	 * 获取订单面单 PDF
	 * <p>
	 * API: GET /v2/campaigns/{campaignId}/orders/{orderId}/delivery/labels
	 * <p>
	 * 限流：100,000 次/小时
	 * <p>
	 * 返回 PDF 文件字节数组，包含该订单所有箱子的面单
	 *
	 * @param credential Yandex 凭证
	 * @param orderId    订单 ID
	 * @return PDF 文件字节数组
	 */
	public byte[] getOrderLabelsPdf(YandexCredential credential, long orderId) {
		String endpoint = BASE_URL + "/v2/campaigns/" + credential.getCampaignId()
				+ "/orders/" + orderId + "/delivery/labels";
		HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());

		return httpExecutor.executeForBytes(
				credential.getApiKey(),
				HttpMethod.GET,
				YandexApiUtils.createUri(endpoint),
				headers,
				null,
				Duration.ofSeconds(60));
	}

	/**
	 * 获取订单面单数据（用于自定义面单打印）
	 * <p>
	 * API: GET /v2/campaigns/{campaignId}/orders/{orderId}/delivery/labels/data
	 * <p>
	 * 限流：100,000 次/小时
	 *
	 * @param credential Yandex 凭证
	 * @param orderId    订单 ID
	 * @return 面单数据响应
	 */
	public YandexGetOrderLabelsDataResponse getOrderLabelsData(YandexCredential credential, long orderId) {
		String endpoint = BASE_URL + "/v2/campaigns/" + credential.getCampaignId()
				+ "/orders/" + orderId + "/delivery/labels/data";
		HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());

		return httpExecutor.execute(
				credential.getApiKey(),
				HttpMethod.GET,
				YandexApiUtils.createUri(endpoint),
				headers,
				null,
				YandexGetOrderLabelsDataResponse.class);
	}

	// ============================================================================
	// 发货单 API
	// ============================================================================

	/**
	 * 搜索发货单
	 * <p>
	 * API: PUT /v2/campaigns/{campaignId}/first-mile/shipments
	 * <p>
	 * 限流：100 次/小时（非常严格！）
	 * limit 最大值：30
	 *
	 * @param credential Yandex 凭证
	 * @param request    搜索请求
	 * @param pageToken  分页 token（首页传 null）
	 * @param limit      每页数量（最大30）
	 * @return 发货单列表响应
	 */
	public YandexSearchShipmentsResponse searchShipments(YandexCredential credential,
														 YandexSearchShipmentsRequest request,
														 String pageToken, Integer limit) {
		try {
			String basePath = BASE_URL + "/v2/campaigns/" + credential.getCampaignId()
					+ "/first-mile/shipments";

			Map<String, Object> params = new LinkedHashMap<>();
			if (pageToken != null) {
				params.put("page_token", pageToken);
			}
			if (limit != null) {
				params.put("limit", limit);
			}

			URI uri = YandexApiUtils.buildUri(basePath, params);
			HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());
			String body = objectMapper.writeValueAsString(request);

			return httpExecutor.execute(
					credential.getApiKey(),
					HttpMethod.PUT,
					uri,
					headers,
					body,
					YandexSearchShipmentsResponse.class);
		} catch (PlatformApiException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
					"序列化请求失败: " + e.getMessage(), 0, e);
		}
	}

	/**
	 * 获取发货单详情
	 * <p>
	 * API: GET /v2/campaigns/{campaignId}/first-mile/shipments/{shipmentId}
	 * <p>
	 * 限流：100 次/小时（非常严格！）
	 *
	 * @param credential Yandex 凭证
	 * @param shipmentId 发货单 ID
	 * @return 发货单详情响应
	 */
	public YandexGetShipmentResponse getShipment(YandexCredential credential, long shipmentId) {
		String endpoint = BASE_URL + "/v2/campaigns/" + credential.getCampaignId()
				+ "/first-mile/shipments/" + shipmentId;
		HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());

		return httpExecutor.execute(
				credential.getApiKey(),
				HttpMethod.GET,
				YandexApiUtils.createUri(endpoint),
				headers,
				null,
				YandexGetShipmentResponse.class);
	}

	/**
	 * 确认发货单
	 * <p>
	 * API: POST /v2/campaigns/{campaignId}/first-mile/shipments/{shipmentId}/confirm
	 * <p>
	 * 限流：100 次/小时（非常严格！）
	 * <p>
	 * 前置条件：发货单的 availableActions 中必须包含 CONFIRM
	 *
	 * @param credential Yandex 凭证
	 * @param shipmentId 发货单 ID
	 * @param request    确认请求（可为 null，表示无外部 ID 和签名人）
	 */
	public void confirmShipment(YandexCredential credential, long shipmentId,
								YandexConfirmShipmentRequest request) {
		try {
			String endpoint = BASE_URL + "/v2/campaigns/" + credential.getCampaignId()
					+ "/first-mile/shipments/" + shipmentId + "/confirm";
			HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());
			String body = request != null ? objectMapper.writeValueAsString(request) : null;

			httpExecutor.executeForString(
					credential.getApiKey(),
					HttpMethod.POST,
					YandexApiUtils.createUri(endpoint),
					headers,
					body,
					null);

			log.info("[YANDEX] 确认发货单成功: shipmentId={}", shipmentId);
		} catch (PlatformApiException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
					"序列化请求失败: " + e.getMessage(), 0, e);
		}
	}

	// ============================================================================
	// 仓库 API
	// ============================================================================

	/**
	 * 获取仓库列表
	 * <p>
	 * API: GET /v2/businesses/{businessId}/warehouses
	 * <p>
	 * 限流：100 次/分钟
	 *
	 * @param credential Yandex 凭证
	 * @return 仓库列表响应
	 */
	public YandexGetWarehousesResponse getWarehouses(YandexCredential credential) {
		String endpoint = BASE_URL + "/v2/businesses/" + credential.getBusinessId() + "/warehouses";
		HttpHeaders headers = YandexApiUtils.createHeaders(credential.getApiKey());

		return httpExecutor.execute(
				credential.getApiKey(),
				HttpMethod.GET,
				YandexApiUtils.createUri(endpoint),
				headers,
				null,
				YandexGetWarehousesResponse.class);
	}
}
