package com.erp.admin.order.service.wildberries.sync;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.erp.admin.order.model.dto.wildberries.WbSupplySyncDTO;
import com.erp.admin.order.model.entity.WbSupply;
import com.erp.admin.order.service.wildberries.WbSupplyService;
import com.erp.admin.order.service.wildberries.converter.WbSupplyConverter;
import com.erp.admin.platform.PlatformApiException;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.wildberries.WbSyncConstants;
import com.erp.admin.platform.wildberries.WildberriesClient;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.model.common.WbPaginationQuery;
import com.erp.admin.platform.wildberries.model.response.supply.WbSupplyBarcodeResponse;
import com.erp.admin.platform.wildberries.model.response.supply.WbSupplyDetail;
import com.erp.admin.platform.wildberries.model.response.supply.WbSupplyListResponse;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.util.SpringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * WbSupplySyncService - Wildberries Supply 同步服务
 * <p>
 * 职责：
 * - Supply 列表同步
 * - Supply 基础数据同步
 * - Supply 面单同步
 * <p>
 * 从 WbSyncService 提取，专注于 Supply 同步逻辑
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbSupplySyncService {

	private final WildberriesClient wbClient;
	private final ShopService shopService;
	private final WbSupplyService wbSupplyService;
	private final CredentialService credentialService;

	// ==================== Supply 列表同步 ====================

	/**
	 * 同步所有启用店铺的 Supply
	 */
	public void syncAllEnabledShopsSupplies() {
		List<Shop> shops = shopService.listEnabledByPlatform(PlatformEnum.Wildberries.code());
		if (CollectionUtils.isEmpty(shops)) {
			log.debug("[WB][SUPPLY_SYNC] 没有启用的店铺");
			return;
		}

		for (Shop shop : shops) {
			try {
				TenantContext.runAs(shop.getTenantId(), () -> {
					syncShopSupplies(shop);
					return null;
				});
			} catch (Exception e) {
				log.error("[WB][SUPPLY_SYNC] 店铺 {} 同步失败: {}", shop.getId(), e.getMessage(), e);
			}
		}
	}

	/**
	 * 同步单个店铺的 Supply
	 *
	 * @param shop 店铺对象
	 */
	public void syncShopSupplies(Shop shop) {
		WbCredential credential = credentialService.parseCredential(shop);

		// 分页拉取 Supply 列表
		long next = 0L;
		int page = 0;
		int pageSize = WbSyncConstants.DEFAULT_ORDER_PAGE_SIZE;
		int maxPages = WbSyncConstants.MAX_PAGE_ITERATIONS;
		Set<String> synced = new HashSet<>();

		while (page < maxPages) {
			try {
				WbPaginationQuery query = WbPaginationQuery.builder()
						.limit(pageSize)
						.next(next)
						.build();

				// 直接调用 Client 获取 Supply 列表
				WbSupplyListResponse response = wbClient.getSuppliesTyped(credential, query);

				if (response == null || CollectionUtils.isEmpty(response.getSupplies())) {
					break;
				}

				// 逐个 Upsert
				for (WbSupplyDetail supply : response.getSupplies()) {
					if (supply.getId() == null || synced.contains(supply.getId())) {
						continue;
					}

					synced.add(supply.getId());

					// 使用 Converter 静态方法转换
					WbSupplySyncDTO dto = WbSupplyConverter.toDTO(supply);
					if (dto != null) {
						wbSupplyService.upsertSupply(shop.getId(), dto);
					}
				}

				// 检查下一页
				if (response.getNext() == null) {
					break;
				}

				long newNext = response.getNext();
				if (newNext == next) {
					break;
				}

				next = newNext;
				page++;
			} catch (PlatformApiException e) {
				// 认证错误：记录并跳出
				if ("UNAUTHORIZED".equals(e.getErrorCode())) {
					log.error("[WB][SUPPLY_SYNC] 店铺 {} 认证失败，停止同步", shop.getId());
					break;
				}
				// 其他平台错误：HttpExecutor 已记录详细日志
				log.warn("[WB][SUPPLY_SYNC] 店铺 {} 分页拉取失败: code={}", shop.getId(), e.getErrorCode());
				break;
			} catch (Exception e) {
				log.error("[WB][SUPPLY_SYNC] 店铺 {} 分页拉取异常: {}", shop.getId(), e.getMessage(), e);
				break;
			}
		}

		log.info("[WB][SUPPLY_SYNC] 店铺 {} 同步了 {} 个批次", shop.getId(), synced.size());
	}

	// ==================== Supply 基础数据同步 ====================

	/**
	 * 同步并插入 Supply 基础数据
	 * <p>
	 * HTTP 调用在事务外执行，只有数据库插入在独立事务内
	 *
	 * @param shopId     店铺ID
	 * @param supplyId   Supply ID
	 * @param credential WB 凭证
	 * @return Supply 实体（不含面单）
	 */
	public WbSupply syncSupplyBasicData(Long shopId, String supplyId, WbCredential credential) {
		// 1. HTTP 调用（事务外）
		WbSupplyDetail wbSupplyDetail = wbClient.getSupplyTyped(credential, supplyId);
		WbSupplySyncDTO dto = WbSupplyConverter.toDTO(wbSupplyDetail);
		log.info("[WB][SUPPLY] 同步 supply 基础信息: supplyId={}", supplyId);

		// 2. 数据库插入（独立事务，通过代理调用）
		return SpringUtils.getBean(WbSupplySyncService.class).doInsertSupply(shopId, dto);
	}

	/**
	 * 插入 Supply 基础数据（独立事务）
	 */
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public WbSupply doInsertSupply(Long shopId, WbSupplySyncDTO dto) {
		return wbSupplyService.insertSupplyKnownAbsent(shopId, dto);
	}

	// ==================== Supply 面单同步 ====================

	/**
	 * 同步 Supply 面单
	 * <p>
	 * HTTP 调用在事务外执行，只有数据库更新在独立事务内
	 *
	 * @param supply     Supply 实体
	 * @param credential WB 凭证
	 */
	public void syncSupplyLabel(WbSupply supply, WbCredential credential) {
		if (StringUtils.hasText(supply.getLabelBase64())) {
			return; // 已有面单，跳过
		}

		// 1. HTTP 调用（事务外）- 直接调用 Client，让异常向上传播
		WbSupplyBarcodeResponse response = wbClient.getSupplyBarcodeTyped(credential, supply.getSupplyId());
		String b64 = response != null ? response.getFile() : null;

		if (!StringUtils.hasText(b64)) {
			throw new IllegalStateException("WB 未返回批次面单: supplyId=" + supply.getSupplyId());
		}

		// 2. 数据库更新（独立事务，通过代理调用）
		SpringUtils.getBean(WbSupplySyncService.class).doUpdateSupplyLabel(supply.getId(), b64);
		supply.setLabelBase64(b64); // 同步更新内存对象
		log.info("[WB][LABEL] Supply 面单更新成功: supplyId={}", supply.getSupplyId());
	}

	/**
	 * 更新 Supply 面单（独立事务）
	 */
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void doUpdateSupplyLabel(Long supplyId, String labelBase64) {
		wbSupplyService.updateLabelBase64(supplyId, labelBase64);
	}

}
