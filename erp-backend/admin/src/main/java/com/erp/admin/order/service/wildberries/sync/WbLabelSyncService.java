package com.erp.admin.order.service.wildberries.sync;

import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.service.wildberries.WbPlatformApi;
import com.erp.admin.order.util.LabelUtils;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.util.SpringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
	public void syncOrderLabel(ErpOrder order, String cachedLabelBase64, WbCredential credential) {
		if (StringUtils.hasText(order.getLabelBase64())) {
			return; // 已有面单，跳过
		}

		// 1. HTTP 调用（事务外）
		String b64 = cachedLabelBase64;
		if (!StringUtils.hasText(b64)) {
			Long wbOrderId = LabelUtils.toLong(order.getPlatformOrderId());
			if (wbOrderId != null) {
				b64 = wbPlatformApi.fetchOrderLabel(credential, wbOrderId);
			}
		}

		if (!StringUtils.hasText(b64)) {
			throw new IllegalStateException("WB 未返回订单面单: orderId=" + order.getId());
		}

		// 2. 数据库更新（独立事务，通过代理调用）
		SpringUtils.getBean(WbLabelSyncService.class).doUpdateOrderLabel(order.getId(), b64);
		order.setLabelBase64(b64); // 同步更新内存对象
		log.info("[WB][LABEL] 订单面单更新成功: orderId={}", order.getId());
	}

	/**
	 * 更新订单面单（独立事务）
	 */
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void doUpdateOrderLabel(Long orderId, String labelBase64) {
		ErpOrder update = new ErpOrder();
		update.setId(orderId);
		update.setLabelBase64(labelBase64);
		erpOrderMapper.updateById(update);
	}

}
