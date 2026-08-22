package com.erp.admin.order.service.label.strategy;

import com.erp.admin.platform.wildberries.WildberriesClient;
import com.erp.admin.platform.wildberries.credential.WbCredential;
import com.erp.admin.platform.wildberries.model.request.sticker.WbGetStickersRequest;
import com.erp.admin.platform.wildberries.model.response.sticker.WbSticker;
import com.erp.admin.platform.wildberries.model.response.sticker.WbStickersResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches WB stickers in API-compliant batches.
 */
@Slf4j
public class WbStickerBatchFetcher {

	private static final int MAX_BATCH_SIZE = 100;

	private final WildberriesClient client;

	public WbStickerBatchFetcher(WildberriesClient client) {
		this.client = client;
	}

	public Map<Long, WbSticker> fetch(WbCredential credential, List<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Long, WbSticker> result = new LinkedHashMap<>();
		for (int from = 0; from < orderIds.size(); from += MAX_BATCH_SIZE) {
			int to = Math.min(from + MAX_BATCH_SIZE, orderIds.size());
			WbGetStickersRequest request = WbGetStickersRequest.builder()
					.orders(orderIds.subList(from, to))
					.build();
			try {
				WbStickersResponse response = client.getOrderStickersTyped(credential, request);
				if (response == null || response.getStickers() == null) {
					continue;
				}
				for (WbSticker sticker : response.getStickers()) {
					if (sticker != null && sticker.getOrderId() != null) {
						result.put(sticker.getOrderId(), sticker);
					}
				}
			}
			catch (Exception ex) {
				log.warn("[WB][LABEL] 面单分批获取失败: from={}, size={}, error={}",
						from, to - from, ex.getMessage());
			}
		}
		return result;
	}
}
