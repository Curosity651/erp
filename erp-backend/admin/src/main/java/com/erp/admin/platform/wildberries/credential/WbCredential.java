package com.erp.admin.platform.wildberries.credential;

import org.springframework.util.StringUtils;

/**
 * Wildberries API credential holder (immutable).
 */
public final class WbCredential {

    private final String apiKey;

    public WbCredential(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("apiKey不能为空");
        }
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }
}
