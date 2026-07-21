package com.erp.admin.platform.ozon;

import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.PlatformApiException;
import com.erp.admin.platform.ozon.credential.OzonCredential;
import com.erp.admin.platform.PlatformHttpExecutor;
import com.erp.admin.platform.ozon.model.request.act.OzonActCreateRequest;
import com.erp.admin.platform.ozon.model.request.act.OzonActIdRequest;
import com.erp.admin.platform.ozon.model.request.label.OzonPackageLabelRequest;
import com.erp.admin.platform.ozon.model.response.act.OzonActCheckStatusResponse;
import com.erp.admin.platform.ozon.model.response.act.OzonActCreateResponse;
import com.erp.admin.platform.ozon.model.request.posting.OzonFboPostingGetRequest;
import com.erp.admin.platform.ozon.model.request.posting.OzonFboPostingListRequest;
import com.erp.admin.platform.ozon.model.request.posting.OzonPostingGetRequest;
import com.erp.admin.platform.ozon.model.request.posting.OzonPostingListRequest;
import com.erp.admin.platform.ozon.model.request.product.OzonProductInfoListRequest;
import com.erp.admin.platform.ozon.model.request.product.OzonProductListRequest;
import com.erp.admin.platform.ozon.model.request.analytics.OzonAnalyticsStocksRequest;
import com.erp.admin.platform.ozon.model.request.posting.OzonShipRequest;
import com.erp.admin.platform.ozon.model.request.warehouse.OzonClusterListRequest;
import com.erp.admin.platform.ozon.model.request.warehouse.OzonFboWarehouseListRequest;
import com.erp.admin.platform.ozon.model.response.warehouse.OzonClusterListResponse;
import com.erp.admin.platform.ozon.model.response.warehouse.OzonFboWarehouseListResponse;
import com.erp.admin.platform.ozon.model.response.analytics.OzonAnalyticsStockItem;
import com.erp.admin.platform.ozon.model.response.analytics.OzonAnalyticsStocksResponse;
import com.erp.admin.platform.ozon.model.response.posting.OzonFboPosting;
import com.erp.admin.platform.ozon.model.response.posting.OzonFboPostingGetResponse;
import com.erp.admin.platform.ozon.model.response.posting.OzonFboPostingListResponse;
import com.erp.admin.platform.ozon.model.response.posting.OzonPosting;
import com.erp.admin.platform.ozon.model.response.posting.OzonPostingGetResponse;
import com.erp.admin.platform.ozon.model.response.posting.OzonPostingListResponse;
import com.erp.admin.platform.ozon.model.response.product.OzonProductInfoItem;
import com.erp.admin.platform.ozon.model.response.product.OzonProductInfoListResponse;
import com.erp.admin.platform.ozon.model.response.product.OzonProductItem;
import com.erp.admin.platform.ozon.model.response.product.OzonProductListResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ozon API 客户端
 * <p>
 * 职责：
 * 1. 封装 Ozon 所有 API 接口
 * 2. 组装请求参数
 * 3. 委托 PlatformHttpExecutor 处理 HTTP 请求
 * <p>
 * API 文档：https://docs.ozon.ru/api/seller/
 *
 * @author system
 */
@Component
@Slf4j
public class OzonClient {

    private static final String PLATFORM = PlatformEnum.Ozon.code();
    private static final String BASE_URL = "https://api-seller.ozon.ru";

    private final PlatformHttpExecutor httpExecutor;
    private final ObjectMapper objectMapper;

    public OzonClient(
            @Qualifier("ozonHttpExecutor") PlatformHttpExecutor httpExecutor,
            @Qualifier("platformObjectMapper") ObjectMapper objectMapper) {
        this.httpExecutor = httpExecutor;
        this.objectMapper = objectMapper;
    }

    // ============================================================================
    // 凭证验证
    // ============================================================================

    /**
     * 验证凭证有效性
     * <p>
     * API: POST /v1/roles
     * <p>
     * 此方法用于凭证验证，调用 roles API 验证 Client-Id 和 Api-Key 是否有效
     * <p>
     * 注意：Ozon API 没有提供获取卖家名称的接口，因此只做凭证验证，不返回店铺信息
     *
     * @param credential 凭证信息（必须包含 client_id 和 api_key）
     * @throws IllegalArgumentException client_id 或 api_key 为空
     * @throws PlatformApiException     凭证验证失败
     */
    public void validateCredential(Map<String, String> credential) {
        String clientId = credential.get("client_id");
        String apiKey = credential.get("api_key");

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("client_id 和 api_key 不能为空");
        }

        try {
            OzonCredential ozonCred = new OzonCredential(clientId, apiKey);

            // 调用 /v1/roles API 验证凭证
            String endpoint = BASE_URL + "/v1/roles";
            HttpHeaders headers = createHeaders(ozonCred);
            String requestBody = "{}";

            // 执行请求（如果凭证无效会抛出异常）
            httpExecutor.executeForString(
                    clientId,
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
					null);

            // 凭证有效，方法正常返回（void）

        } catch (PlatformApiException e) {
            log.error("[OZON] 凭证验证失败: code={}, msg={}", e.getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[OZON] 凭证验证异常", e);
            throw new PlatformApiException(PLATFORM, "CREDENTIAL_VALIDATION_ERROR",
                    "凭证验证异常: " + e.getMessage(), 0, e);
        }
    }

    // ============================================================================
    // 订单相关 API
    // ============================================================================

    /**
     * 获取订单列表
     * <p>
     * API: POST /v3/posting/fbs/list
     *
     * @param credential Ozon 凭证
     * @param request    订单列表请求参数
     * @return 订单列表响应
     */
    public OzonPostingListResponse getPostings(OzonCredential credential, OzonPostingListRequest request) {
        try {
            String endpoint = BASE_URL + "/v3/posting/fbs/list";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(request);

            return httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonPostingListResponse.class);
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 获取订单详情
     * <p>
     * API: POST /v3/posting/fbs/get
     *
     * @param credential Ozon 凭证
     * @param request    订单详情请求参数
     * @return 订单详情
     */
    public OzonPosting getPosting(OzonCredential credential, OzonPostingGetRequest request) {
        try {
            String endpoint = BASE_URL + "/v3/posting/fbs/get";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(request);

            OzonPostingGetResponse response = httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonPostingGetResponse.class);

            return response.getResult();
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 获取订单详情（简化版本，只需要 postingNumber）
     *
     * @param credential    Ozon 凭证
     * @param postingNumber 发货单号
     * @return 订单详情
     */
    public OzonPosting getPosting(OzonCredential credential, String postingNumber) {
        OzonPostingGetRequest request = OzonPostingGetRequest.builder()
                .postingNumber(postingNumber)
                .build();
        return getPosting(credential, request);
    }

    // ============================================================================
    // 发货相关 API
    // ============================================================================

    /**
     * 发货订单（打包订单）
     * <p>
     * API: POST /v4/posting/fbs/ship
     * <p>
     * 将订单状态更改为 awaiting_deliver（等待发货）
     *
     * @param credential Ozon 凭证
     * @param request    发货请求参数
     */
    public void shipPosting(OzonCredential credential, OzonShipRequest request) {
        try {
            String endpoint = BASE_URL + "/v4/posting/fbs/ship";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(request);

            httpExecutor.executeForString(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
					null);

            log.info("[OZON] 订单发货成功: postingNumber={}", request.getPostingNumber());
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    // ============================================================================
    // 面单相关 API
    // ============================================================================

    /**
     * 获取包裹面单（PDF 格式）
     * <p>
     * API: POST /v2/posting/fbs/package-label
     * <p>
     * 返回包含 Base64 编码的 PDF 数据的响应对象，最多支持 20 个发货单号
     *
     * @param credential Ozon 凭证
     * @param request    面单请求参数
     * @return pdf 文件内容
     */
    public byte[] getPackageLabel(OzonCredential credential, OzonPackageLabelRequest request) {
        if (request.getPostingNumber().size() > 20) {
            throw new IllegalArgumentException("最多支持 20 个发货单号");
        }

        try {
            String endpoint = BASE_URL + "/v2/posting/fbs/package-label";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(request);

            // 这里是一个坑，Ozon API 文档有问题，接口返回的是一个字节数组，而不是一个字符串
            byte[] fileContent = httpExecutor.executeForBytes(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody);

            log.info("[OZON] 获取面单成功: postingNumbers={}, fileSize={}",
                    request.getPostingNumber(), fileContent.length);

            return fileContent;
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    // ============================================================================
    // 运单（交接单 act）相关 API
    //
    // 三段式异步：create 拿到 actId → 轮询 check-status 至就绪 → get-pdf 取 PDF。
    // Ozon 侧生成通常需 1~2 分钟。运单按「物流方式 + 发货日期」汇总当日货件，
    // 其内容不等于调用方选中的订单集合。
    // ============================================================================

    /**
     * 创建运单
     * <p>
     * API: POST /v2/posting/fbs/act/create
     *
     * @param credential Ozon 凭证
     * @param request    创建参数（物流方式 / 发货日期 / 箱数）
     * @return Ozon 侧运单 ID
     */
    public Long createAct(OzonCredential credential, OzonActCreateRequest request) {
        try {
            String endpoint = BASE_URL + "/v2/posting/fbs/act/create";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(request);

            OzonActCreateResponse response = httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonActCreateResponse.class);

            Long actId = response != null ? response.actId() : null;
            if (actId == null) {
                throw new PlatformApiException(PLATFORM, "ACT_CREATE_NO_ID",
                        "创建运单成功但未返回运单ID", 0);
            }

            log.info("[OZON] 创建运单成功: deliveryMethodId={}, departureDate={}, actId={}",
                    request.getDeliveryMethodId(), request.getDepartureDate(), actId);
            return actId;
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 查询运单生成状态
     * <p>
     * API: POST /v2/posting/fbs/act/check-status
     *
     * @param credential Ozon 凭证
     * @param actId      Ozon 侧运单 ID
     * @return 原始状态串（如 in_process / ready），不做枚举映射
     */
    public String checkActStatus(OzonCredential credential, Long actId) {
        try {
            String endpoint = BASE_URL + "/v2/posting/fbs/act/check-status";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(
                    OzonActIdRequest.builder().id(actId).build());

            OzonActCheckStatusResponse response = httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonActCheckStatusResponse.class);

            String status = response != null ? response.status() : null;
            log.info("[OZON] 运单状态: actId={}, status={}", actId, status);
            return status;
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 下载运单 PDF
     * <p>
     * API: POST /v2/posting/fbs/act/get-pdf
     * <p>
     * 与 package-label 一样，该接口直接返回二进制 PDF 而非 JSON。
     * 必须在 check-status 返回就绪后调用，否则 Ozon 会报错。
     *
     * @param credential Ozon 凭证
     * @param actId      Ozon 侧运单 ID
     * @return PDF 文件字节
     */
    public byte[] getActPdf(OzonCredential credential, Long actId) {
        try {
            String endpoint = BASE_URL + "/v2/posting/fbs/act/get-pdf";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(
                    OzonActIdRequest.builder().id(actId).build());

            byte[] fileContent = httpExecutor.executeForBytes(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody);

            log.info("[OZON] 下载运单成功: actId={}, fileSize={}", actId,
                    fileContent != null ? fileContent.length : 0);
            return fileContent;
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    // ============================================================================
    // FBO 订单相关 API
    // ============================================================================

    /**
     * 获取 FBO 订单列表
     * <p>
     * API: POST /v2/posting/fbo/list
     * <p>
     * FBO (Fulfillment by Ozon) 订单由 Ozon 仓库发货，卖家只能查看订单数据
     *
     * @param credential Ozon 凭证
     * @param request    FBO 订单列表请求参数
     * @return FBO 订单列表响应
     */
    public OzonFboPostingListResponse getFboPostings(OzonCredential credential, OzonFboPostingListRequest request) {
        try {
            String endpoint = BASE_URL + "/v2/posting/fbo/list";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(request);

            return httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonFboPostingListResponse.class);
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 获取 FBO 订单详情
     * <p>
     * API: POST /v2/posting/fbo/get
     * <p>
     * FBO 订单由 Ozon 仓库发货，卖家只能查看订单数据
     *
     * @param credential Ozon 凭证
     * @param request    FBO 订单详情请求参数
     * @return FBO 订单详情
     */
    public OzonFboPosting getFboPosting(OzonCredential credential, OzonFboPostingGetRequest request) {
        try {
            String endpoint = BASE_URL + "/v2/posting/fbo/get";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(request);

            OzonFboPostingGetResponse response = httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonFboPostingGetResponse.class);

            return response.getResult();
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 获取 FBO 订单详情（简化版本，只需要 postingNumber）
     *
     * @param credential    Ozon 凭证
     * @param postingNumber 发货单号
     * @return FBO 订单详情
     */
    public OzonFboPosting getFboPosting(OzonCredential credential, String postingNumber) {
        OzonFboPostingGetRequest request = OzonFboPostingGetRequest.builder()
                .postingNumber(postingNumber)
                .build();
        return getFboPosting(credential, request);
    }

    // ============================================================================
    // FBO 仓库相关 API
    // ============================================================================

    /**
     * 获取集群仓库列表
     * <p>
     * API: POST /v1/cluster/list
     * <p>
     * 返回仓库的基本信息（ID、名称、类型），不包含地址
     *
     * @param credential  Ozon 凭证
     * @param clusterType 集群类型：CLUSTER_TYPE_OZON 或 CLUSTER_TYPE_CIS
     * @return 集群列表响应
     */
    public OzonClusterListResponse getClusterList(OzonCredential credential, String clusterType) {
        try {
            String endpoint = BASE_URL + "/v1/cluster/list";
            HttpHeaders headers = createHeaders(credential);

            OzonClusterListRequest request = OzonClusterListRequest.builder()
                    .clusterType(clusterType)
                    .build();

            String requestBody = objectMapper.writeValueAsString(request);

            return httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonClusterListResponse.class);
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 搜索 FBO 仓库（获取地址信息）
     * <p>
     * API: POST /v1/warehouse/fbo/list
     * <p>
     * 注意：search 参数最少需要 4 个字符，filter_by_supply_type 是必填字段
     *
     * @param credential Ozon 凭证
     * @param request    仓库搜索请求
     * @return 仓库列表响应
     */
    public OzonFboWarehouseListResponse searchFboWarehouses(OzonCredential credential,
            OzonFboWarehouseListRequest request) {
        try {
            String endpoint = BASE_URL + "/v1/warehouse/fbo/list";
            HttpHeaders headers = createHeaders(credential);
            String requestBody = objectMapper.writeValueAsString(request);

            return httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonFboWarehouseListResponse.class);
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    // ============================================================================
    // FBO 库存相关 API
    // ============================================================================

    /**
     * 获取 FBO 库存余额分析
     * <p>
     * API: POST /v1/analytics/stocks
     * <p>
     * 注意：此接口需要传入 SKU 列表，最多 100 个。
     * 如需获取全部库存，请先调用 getAllProductSkus() 获取 SKU 列表，再分批调用此方法。
     * <p>
     * 响应中包含 warehouse_id 和 warehouse_name，可用于「被动发现」自动创建仓库。
     *
     * @param credential Ozon 凭证
     * @param skus SKU 列表（最多 100 个）
     * @return FBO 库存项列表
     */
    public List<OzonAnalyticsStockItem> getFboStocks(OzonCredential credential, List<Long> skus) {
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyList();
        }
        if (skus.size() > 100) {
            throw new IllegalArgumentException("SKU 列表最多 100 个，当前: " + skus.size());
        }

        try {
            String endpoint = BASE_URL + "/v1/analytics/stocks";
            HttpHeaders headers = createHeaders(credential);

            OzonAnalyticsStocksRequest request = OzonAnalyticsStocksRequest.builder()
                    .skus(skus)
                    .build();

            String requestBody = objectMapper.writeValueAsString(request);

            OzonAnalyticsStocksResponse response = httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonAnalyticsStocksResponse.class);

            List<OzonAnalyticsStockItem> items = response.getItems();
            if (items == null) {
                return Collections.emptyList();
            }

            return items;
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 分批获取 FBO 库存余额分析（自动分批，每批 100 个 SKU）
     * <p>
     * 内部自动将 SKU 列表拆分为多批，每批最多 100 个，逐批调用 /v1/analytics/stocks。
     *
     * @param credential Ozon 凭证
     * @param allSkus 全部 SKU 列表
     * @return 全部 FBO 库存项列表
     */
    public List<OzonAnalyticsStockItem> getFboStocksBatch(OzonCredential credential, List<Long> allSkus) {
        if (allSkus == null || allSkus.isEmpty()) {
            return Collections.emptyList();
        }

        List<OzonAnalyticsStockItem> result = new ArrayList<>();
        int batchSize = 100;

        for (int i = 0; i < allSkus.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allSkus.size());
            List<Long> batch = allSkus.subList(i, end);

            List<OzonAnalyticsStockItem> batchResult = getFboStocks(credential, batch);
            result.addAll(batchResult);

            log.debug("FBO 库存查询进度: {}/{}", end, allSkus.size());
        }

        return result;
    }

    /**
     * 获取店铺所有商品列表
     * <p>
     * API: POST /v3/product/list
     * <p>
     * 自动分页获取所有商品，返回基础商品信息（不含 SKU）。
     * 如需获取 SKU，请使用 getProductInfoList() 方法。
     *
     * @param credential Ozon 凭证
     * @return 商品列表
     */
    public List<OzonProductItem> getProducts(OzonCredential credential) {
        List<OzonProductItem> allProducts = new ArrayList<>();
        String lastId = "";
        int limit = 1000;

        try {
            while (true) {
                String endpoint = BASE_URL + "/v3/product/list";
                HttpHeaders headers = createHeaders(credential);

                OzonProductListRequest request = OzonProductListRequest.builder()
						.filter(new OzonProductListRequest.Filter())
                        .limit(limit)
                        .lastId(lastId)
                        .build();

                String requestBody = objectMapper.writeValueAsString(request);

                OzonProductListResponse response = httpExecutor.execute(
                        credential.getClientId(),
                        HttpMethod.POST,
                        URI.create(endpoint),
                        headers,
                        requestBody,
                        OzonProductListResponse.class);

                OzonProductListResponse.Result result = response.getResult();
                if (result == null || result.getItems() == null || result.getItems().isEmpty()) {
                    break;
                }

                allProducts.addAll(result.getItems());

                // 获取下一页
                lastId = result.getLastId();
                if (lastId == null || lastId.isEmpty() || result.getItems().size() < limit) {
                    break;
                }
            }

            log.info("获取商品列表完成，共 {} 个", allProducts.size());
            return allProducts;
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 获取商品详情列表（包含 SKU）
     * <p>
     * API: POST /v3/product/info/list
     * <p>
     * 通过 product_id 列表获取商品详细信息，包含 SKU。
     * 一次请求最多传入 1000 个标识符。
     *
     * @param credential Ozon 凭证
     * @param productIds 商品 ID 列表（最多 1000 个）
     * @return 商品详情列表
     */
    public List<OzonProductInfoItem> getProductInfoList(OzonCredential credential, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (productIds.size() > 1000) {
            throw new IllegalArgumentException("商品 ID 列表最多 1000 个，当前: " + productIds.size());
        }

        try {
            String endpoint = BASE_URL + "/v3/product/info/list";
            HttpHeaders headers = createHeaders(credential);

            OzonProductInfoListRequest request = OzonProductInfoListRequest.builder()
                    .productIds(productIds)
                    .build();

            String requestBody = objectMapper.writeValueAsString(request);

            OzonProductInfoListResponse response = httpExecutor.execute(
                    credential.getClientId(),
                    HttpMethod.POST,
                    URI.create(endpoint),
                    headers,
                    requestBody,
                    OzonProductInfoListResponse.class);

            List<OzonProductInfoItem> items = response.getItems();
            if (items == null) {
                return Collections.emptyList();
            }

            return items;
        } catch (PlatformApiException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new PlatformApiException(PLATFORM, "REQUEST_SERIALIZE_ERROR",
                    "序列化请求失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 分批获取商品详情列表（自动分批，每批 1000 个）
     * <p>
     * 内部自动将 product_id 列表拆分为多批，每批最多 1000 个。
     *
     * @param credential Ozon 凭证
     * @param allProductIds 全部商品 ID 列表
     * @return 全部商品详情列表
     */
    public List<OzonProductInfoItem> getProductInfoListBatch(OzonCredential credential, List<Long> allProductIds) {
        if (allProductIds == null || allProductIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<OzonProductInfoItem> result = new ArrayList<>();
        int batchSize = 1000;

        for (int i = 0; i < allProductIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allProductIds.size());
            List<Long> batch = allProductIds.subList(i, end);

            List<OzonProductInfoItem> batchResult = getProductInfoList(credential, batch);
            result.addAll(batchResult);

            log.debug("商品详情查询进度: {}/{}", end, allProductIds.size());
        }

        return result;
    }

    /**
     * 获取店铺所有商品的 SKU 列表
     * <p>
     * 正确流程：
     * 1. 调用 /v3/product/list 获取所有 product_id
     * 2. 调用 /v3/product/info/list 获取 sku
     * <p>
     * 注意：/v3/product/list 不返回 sku，必须通过 /v3/product/info/list 获取。
     *
     * @param credential Ozon 凭证
     * @return 商品 SKU 列表
     */
    public List<Long> getAllProductSkus(OzonCredential credential) {
        // Step 1: 获取所有商品的 product_id
        List<OzonProductItem> products = getProducts(credential);
        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = products.stream()
                .map(OzonProductItem::getProductId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 2: 通过 product_id 获取 sku
        List<OzonProductInfoItem> productInfoList = getProductInfoListBatch(credential, productIds);

        return productInfoList.stream()
                .map(OzonProductInfoItem::getSku)
                .filter(sku -> sku != null && sku > 0)
                .collect(Collectors.toList());
    }

    // ============================================================================
    // 工具方法
    // ============================================================================

    /**
     * 创建请求头
     *
     * @param credential Ozon 凭证
     * @return HTTP 请求头
     */
    private HttpHeaders createHeaders(OzonCredential credential) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Client-Id", credential.getClientId());
        headers.set("Api-Key", credential.getApiKey());
        return headers;
    }

}
