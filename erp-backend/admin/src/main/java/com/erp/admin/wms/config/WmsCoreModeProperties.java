package com.erp.admin.wms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "erp.wms")
public class WmsCoreModeProperties {

    public enum Mode {
        LEGACY,
        LOGICAL_LOCATION
    }

    private Mode coreMode = Mode.LEGACY;

}
