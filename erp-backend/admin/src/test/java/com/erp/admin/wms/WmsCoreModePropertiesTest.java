package com.erp.admin.wms;

import com.erp.admin.wms.config.WmsCoreModeProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WmsCoreModePropertiesTest {

    @Test
    void defaults_to_legacy_mode() {
        WmsCoreModeProperties properties = new WmsCoreModeProperties();

        assertThat(properties.getCoreMode()).isEqualTo(WmsCoreModeProperties.Mode.LEGACY);
    }

}
