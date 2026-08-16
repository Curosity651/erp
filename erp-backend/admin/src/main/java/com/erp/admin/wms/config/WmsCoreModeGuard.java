package com.erp.admin.wms.config;

import com.erp.admin.wms.enums.WmsResultCode;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WmsCoreModeGuard {

	private final WmsCoreModeProperties properties;

	public void assertLegacyWriteAllowed(String operationName) {
		if (properties.getCoreMode() == WmsCoreModeProperties.Mode.LOGICAL_LOCATION) {
			throw new BusinessException(WmsResultCode.LEGACY_INVENTORY_WRITE_DISABLED.getCode(),
				operationName + "已停用：当前系统使用库位库存模式");
		}

	}
}
