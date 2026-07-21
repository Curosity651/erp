package com.erp.admin.platform.wildberries;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.financial.model.entity.WbReportDetail;
import com.erp.admin.platform.PlatformApiException;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.PlatformHttpExecutor;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.model.common.WbPaginationQuery;
import com.erp.admin.platform.wildberries.model.request.order.WbOrderStatusRequest;
import com.erp.admin.platform.wildberries.model.request.report.WbReportDetailRequest;
import com.erp.admin.platform.wildberries.model.request.supply.WbAddOrderToSupplierRequest;
import com.erp.admin.platform.wildberries.model.request.supply.WbCreateSupplyRequest;
import com.erp.admin.platform.wildberries.model.response.office.WbOffice;
import com.erp.admin.platform.wildberries.model.response.order.WbOrderListResponse;
import com.erp.admin.platform.wildberries.model.response.order.WbOrderStatusResponse;
import com.erp.admin.platform.wildberries.model.response.seller.WbSellerInfo;
import com.erp.admin.platform.wildberries.model.response.sticker.WbStickersResponse;
import com.erp.admin.platform.wildberries.model.response.supply.WbCreateSupplyResponse;
import com.erp.admin.platform.wildberries.model.response.supply.WbSupplyBarcodeResponse;
import com.erp.admin.platform.wildberries.model.response.supply.WbSupplyDetail;
import com.erp.admin.platform.wildberries.model.response.supply.WbSupplyListResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.util.JsonUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Wildberries API 客户端
 * <p>
 * 职责：
 * 1. 封装 WB 所有 API 接口
 * 2. 组装请求参数
 * 3. 委托 PlatformHttpExecutor 处理 HTTP 请求
 * <p>
 * API 文档：https://openapi.wildberries.ru/
 */
@Component
@Slf4j
public class WildberriesClient {

	private static final String PLATFORM = PlatformEnum.Wildberries.code();
	private static final String MARKETPLACE_ENDPOINT = "https://marketplace-api.wildberries.ru";
	private static final String COMMON_ENDPOINT = "https://common-api.wildberries.ru";
	private static final String STATISTICS_ENDPOINT = "https://statistics-api.wildberries.ru";

	private final PlatformHttpExecutor httpExecutor;
	private final ObjectMapper objectMapper;

	public WildberriesClient(
			@Qualifier("wbHttpExecutor") PlatformHttpExecutor httpExecutor,
			@Qualifier("platformObjectMapper") ObjectMapper objectMapper) {
		this.httpExecutor = httpExecutor;
		this.objectMapper = objectMapper;
	}

	// ============================================================================
	// 凭证验证
	// ============================================================================

	/**
	 * 获取卖家信息（强类型版本）
	 * <p>
	 * API: GET /api/v1/seller-info
	 * <p>
	 * 注意：此接口使用不同的 BASE_URL (common-api.wildberries.ru)
	 *
	 * @param credential WB 凭证
	 * @return 卖家信息
	 */
	public WbSellerInfo getSellerInfo(WbCredential credential) {

		return httpExecutor.execute(
				WbApiUtils.rateKey(credential.getApiKey()),
				HttpMethod.GET,
				WbApiUtils.createUri(COMMON_ENDPOINT + "/api/v1/seller-info"),
				WbApiUtils.createHeaders(credential.getApiKey()),
				null,
				WbSellerInfo.class);
	}

	/**
	 * 验证凭证并获取店铺信息
	 * <p>
	 * 此方法用于凭证验证，调用 getSellerInfo 获取卖家信息
	 *
	 * @param credential 凭证信息（必须包含 api_key）
	 * @return 卖家信息（包含 sid、name、tradeMark）
	 * @throws IllegalArgumentException api_key 为空
	 * @throws PlatformApiException     验证失败
	 */
	public WbSellerInfo validateCredentialAndGetShopInfo(Map<String, String> credential) {
		String apiKey = credential.get("api_key");
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException("api_key 不能为空");
		}

		try {
			WbCredential wbCred = new WbCredential(apiKey);
			WbSellerInfo sellerInfo = getSellerInfo(wbCred);

			if (!StringUtils.hasText(sellerInfo.getSid())) {
				throw new PlatformApiException(PLATFORM, "MALFORMED_RESPONSE",
						"缺少 sid 字段", 0);
			}

			return sellerInfo;

		} catch (PlatformApiException e) {
			throw e;
		} catch (Exception e) {
			throw new PlatformApiException(PLATFORM, "VALIDATION_ERROR",
					"凭证验证失败: " + e.getMessage(), 0, e);
		}
	}

	// ============================================================================
	// 订单相关 API
	// ============================================================================

	/**
	 * 获取订单列表（强类型版本）
	 * <p>
	 * API: GET /api/v3/orders
	 * <p>
	 * 支持按时间范围查询，最多可查询 30 天的数据
	 *
	 * @param credential WB 凭证
	 * @param query      分页查询参数（支持 limit, next, dateFrom, dateTo）
	 * @return 订单列表响应
	 */
	public WbOrderListResponse getOrdersTyped(
			WbCredential credential,
			WbPaginationQuery query) {

		Map<String, Object> params = new HashMap<>();
		if (query != null) {
			if (query.getLimit() != null) {
				params.put("limit", query.getLimit());
			}
			if (query.getNext() != null) {
				params.put("next", query.getNext());
			}
			if (query.getDateFrom() != null) {
				params.put("dateFrom", query.getDateFrom());
			}
			if (query.getDateTo() != null) {
				params.put("dateTo", query.getDateTo());
			}
		}

		URI uri = WbApiUtils.buildUri(MARKETPLACE_ENDPOINT + "/api/v3/orders", params);
		return httpExecutor.execute(
				WbApiUtils.rateKey(credential.getApiKey()),
				HttpMethod.GET,
				uri,
				WbApiUtils.createHeaders(credential.getApiKey()),
				null,
				WbOrderListResponse.class);
	}

	/**
	 * 获取订单状态（批量，强类型版本）
	 * <p>
	 * API: POST /api/v3/orders/status
	 *
	 * @param credential WB 凭证
	 * @param request    订单状态查询请求
	 * @return 订单状态响应
	 */
	public WbOrderStatusResponse getOrderStatusesTyped(
			WbCredential credential,
			WbOrderStatusRequest request) {

		if (request == null || request.getOrders() == null || request.getOrders().isEmpty()) {
			throw new IllegalArgumentException("订单ID列表不能为空");
		}

		try {
			String body = objectMapper.writeValueAsString(request);

			return httpExecutor.execute(
					WbApiUtils.rateKey(credential.getApiKey()),
					HttpMethod.POST,
					WbApiUtils.createUri(MARKETPLACE_ENDPOINT + "/api/v3/orders/status"),
					WbApiUtils.createJsonHeaders(credential.getApiKey()),
					body,
					WbOrderStatusResponse.class);
		} catch (PlatformApiException e) {
			throw e;
		} catch (Exception e) {
			throw new PlatformApiException(PLATFORM, "PARSE_ERROR",
					"序列化订单状态请求失败: " + e.getMessage(), 0, e);
		}
	}

	/**
	 * 批量获取订单贴纸（强类型版本）
	 * <p>
	 * API: POST /api/v3/orders/stickers?type=png&width=40&height=30
	 *
	 * @param credential WB 凭证
	 * @param request    获取贴纸请求
	 * @return 贴纸列表响应
	 */
	public WbStickersResponse getOrderStickersTyped(
			WbCredential credential,
			com.erp.admin.platform.wildberries.model.request.sticker.WbGetStickersRequest request) {

		if (request == null || request.getOrders() == null || request.getOrders().isEmpty()) {
			throw new IllegalArgumentException("订单ID列表不能为空");
		}

		try {
			String body = objectMapper.writeValueAsString(request);

			Map<String, Object> queryParams = new HashMap<>();
			queryParams.put("type", "png");
			queryParams.put("width", 40);
			queryParams.put("height", 30);

			URI uri = WbApiUtils.buildUri(MARKETPLACE_ENDPOINT + "/api/v3/orders/stickers", queryParams);

			return httpExecutor.execute(
					WbApiUtils.rateKey(credential.getApiKey()),
					HttpMethod.POST,
					uri,
					WbApiUtils.createJsonHeaders(credential.getApiKey()),
					body,
					WbStickersResponse.class);
		} catch (PlatformApiException e) {
			throw e;
		} catch (Exception e) {
			throw new PlatformApiException(PLATFORM, "PARSE_ERROR",
					"序列化订单贴纸请求失败: " + e.getMessage(), 0, e);
		}
	}

	// ============================================================================
	// Supply（批次）相关 API（WB 特有概念）
	// ============================================================================

	/**
	 * 创建批次（强类型版本）
	 * <p>
	 * API: POST /api/v3/supplies
	 *
	 * @param credential WB 凭证
	 * @param request    创建 Supply 请求
	 * @return 创建 Supply 响应（包含 supplyId）
	 */
	public WbCreateSupplyResponse createSupplyTyped(
			WbCredential credential,
			WbCreateSupplyRequest request) {

		try {
			String body = request != null ? objectMapper.writeValueAsString(request) : "{}";

			return httpExecutor.execute(
					WbApiUtils.rateKey(credential.getApiKey()),
					HttpMethod.POST,
					WbApiUtils.createUri(MARKETPLACE_ENDPOINT + "/api/v3/supplies"),
					WbApiUtils.createJsonHeaders(credential.getApiKey()),
					body,
					WbCreateSupplyResponse.class);
		} catch (PlatformApiException e) {
			throw e;
		} catch (Exception e) {
			throw new PlatformApiException(PLATFORM, "PARSE_ERROR",
					"序列化创建 Supply 请求失败: " + e.getMessage(), 0, e);
		}
	}

	/**
	 * 将订单添加到批次
	 * <p>
	 * API: PATCH /api/marketplace/v3/supplies/{supplyId}/orders
	 *
	 * @param credential WB 凭证
	 * @param supplyId   批次ID
	 * @param orderId    wb orderId
	 */
	public void addOrdersToSupply(WbCredential credential, String supplyId, String orderId) {
		if (!StringUtils.hasText(supplyId)) {
			throw new IllegalArgumentException("supplyId 不能为空");
		}

		WbAddOrderToSupplierRequest request = new WbAddOrderToSupplierRequest();
		Long platformOrderId = Long.parseLong(orderId);
		request.setOrders(Collections.singletonList(platformOrderId));

		httpExecutor.execute(
				WbApiUtils.rateKey(credential.getApiKey()),
				HttpMethod.PATCH,
				WbApiUtils.createUri(MARKETPLACE_ENDPOINT + "/api/marketplace/v3/supplies/" + supplyId + "/orders"),
				WbApiUtils.createJsonHeaders(credential.getApiKey()),
				JsonUtils.toJson(request),
				Object.class);
	}

	/**
	 * 发货确认（关闭批次）
	 * <p>
	 * API: PATCH /api/v3/supplies/{supplyId}/deliver
	 *
	 * @param credential WB 凭证
	 * @param supplyId   批次ID
	 */
	public void shipSupply(WbCredential credential, String supplyId) {
		if (!StringUtils.hasText(supplyId)) {
			throw new IllegalArgumentException("supplyId 不能为空");
		}

		httpExecutor.execute(
				WbApiUtils.rateKey(credential.getApiKey()),
				HttpMethod.PATCH,
				WbApiUtils.createUri(MARKETPLACE_ENDPOINT + "/api/v3/supplies/" + supplyId + "/deliver"),
				WbApiUtils.createJsonHeaders(credential.getApiKey()),
				null,
				Object.class);
	}

	/**
	 * 获取批次列表（强类型版本）
	 * <p>
	 * API: GET /api/v3/supplies
	 *
	 * @param credential WB 凭证
	 * @param query      分页查询参数
	 * @return 批次列表响应
	 */
	public WbSupplyListResponse getSuppliesTyped(
			WbCredential credential,
			WbPaginationQuery query) {

		Map<String, Object> params = new HashMap<>();
		if (query != null) {
			if (query.getLimit() != null) {
				params.put("limit", query.getLimit());
			}
			if (query.getNext() != null) {
				params.put("next", query.getNext());
			}
		}

		URI uri = WbApiUtils.buildUri(MARKETPLACE_ENDPOINT + "/api/v3/supplies", params);
		return httpExecutor.execute(
				WbApiUtils.rateKey(credential.getApiKey()),
				HttpMethod.GET,
				uri,
				WbApiUtils.createHeaders(credential.getApiKey()),
				null,
				WbSupplyListResponse.class);
	}

	/**
	 * 获取批次详情（强类型版本）
	 * <p>
	 * API: GET /api/v3/supplies/{supplyId}
	 *
	 * @param credential WB 凭证
	 * @param supplyId   批次ID
	 * @return 批次详情
	 */
	public WbSupplyDetail getSupplyTyped(
			WbCredential credential, String supplyId) {

		if (!StringUtils.hasText(supplyId)) {
			throw new IllegalArgumentException("supplyId 不能为空");
		}

		return httpExecutor.execute(
				WbApiUtils.rateKey(credential.getApiKey()),
				HttpMethod.GET,
				WbApiUtils.createUri(MARKETPLACE_ENDPOINT + "/api/v3/supplies/" + supplyId),
				WbApiUtils.createHeaders(credential.getApiKey()),
				null,
				WbSupplyDetail.class);
	}

	/**
	 * 获取批次条码（强类型版本）
	 * <p>
	 * API: GET /api/v3/supplies/{supplyId}/barcode?type=png
	 *
	 * @param credential WB 凭证
	 * @param supplyId   批次ID
	 * @return 条码响应（包含 Base64 图片）
	 */
	public WbSupplyBarcodeResponse getSupplyBarcodeTyped(
			WbCredential credential, String supplyId) {

		if (!StringUtils.hasText(supplyId)) {
			throw new IllegalArgumentException("supplyId 不能为空");
		}

		Map<String, Object> params = new HashMap<>();
		params.put("type", "png");

		URI uri = WbApiUtils.buildUri(
				MARKETPLACE_ENDPOINT + "/api/v3/supplies/" + supplyId + "/barcode",
				params);

		return httpExecutor.execute(
				WbApiUtils.rateKey(credential.getApiKey()),
				HttpMethod.GET,
				uri,
				WbApiUtils.createHeaders(credential.getApiKey()),
				null,
				WbSupplyBarcodeResponse.class);
	}

	// ============================================================================
	// Offices（仓库/办公室）相关 API
	// ============================================================================

	/**
	 * 获取仓库/办公室列表（强类型版本）
	 * <p>
	 * API: GET /api/v3/offices
	 * <p>
	 * 注意：此 API 直接返回数组，不是对象包装
	 *
	 * @param credential WB 凭证
	 * @return 仓库列表
	 */
	public List<WbOffice> getOfficesTyped(
			WbCredential credential) {

		URI uri = WbApiUtils.createUri(MARKETPLACE_ENDPOINT + "/api/v3/offices");
		return httpExecutor.executeForList(
				WbApiUtils.rateKey(credential.getApiKey()),
				HttpMethod.GET,
				uri,
				WbApiUtils.createHeaders(credential.getApiKey()),
				null,
				WbOffice.class);
	}

	// ============================================================================
	// 财务报表相关 API
	// ============================================================================

	/**
	 * 获取财务报表明细（分页）
	 * <p>
	 * API: GET /api/v5/supplier/reportDetailByPeriod
	 * <p>
	 * 限流规则：1 分钟 1 次请求（独立限流）
	 * <p>
	 * 分页说明：
	 * - 首次请求：rrdid=0
	 * - 后续请求：使用上次响应中最后一条记录的 rrd_id
	 * - 直到返回空数组 [] 表示数据拉取完毕
	 *
	 * @param credential WB 凭证
	 * @param request    查询请求（包含日期范围、分页参数等）
	 * @return 财务报表明细列表
	 */
	public List<WbReportDetail> getReportDetailByPeriod(
			WbCredential credential,
			WbReportDetailRequest request) {

		if (request == null || !StringUtils.hasText(request.getDateFrom())
				|| !StringUtils.hasText(request.getDateTo())) {
			throw new IllegalArgumentException("dateFrom 和 dateTo 不能为空");
		}

		Map<String, Object> params = new HashMap<>();
		params.put("dateFrom", request.getDateFrom());
		params.put("dateTo", request.getDateTo());
		if (request.getLimit() != null) {
			params.put("limit", request.getLimit());
		}
		if (request.getRrdid() != null) {
			params.put("rrdid", request.getRrdid());
		}
		if (StringUtils.hasText(request.getPeriod())) {
			params.put("period", request.getPeriod());
		}

		String url = STATISTICS_ENDPOINT + "/api/v5/supplier/reportDetailByPeriod";
		URI uri = WbApiUtils.buildUri(url, params);

		return httpExecutor.executeForList(
				WbApiUtils.rateKey(credential.getApiKey()),
				HttpMethod.GET,
				uri,
				WbApiUtils.createHeaders(credential.getApiKey()),
				null,
				WbReportDetail.class,
				Duration.ofSeconds(100));
	}

}
