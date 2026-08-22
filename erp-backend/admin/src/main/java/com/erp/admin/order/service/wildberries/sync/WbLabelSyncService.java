package com.erp.admin.order.service.wildberries.sync;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.service.wildberries.WbPlatformApi;
import com.erp.admin.order.util.LabelUtils;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.model.response.sticker.WbSticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.util.JsonUtils;
import org.ballcat.common.util.SpringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * WbLabelSyncService - Wildberries 订单面单同步服务
 * <p>
 * 负责同步订单面单（label / sticker），从 WbOrderSyncService 拆分而来。
 * <p>
 * 设计要点：
 * - HTTP 调用在事务外执行
 * - 数据库更新在独立事务内完成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WbLabelSyncService {

	private final WbPlatformApi wbPlatformApi;
	private final ErpOrderMapper erpOrderMapper;

	/**
	 * 同步订单面单
	 * <p>
	 * HTTP 调用在事务外执行，只有数据库更新在独立事务内
	 *
	 * @param order              订单实体
	 * @param cachedLabelBase64  批量获取的面单缓存（可选）
	 * @param credential         WB 凭证
	 */
	public void syncOrderLabel(ErpOrder order, WbSticker cachedSticker, WbCredential credential) {
		if (StringUtils.hasText(order.getLabelBase64())
				&& StringUtils.hasText(order.getLabelVerifyCodes())) {
			return; // 已有面单，跳过
		}

		// 1. HTTP 调用（事务外）
		String b64 = cachedSticker == null ? null : cachedSticker.getFile();
		if (!StringUtils.hasText(b64)) {
			Long wbOrderId = LabelUtils.toLong(order.getPlatformOrderId());
			if (wbOrderId != null) {
				b64 = wbPlatformApi.fetchOrderLabel(credential, wbOrderId);
			}
		}
		if (!StringUtils.hasText(b64)) {
			b64 = order.getLabelBase64();
		}

		if (!StringUtils.hasText(b64)) {
			throw new IllegalStateException("WB 未返回订单面单: orderId=" + order.getId());
		}

		// 2. 数据库更新（独立事务，通过代理调用）
		List<String> verifyCodes = stickerCodes(cachedSticker);
		SpringUtils.getBean(WbLabelSyncService.class)
				.doUpdateOrderLabel(order.getId(), b64, verifyCodes);
		order.setLabelBase64(b64); // 同步更新内存对象
		if (!verifyCodes.isEmpty()) {
			order.setLabelVerifyCodes(JsonUtils.toJson(verifyCodes));
		}
		log.info("[WB][LABEL] 订单面单更新成功: orderId={}", order.getId());
	}

	/**
	 * 更新订单面单（独立事务）
	 */
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void doUpdateOrderLabel(Long orderId, String labelBase64) {
		doUpdateOrderLabel(orderId, labelBase64, java.util.Collections.emptyList());
	}

	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void doUpdateOrderLabel(Long orderId, String labelBase64, List<String> verifyCodes) {
		ErpOrder update = new ErpOrder();
		update.setId(orderId);
		update.setLabelBase64(labelBase64);
		if (verifyCodes != null && !verifyCodes.isEmpty()) {
			update.setLabelVerifyCodes(JsonUtils.toJson(verifyCodes));
		}
		erpOrderMapper.updateById(update);
	}

	private List<String> stickerCodes(WbSticker sticker) {
		List<String> result = new ArrayList<>();
		if (sticker == null) {
			return result;
		}
		if (StringUtils.hasText(sticker.getBarcode())) {
			result.add(sticker.getBarcode().trim());
		}
		if (StringUtils.hasText(sticker.getPartA()) && StringUtils.hasText(sticker.getPartB())) {
			result.add(sticker.getPartA().trim() + sticker.getPartB().trim());
		}
		return result;
	}

}
