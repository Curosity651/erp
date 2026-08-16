package com.erp.admin.wms;

import com.erp.admin.wms.config.WmsCoreModeGuard;
import com.erp.admin.wms.config.WmsCoreModeProperties;
import org.ballcat.common.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WmsCoreModeGuardTest {

	@Test
	void legacy_mode_allows_legacy_writes() {
		WmsCoreModeProperties properties = new WmsCoreModeProperties();
		properties.setCoreMode(WmsCoreModeProperties.Mode.LEGACY);

		assertThatCode(() -> new WmsCoreModeGuard(properties).assertLegacyWriteAllowed("托盘容量修改"))
			.doesNotThrowAnyException();
	}

	@Test
	void logical_location_mode_rejects_legacy_writes_with_clear_message() {
		WmsCoreModeProperties properties = new WmsCoreModeProperties();
		properties.setCoreMode(WmsCoreModeProperties.Mode.LOGICAL_LOCATION);

		assertThatThrownBy(() -> new WmsCoreModeGuard(properties).assertLegacyWriteAllowed("托盘容量修改"))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("托盘容量修改")
			.hasMessageContaining("库位库存模式");
	}
}
