package com.erp.admin.order.service;

import com.erp.admin.order.mapper.ErpOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 平台确认调用的数据库幂等门闩。
 */
@Service
@RequiredArgsConstructor
public class PlatformConfirmGuard {

	private final ErpOrderMapper orderMapper;

	public boolean tryClaim(Long orderId) {
		return orderId != null && orderMapper.claimPlatformConfirm(orderId) == 1;
	}

	public void success(Long orderId) {
		orderMapper.finishPlatformConfirm(orderId, "SUCCESS");
	}

	public void failure(Long orderId) {
		orderMapper.finishPlatformConfirm(orderId, "FAILED");
	}
}
