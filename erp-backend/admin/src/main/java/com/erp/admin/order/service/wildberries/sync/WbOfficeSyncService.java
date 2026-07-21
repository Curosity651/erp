package com.erp.admin.order.service.wildberries.sync;

import java.util.List;

import com.erp.admin.order.mapper.WbOfficeMapper;
import com.erp.admin.order.model.entity.WbOffice;
import com.erp.admin.order.service.wildberries.converter.WbOfficeConverter;
import com.erp.admin.platform.PlatformApiException;
import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialService;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.WildberriesClient;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * WbOfficeSyncService - Wildberries Office（仓库）同步服务
 * <p>
 * 职责：
 * - Office 列表同步
 * - Office 数据 Upsert
 * <p>
 * 从 WbSyncService 提取，专注于 Office 同步逻辑
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbOfficeSyncService {

	private final WildberriesClient wbClient;
	private final ShopService shopService;
	private final WbOfficeMapper wbOfficeMapper;
	private final CredentialService credentialService;

	// ==================== Office 列表同步 ====================

	/**
	 * 同步所有启用店铺的 Office
	 */
	public void syncAllEnabledShopsOffices() {
		List<Shop> shops = shopService.listEnabledByPlatform(PlatformEnum.Wildberries.code());
		if (CollectionUtils.isEmpty(shops)) {
			log.debug("[WB][OFFICE_SYNC] 没有启用的店铺");
			return;
		}

		for (Shop shop : shops) {
			try {
				TenantContext.runAs(shop.getTenantId(), () -> {
					syncShopOffices(shop);
					return null;
				});
			} catch (Exception e) {
				log.error("[WB][OFFICE_SYNC] 店铺 {} 同步失败: {}", shop.getId(), e.getMessage(), e);
			}
		}
	}

	/**
	 * 同步单个店铺的 Office
	 *
	 * @param shop 店铺对象
	 */
	public void syncShopOffices(Shop shop) {
		// 1. 解析凭证
		WbCredential credential = credentialService.parseCredential(shop);

		// 2. 直接调用 Client 获取仓库列表
		try {
			List<com.erp.admin.platform.wildberries.model.response.office.WbOffice> offices =
					wbClient.getOfficesTyped(credential);

			if (CollectionUtils.isEmpty(offices)) {
				log.debug("[WB][OFFICE_SYNC] 店铺 {} 没有仓库数据", shop.getId());
				return;
			}

			// 3. 逐个 Upsert（使用 Converter 转换）
			int count = 0;
			for (com.erp.admin.platform.wildberries.model.response.office.WbOffice office : offices) {
				if (office.getId() == null) {
					continue;
				}

				// 使用 Converter 静态方法转换为实体
				WbOffice entity = WbOfficeConverter.toEntity(office, shop.getId());
				if (entity != null) {
					upsertOfficeEntity(entity);
					count++;
				}
			}

			log.info("[WB][OFFICE_SYNC] 店铺 {} 同步了 {} 个仓库", shop.getId(), count);
		} catch (PlatformApiException e) {
			// 认证错误：记录并跳过
			if ("UNAUTHORIZED".equals(e.getErrorCode())) {
				log.error("[WB][OFFICE_SYNC] 店铺 {} 认证失败", shop.getId());
				return;
			}
			// 其他平台错误：HttpExecutor 已记录详细日志
			log.warn("[WB][OFFICE_SYNC] 店铺 {} 同步失败: code={}", shop.getId(), e.getErrorCode());
		} catch (Exception e) {
			log.error("[WB][OFFICE_SYNC] 店铺 {} 同步异常: {}", shop.getId(), e.getMessage(), e);
		}
	}

	/**
	 * Upsert 仓库信息（使用实体）
	 *
	 * @param entity 仓库实体
	 */
	private void upsertOfficeEntity(WbOffice entity) {
		if (entity == null || entity.getOfficeId() == null) {
			return;
		}

		WbOffice existing = wbOfficeMapper.selectByShopIdAndOfficeId(entity.getShopId(), entity.getOfficeId());

		if (existing == null) {
			wbOfficeMapper.insert(entity);
		} else {
			entity.setId(existing.getId());
			wbOfficeMapper.updateById(entity);
		}
	}

}
