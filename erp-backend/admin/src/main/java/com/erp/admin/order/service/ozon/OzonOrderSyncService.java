package com.erp.admin.order.service.ozon;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.dto.ozon.OzonPostingSyncDTO;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.enums.ErpOrderStatusEnum;
import com.erp.admin.order.model.vo.SyncSummaryVO;
import com.erp.admin.order.service.OrderLifecycleService;
import com.erp.admin.order.service.ozon.converter.OzonOrderStatusConverter;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.sync.model.enums.SyncTaskTypeEnum;
import com.erp.admin.sync.service.SyncCursorService;
import com.erp.admin.platform.ozon.OzonClient;
import com.erp.admin.platform.ozon.credential.OzonCredential;
import com.erp.admin.platform.ozon.model.request.posting.OzonFboPostingFilter;
import com.erp.admin.platform.ozon.model.request.posting.OzonFboPostingListRequest;
import com.erp.admin.platform.ozon.model.request.posting.OzonPostingFilter;
import com.erp.admin.platform.ozon.model.request.posting.OzonPostingListRequest;
import com.erp.admin.platform.ozon.model.request.posting.OzonPostingWith;
import com.erp.admin.platform.ozon.model.response.posting.OzonFboPosting;
import com.erp.admin.platform.ozon.model.response.posting.OzonFboPostingListResponse;
import com.erp.admin.platform.ozon.model.response.posting.OzonPosting;
import com.erp.admin.platform.ozon.model.response.posting.OzonPostingListResponse;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * OzonOrderSyncService - Ozon 订单同步服务
 * <p>
 * 职责：
 * - 整合所有同步逻辑（FBS 订单、FBO 订单）
 * - 直接调用 OzonClient 和 OzonOrderUpsertService
 * - 负责同步业务逻辑（时间窗口计算、数据拉取、持久化）
 * <p>
 * 注意：
 * - 单个订单转换/落库失败仅记日志跳过，不中断整店同步（漏掉的订单由未完结状态回溯兜底重试）
 * - 分页请求级别的失败（网络/API错误）仍向上抛出，中止本店铺本轮同步、游标不推进
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OzonOrderSyncService {

	private final OzonClient ozonClient;
	private final OzonOrderUpsertService ozonOrderUpsertService;
	private final OzonPlatformApi ozonPlatformApi;
	private final OrderLifecycleService lifecycleService;
	private final ShopService shopService;
	private final ObjectMapper objectMapper;
	private final ErpOrderMapper erpOrderMapper;
	private final CredentialService credentialService;
	private final SyncCursorService syncCursorService;

	private static final int DEFAULT_PAGE_SIZE = 1000; // Ozon API 最大支持 1000
	private static final int DEFAULT_LOOKBACK_DAYS = 30; // 默认同步最近 30 天的订单
	private static final int MAX_TIME_RANGE_DAYS = 90; // Ozon API 时间范围限制：使用 90 天分段，避免触发 PERIOD_IS_TOO_LONG 错误
	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private static final LocalDateTime DEFAULT_START_DATE = LocalDateTime.of(2025, 1, 1, 0, 0, 0); // 默认起始日期：2025-01-01

	/**
	 * 同步所有启用店铺的订单（FBS + FBO）
	 *
	 * @param fetchAll 是否全量同步（从 DEFAULT_START_DATE=2025-01-01 开始）
	 */
	public void syncAllShops(boolean fetchAll) {
		List<Shop> shops = shopService.listEnabledByPlatform(PlatformEnum.Ozon.code());
		if (CollectionUtils.isEmpty(shops)) {
			log.debug("[OZON][SYNC] 没有启用的店铺");
			return;
		}

		for (Shop shop : shops) {
			try {
				// 多租户：按店铺所属货主建立上下文，确保同步落库 tenant_id 正确
				TenantContext.runAs(shop.getTenantId(), () -> {
					syncShopOrders(shop, fetchAll);
					return null;
				});
			} catch (Exception e) {
				log.error("[OZON][SYNC] 店铺 {} 同步失败: {}", shop.getId(), e.getMessage(), e);
			}
		}
	}

	/**
	 * 同步单个店铺的订单（智能计算时间窗口）
	 *
	 * @param shop     店铺对象
	 * @param fetchAll 是否全量同步
	 */
	private void syncShopOrders(Shop shop, boolean fetchAll) {
		// 1. 计算同步窗口
		LocalDateTime windowStart = resolveWindowStart(shop.getId(), fetchAll);
		LocalDateTime windowEnd = LocalDateTime.now(ZoneOffset.UTC);

		// 2. 兜底：避免 dateFrom > dateTo
		if (windowStart.isAfter(windowEnd)) {
			windowStart = windowEnd.minusDays(1);
		}

		log.info("[OZON][SYNC] 店铺 {} 同步窗口: {} ~ {}", shop.getId(), windowStart, windowEnd);

		// 3. 执行同步
		syncShopPostings(shop.getId(), windowStart, windowEnd);
	}

	/**
	 * 计算同步窗口起始时间（基于 sync_cursor 游标）
	 *
	 * @param shopId   店铺ID
	 * @param fetchAll 是否全量同步
	 * @return 窗口起始时间（UTC）
	 */
	private LocalDateTime resolveWindowStart(Long shopId, boolean fetchAll) {
		if (fetchAll) {
			return DEFAULT_START_DATE;
		}

		try {
			LocalDateTime cursor = syncCursorService.getCursor(
					shopId, PlatformEnum.Ozon.code(), SyncTaskTypeEnum.ORDER_INCREMENTAL);

			if (cursor == null) {
				log.info("[OZON][SYNC] 店铺 {} 首次同步，从 {} 开始", shopId, DEFAULT_START_DATE);
				return DEFAULT_START_DATE;
			}

			LocalDateTime startTime = cursor.minusMinutes(30);

			log.info("[OZON][SYNC] 店铺 {} 增量同步，游标={}, 回溯 30min, 起点={}",
					shopId, cursor, startTime);
			return startTime;

		} catch (Exception e) {
			log.warn("[OZON][SYNC] 获取游标失败，使用默认回溯: {}", e.getMessage());
			return LocalDateTime.now(ZoneOffset.UTC).minusDays(DEFAULT_LOOKBACK_DAYS);
		}
	}

	/**
	 * 同步所有启用店铺的未完结订单状态
	 */
	public void syncAllEnabledShopsUnfinishedOrderStatuses() {
		List<Shop> shops = shopService.listEnabledByPlatform(PlatformEnum.Ozon.code());
		if (CollectionUtils.isEmpty(shops)) {
			log.debug("[OZON][STATUS_SYNC] 没有启用的店铺");
			return;
		}

		for (Shop shop : shops) {
			try {
				TenantContext.runAs(shop.getTenantId(), () -> {
					syncShopUnfinishedOrderStatuses(shop.getId());
					return null;
				});
			} catch (Exception e) {
				log.error("[OZON][STATUS_SYNC] 店铺 {} 同步未完结订单状态失败: {}", shop.getId(), e.getMessage(), e);
			}
		}
	}

	/**
	 * 同步单个店铺的未完结订单状态
	 * <p>
	 * 查询状态为 PENDING, READY_TO_SHIP, SHIPPED, ARRIVED_AT_PLATFORM_WAREHOUSE 的订单
	 * 找到最早的未完结订单时间，重新同步该时间段的所有订单
	 *
	 * @param shopId 店铺ID
	 */
	public void syncShopUnfinishedOrderStatuses(Long shopId) {
		Shop shop = shopService.getById(shopId);
		if (shop == null) {
			log.warn("[OZON][STATUS_SYNC] 店铺不存在 shopId={}", shopId);
			return;
		}

		// 1. 查询未完结订单
		LambdaQueryWrapper<ErpOrder> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ErpOrder::getShopId, shopId)
				.eq(ErpOrder::getPlatform, PlatformEnum.Ozon.code())
				.in(ErpOrder::getErpStatus,
						 "READY_TO_SHIP", "SHIPPED", "ARRIVED_AT_PLATFORM_WAREHOUSE")
				.orderByAsc(ErpOrder::getPlatformCreatedAt)
				.last("limit 1");

		ErpOrder earliestUnfinished = erpOrderMapper.selectOne(wrapper);

		if (earliestUnfinished == null) {
			log.debug("[OZON][STATUS_SYNC] 店铺 {} 没有未完结订单", shopId);
			return;
		}

		// 2. 确定同步时间范围：从最早未完结订单时间开始
		LocalDateTime since = earliestUnfinished.getPlatformCreatedAt() != null
				? earliestUnfinished.getPlatformCreatedAt()
				: earliestUnfinished.getCreateTime();

		if (since == null) {
			since = LocalDateTime.now(ZoneOffset.UTC).minusDays(7);
		} else {
			// 回溯 1 小时，确保不遗漏
			since = since.minusHours(1);
		}

		LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);

		log.info("[OZON][STATUS_SYNC] 店铺 {} 开始同步未完结订单状态，时间范围: {} ~ {}",
				shopId, since, to);

		// 3. 重新同步该时间段的所有订单
		try {
			syncShopPostings(shopId, since, to);
			log.info("[OZON][STATUS_SYNC] 店铺 {} 未完结订单状态同步完成", shopId);
		} catch (Exception e) {
			log.error("[OZON][STATUS_SYNC] 店铺 {} 未完结订单状态同步失败: {}", shopId, e.getMessage(), e);
		}
	}

	/**
	 * 同步店铺所有订单（FBS + FBO）
	 * <p>
	 * 注意：按 MAX_TIME_RANGE_DAYS=90 天分段请求，避免触发 Ozon API 的 PERIOD_IS_TOO_LONG 限制
	 *
	 * @param shopId 店铺ID
	 * @param since  开始时间
	 * @param to     结束时间
	 */
	public void syncShopPostings(Long shopId, LocalDateTime since, LocalDateTime to) {
		log.info("[OZON][SYNC] 开始同步店铺订单: shopId={}, since={}, to={}", shopId, since, to);

		try {
			// 按 90 天分段同步（Ozon API 时间范围限制）
			LocalDateTime cursor = since;
			while (!cursor.isAfter(to)) {
				LocalDateTime segmentEnd = cursor.plusDays(MAX_TIME_RANGE_DAYS);
				if (segmentEnd.isAfter(to)) {
					segmentEnd = to;
				}

				log.debug("[OZON][SYNC] 店铺 {} 同步片段: {} ~ {}", shopId, cursor, segmentEnd);

				// 1. 同步 FBS 订单
				boolean fbsOk = syncShopFbsPostings(shopId, cursor, segmentEnd);

				// 2. 同步 FBO 订单
				boolean fboOk = syncShopFboPostings(shopId, cursor, segmentEnd);

				if (!fbsOk || !fboOk) {
					log.warn("[OZON][SYNC] 店铺 {} 片段 {}~{} 存在订单入库失败，保留游标不推进，下轮重试（若持续失败请人工排查坏单）",
								shopId, cursor, segmentEnd);
					break;
				}

				// 移动到下一个时间段
				cursor = segmentEnd.plusSeconds(1);

				// 阶段性同步完成，推进游标
				syncCursorService.advanceCursor(
						shopId, PlatformEnum.Ozon.code(), SyncTaskTypeEnum.ORDER_INCREMENTAL, segmentEnd);
			}

			log.info("[OZON][SYNC] 店铺订单同步完成: shopId={}", shopId);
		} catch (RuntimeException e) {
			// 检查是否为账户被封禁错误
			if (e.getMessage() != null && e.getMessage().contains("Company is blocked")) {
				log.error("[OZON][SYNC] ⚠️ 店铺账户被封禁: shopId={}. 错误信息: {}. 请联系 Ozon 客服解决此问题!",
						shopId, e.getMessage());
				throw new RuntimeException("店铺账户已被 Ozon 封禁,请联系 Ozon 客服解决 (Shop ID: " + shopId + ")", e);
			}
			throw e;
		}
	}

	/**
	 * 同步店铺 FBS 订单
	 * <p>
	 * 注意：不捕获多商品订单异常，让异常向上传播，停止整个同步流程
	 *
	 * @param shopId 店铺ID
	 * @param since  开始时间
	 * @param to     结束时间
	 */
	public boolean syncShopFbsPostings(Long shopId, LocalDateTime since, LocalDateTime to) {
		log.info("[OZON] 开始同步 FBS 订单: shopId={}, since={}, to={}", shopId, since, to);

		// 1. 获取店铺信息
		Shop shop = shopService.getById(shopId);
		if (shop == null) {
			log.warn("[OZON] 店铺不存在: shopId={}", shopId);
			return true;
		}

		// 2. 获取店铺凭证
		OzonCredential credential;
		try {
			credential = credentialService.parseCredential(shop);
		} catch (Exception ex) {
			log.error("[OZON] 店铺凭证解析失败: shopId={}, error={}", shopId, ex.getMessage());
			return false;
		}

		// 是否本段全部订单都成功入库（有任一单失败则不推进游标，避免漏单）
		boolean allOk = true;
		int successCount = 0;
		int offset = 0;
		boolean hasNext = true;

		// 3. 分页拉取订单
		while (hasNext) {
			try {
				// 构建请求
				OzonPostingFilter filter = OzonPostingFilter.builder()
						.since(since.atOffset(ZoneOffset.UTC))
						.to(to.atOffset(ZoneOffset.UTC))
						.build();

				OzonPostingWith with = OzonPostingWith.builder()
						.financialData(true)
						.build();

				OzonPostingListRequest request = OzonPostingListRequest.builder()
						.filter(filter)
						.limit(DEFAULT_PAGE_SIZE)
						.offset(offset)
						.with(with)
						.dir("ASC")
						.build();

				// 调用 API
				OzonPostingListResponse response = ozonClient.getPostings(credential, request);

				if (response == null || response.getResult() == null) {
					break;
				}

				List<OzonPosting> postings = response.getResult().getPostings();
				if (CollectionUtils.isEmpty(postings)) {
					break;
				}

				// 4. 处理订单（单单隔离：一单失败记日志跳过，不中断整店）
				for (OzonPosting posting : postings) {
					try {
						OzonPostingSyncDTO dto = convertToDTO(posting, "FBS");
						ozonOrderUpsertService.upsertOrder(shopId, dto);
						successCount++;
					} catch (Exception e) {
						allOk = false;
						log.error("[OZON] FBS 订单处理失败，跳过继续: posting_number={}, error={}",
								posting.getPostingNumber(), e.getMessage(), e);
					}
				}

				// 5. 检查是否有下一页
				hasNext = Boolean.TRUE.equals(response.getResult().getHasNext());
				offset += DEFAULT_PAGE_SIZE;

				log.debug("[OZON] FBS 订单同步进度: shopId={}, offset={}, count={}", shopId, offset, postings.size());

			} catch (Exception e) {
				// 不捕获多商品订单异常，直接向上抛出
				log.error("[OZON] FBS 订单同步失败: shopId={}, offset={}, error={}", shopId, offset, e.getMessage());
				throw new RuntimeException("FBS 订单同步失败: " + e.getMessage(), e);
			}
		}

		log.info("[OZON] FBS 订单同步完成: shopId={}, 成功={}", shopId, successCount);
		return allOk;
	}

	/**
	 * 同步店铺 FBO 订单
	 * <p>
	 * 注意：不捕获多商品订单异常，让异常向上传播，停止整个同步流程
	 *
	 * @param shopId 店铺ID
	 * @param since  开始时间
	 * @param to     结束时间
	 */
	public boolean syncShopFboPostings(Long shopId, LocalDateTime since, LocalDateTime to) {
		log.info("[OZON] 开始同步 FBO 订单: shopId={}, since={}, to={}", shopId, since, to);

		// 1. 获取店铺信息
		Shop shop = shopService.getById(shopId);
		if (shop == null) {
			log.warn("[OZON] 店铺不存在: shopId={}", shopId);
			return true;
		}

		// 2. 获取店铺凭证
		OzonCredential credential;
		try {
			credential = credentialService.parseCredential(shop);
		} catch (Exception ex) {
			log.error("[OZON] 店铺凭证解析失败: shopId={}, error={}", shopId, ex.getMessage());
			return false;
		}

		boolean allOk = true;
		int successCount = 0;
		int offset = 0;
		boolean hasNext = true;

		// 3. 分页拉取订单
		while (hasNext) {
			try {
				// 构建请求
				OzonFboPostingFilter filter = OzonFboPostingFilter.builder()
						.since(formatDateTime(since))
						.to(formatDateTime(to))
						.build();

				OzonPostingWith with = OzonPostingWith.builder()
						.financialData(true)
						.build();

				OzonFboPostingListRequest request = OzonFboPostingListRequest.builder()
						.filter(filter)
						.limit(DEFAULT_PAGE_SIZE)
						.with(with)
						.offset(offset)
						.dir("ASC")
						.build();

				// 调用 API
				OzonFboPostingListResponse response = ozonClient.getFboPostings(credential, request);

				if (response == null || response.getResult() == null) {
					break;
				}

				// FBO API 的 result 字段直接是订单数组
				List<OzonFboPosting> postings = response.getResult();
				if (CollectionUtils.isEmpty(postings)) {
					break;
				}

				// 4. 处理订单（单单隔离：一单失败记日志跳过，不中断整店）
				for (OzonFboPosting posting : postings) {
					try {
						OzonPostingSyncDTO dto = convertFboToDTO(posting, "FBO");
						ozonOrderUpsertService.upsertOrder(shopId, dto);
						successCount++;
					} catch (Exception e) {
						allOk = false;
						log.error("[OZON] FBO 订单处理失败，跳过继续: posting_number={}, error={}",
								posting.getPostingNumber(), e.getMessage(), e);
					}
				}

				// 5. 检查是否有下一页
				// FBO API 不返回 has_next 字段，通过返回的订单数量判断
				hasNext = postings.size() >= DEFAULT_PAGE_SIZE;
				offset += DEFAULT_PAGE_SIZE;

				log.debug("[OZON] FBO 订单同步进度: shopId={}, offset={}, count={}", shopId, offset, postings.size());

			} catch (Exception e) {
				// 不捕获多商品订单异常，直接向上抛出
				log.error("[OZON] FBO 订单同步失败: shopId={}, offset={}, error={}", shopId, offset, e.getMessage());
				throw new RuntimeException("FBO 订单同步失败: " + e.getMessage(), e);
			}
		}

		log.info("[OZON] FBO 订单同步完成: shopId={}, 成功={}", shopId, successCount);
		return allOk;
	}

	/**
	 * 转换 FBS 订单为 DTO
	 *
	 * @param posting         Ozon 订单
	 * @param fulfillmentType 履约类型
	 * @return 订单 DTO
	 */
	private OzonPostingSyncDTO convertToDTO(OzonPosting posting, String fulfillmentType) {
		try {
			// 序列化财务数据
			String financialDataJson = posting.getFinancialData() != null
					? objectMapper.writeValueAsString(posting.getFinancialData())
					: null;

			// 序列化原始 JSON
			String rawJson = objectMapper.writeValueAsString(posting);

			// 构建配送方式字符串
			String deliveryMethod = null;
			if (posting.getDeliveryMethod() != null) {
				deliveryMethod = String.format("%s (ID: %d)",
						posting.getDeliveryMethod().getName(),
						posting.getDeliveryMethod().getId());
			}

			// 获取仓库ID (FBS: 从 delivery_method 中获取)
			Long warehouseId = posting.getDeliveryMethod() != null ? posting.getDeliveryMethod().getWarehouseId() : null;

			// 发货仓库名（仅 FBS 有 delivery_method）；仓库名含「大」字即大仓，决定是否需要生成运单(act)
			String warehouseName = posting.getDeliveryMethod() != null
					? posting.getDeliveryMethod().getWarehouse()
					: null;

			// 获取商品价格和币种
			String productPrice = null;
			String productCurrencyCode = null;
			if (!CollectionUtils.isEmpty(posting.getProducts())) {
				productPrice = posting.getProducts().get(0).getPrice();
				productCurrencyCode = posting.getProducts().get(0).getCurrencyCode();
			}

			// 获取客户支付价格（从 financial_data 获取）
			String customerPrice = null;
			String customerPriceCurrencyCode = null;
			if (posting.getFinancialData() != null
					&& posting.getFinancialData().getProducts() != null
					&& !posting.getFinancialData().getProducts().isEmpty()) {

				customerPrice = posting.getFinancialData().getProducts().get(0).getCustomerPrice();
				customerPriceCurrencyCode = posting.getFinancialData().getProducts().get(0).getCurrencyCode();
			}

			return OzonPostingSyncDTO.builder()
					.postingNumber(posting.getPostingNumber())
					.orderNumber(posting.getOrderNumber())
					.status(posting.getStatus())
					.substatus(posting.getSubstatus())
					.fulfillmentType(fulfillmentType)
					.products(posting.getProducts())
					.warehouseId(warehouseId)
					.warehouseName(warehouseName)
					.destinationWarehouseId(warehouseId) // FBS: 仓库即发货点
					.deliveryMethod(deliveryMethod)
					.inProcessAt(posting.getInProcessAt())
					.shipmentDate(posting.getShipmentDate())
					.deliveringDate(posting.getDeliveringDate())
					.currencyCode(determineCurrencyCode(posting))
					.productPrice(productPrice)
					.productCurrencyCode(productCurrencyCode)
					.customerPrice(customerPrice)
					.customerPriceCurrencyCode(customerPriceCurrencyCode)
					.financialDataJson(financialDataJson)
					.rawJson(rawJson)
					.build();
		} catch (Exception e) {
			log.error("[OZON] 转换订单 DTO 失败: postingNumber={}, error={}",
					posting.getPostingNumber(), e.getMessage());
			throw new RuntimeException("转换订单 DTO 失败", e);
		}
	}

	/**
	 * 转换 FBO 订单为 DTO
	 *
	 * @param posting         FBO 订单
	 * @param fulfillmentType 履约类型
	 * @return 订单 DTO
	 */
	private OzonPostingSyncDTO convertFboToDTO(OzonFboPosting posting, String fulfillmentType) {
		try {
			// 序列化财务数据
			String financialDataJson = posting.getFinancialData() != null
					? objectMapper.writeValueAsString(posting.getFinancialData())
					: null;

			// 序列化原始 JSON
			String rawJson = objectMapper.writeValueAsString(posting);

			// 获取仓库ID (FBO: 从 analytics_data 中获取)
			Long warehouseId = (posting.getAnalyticsData() != null && posting.getAnalyticsData().getWarehouseId() != null)
					? posting.getAnalyticsData().getWarehouseId()
					: null;

			// 获取商品价格和币种
			String productPrice = null;
			String productCurrencyCode = null;
			if (!CollectionUtils.isEmpty(posting.getProducts())) {
				productPrice = posting.getProducts().get(0).getPrice();
				productCurrencyCode = posting.getProducts().get(0).getCurrencyCode();
			}

			// 获取客户支付价格（从 financial_data 获取）
			// FBO订单特殊处理: customer_price通常为null,使用financial_data.products[0].price
			String customerPrice = null;
			String customerPriceCurrencyCode = null;
			if (posting.getFinancialData() != null
					&& posting.getFinancialData().getProducts() != null
					&& !posting.getFinancialData().getProducts().isEmpty()) {

				customerPrice = posting.getFinancialData().getProducts().get(0).getCustomerPrice();

				// FBO订单: 如果customer_price为null,使用financial_data中的price
				if (customerPrice == null || customerPrice.isEmpty()) {
					customerPrice = posting.getFinancialData().getProducts().get(0).getPrice();
					log.debug("[OZON] FBO订单customer_price为null,使用financial_data.price: postingNumber={}, price={}",
							posting.getPostingNumber(), customerPrice);
				}

				customerPriceCurrencyCode = posting.getFinancialData().getProducts().get(0).getCurrencyCode();
			}

			return OzonPostingSyncDTO.builder()
					.postingNumber(posting.getPostingNumber())
					.orderNumber(posting.getOrderNumber())
					.status(posting.getStatus())
					.substatus(posting.getSubstatus())
					.fulfillmentType(fulfillmentType)
					.products(posting.getProducts())
					.warehouseId(warehouseId) // FBO: 从 analytics_data 获取 Ozon 履约仓库ID
					.destinationWarehouseId(warehouseId) // FBO: 仓库即发货点
					.deliveryMethod(null) // FBO 订单没有配送方式
					.inProcessAt(posting.getInProcessAt())
					.shipmentDate(posting.getShipmentDate())
					.deliveringDate(posting.getDeliveringDate())
					.currencyCode(determineFboCurrencyCode(posting))
					.productPrice(productPrice)
					.productCurrencyCode(productCurrencyCode)
					.customerPrice(customerPrice)
					.customerPriceCurrencyCode(customerPriceCurrencyCode)
					.financialDataJson(financialDataJson)
					.rawJson(rawJson)
					.build();
		} catch (Exception e) {
			log.error("[OZON] 转换 FBO 订单 DTO 失败: postingNumber={}, error={}",
					posting.getPostingNumber(), e.getMessage());
			throw new RuntimeException("转换 FBO 订单 DTO 失败", e);
		}
	}

	/**
	 * 确定币种代码（从商品或财务数据中获取）
	 *
	 * @param posting 订单
	 * @return 币种代码
	 */
	private String determineCurrencyCode(OzonPosting posting) {
		// 优先从商品中获取
		if (!CollectionUtils.isEmpty(posting.getProducts()) && posting.getProducts().get(0).getCurrencyCode() != null) {
			return posting.getProducts().get(0).getCurrencyCode();
		}

		// 从财务数据中获取
		if (posting.getFinancialData() != null && posting.getFinancialData().getProducts() != null
				&& !posting.getFinancialData().getProducts().isEmpty()) {
			return posting.getFinancialData().getProducts().get(0).getCurrencyCode();
		}

		return "RUB"; // 默认卢布
	}

	/**
	 * 确定 FBO 订单的币种代码
	 *
	 * @param posting FBO 订单
	 * @return 币种代码
	 */
	private String determineFboCurrencyCode(OzonFboPosting posting) {
		// 优先从商品中获取
		if (!CollectionUtils.isEmpty(posting.getProducts()) && posting.getProducts().get(0).getCurrencyCode() != null) {
			return posting.getProducts().get(0).getCurrencyCode();
		}

		// 从财务数据中获取
		if (posting.getFinancialData() != null && posting.getFinancialData().getProducts() != null
				&& !posting.getFinancialData().getProducts().isEmpty()) {
			return posting.getFinancialData().getProducts().get(0).getCurrencyCode();
		}

		return "RUB"; // 默认卢布
	}

	/**
	 * 格式化时间为 ISO 8601 格式
	 *
	 * @param dateTime 本地时间
	 * @return ISO 8601 格式字符串
	 */
	private String formatDateTime(LocalDateTime dateTime) {
		OffsetDateTime offsetDateTime = dateTime.atOffset(ZoneOffset.UTC);
		return offsetDateTime.format(ISO_FORMATTER);
	}

	// ==================== 按订单ID同步 ====================

	/**
	 * 按订单ID列表同步状态
	 */
	public SyncSummaryVO syncOrdersByIds(List<Long> orderIds) {
		SyncSummaryVO summary = new SyncSummaryVO();
		summary.setTotal(orderIds != null ? orderIds.size() : 0);
		summary.setSuccess(0);
		summary.setFailed(0);

		if (orderIds == null || orderIds.isEmpty()) {
			return summary;
		}

		List<ErpOrder> orders = erpOrderMapper.selectByIds(orderIds);
		if (orders == null || orders.isEmpty()) {
			return summary;
		}

		Map<Long, List<ErpOrder>> groupByShop = orders.stream()
				.collect(Collectors.groupingBy(ErpOrder::getShopId));

		for (Map.Entry<Long, List<ErpOrder>> entry : groupByShop.entrySet()) {
			syncShopOrdersByIds(entry.getKey(), entry.getValue(), summary);
		}

		return summary;
	}

	/**
	 * 按店铺同步指定订单的状态
	 */
	private void syncShopOrdersByIds(Long shopId, List<ErpOrder> orders, SyncSummaryVO summary) {
		Shop shop = shopService.getById(shopId);
		if (shop == null) {
			summary.setFailed(summary.getFailed() + orders.size());
			return;
		}

		OzonCredential credential;
		try {
			credential = credentialService.parseCredential(shop);
		} catch (Exception ex) {
			summary.setFailed(summary.getFailed() + orders.size());
			return;
		}

		List<String> postingNumbers = orders.stream()
				.map(ErpOrder::getShipmentId)
				.filter(s -> s != null && !s.isEmpty())
				.collect(Collectors.toList());

		if (postingNumbers.isEmpty()) {
			return;
		}

		Map<String, OzonPosting> postingMap = ozonPlatformApi.batchFetchPostings(credential, postingNumbers);

		for (ErpOrder order : orders) {
			try {
				OzonPosting posting = postingMap.get(order.getShipmentId());
				if (posting != null) {
					String oldErpStatus = order.getErpStatus();
					ErpOrderStatusEnum newErpStatus = OzonOrderStatusConverter.toErpStatus(
							posting.getStatus(), posting.getSubstatus());

					ErpOrder updateOrder = new ErpOrder();
					updateOrder.setId(order.getId());
					// null=不可映射：只更新平台状态镜像，保持 ERP 状态不变
					if (newErpStatus != null) {
						updateOrder.setErpStatus(newErpStatus.name());
					}
					updateOrder.setPlatformStatus(posting.getStatus());
					updateOrder.setPlatformSubstatus(posting.getSubstatus());
					updateOrder.setUpdateTime(LocalDateTime.now());
					erpOrderMapper.updateById(updateOrder);

					// 触发状态变更副作用（仅在状态真实变化时）
					if (newErpStatus != null && !newErpStatus.name().equals(oldErpStatus)) {
						lifecycleService.onStatusChanged(order, oldErpStatus, newErpStatus.name());
					}

					summary.setSuccess(summary.getSuccess() + 1);
				} else {
					summary.setFailed(summary.getFailed() + 1);
				}
			} catch (Exception ex) {
				summary.setFailed(summary.getFailed() + 1);
				log.error("[OZON][SYNC] 订单状态同步失败 orderId={} error={}",
						order.getId(), ex.getMessage(), ex);
			}
		}
	}
}
