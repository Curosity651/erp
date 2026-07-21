package com.erp.admin.platform.ozon.credential;

import org.springframework.util.StringUtils;

/**
 * Ozon API credential holder (immutable).
 * 
 * @author system
 */
public final class OzonCredential {

    private final String clientId;
    private final String apiKey;

    public OzonCredential(String clientId, String apiKey) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("clientId/apiKey不能为空");
        }
        this.clientId = clientId;
        this.apiKey = apiKey;
    }

    public String getClientId() {
        return clientId;
    }

    public String getApiKey() {
        return apiKey;
    }
    
    @Override
    public String toString() {
        return "OzonCredential{" +
                "clientId='" + maskSensitive(clientId) + '\'' +
                ", apiKey='" + maskSensitive(apiKey) + '\'' +
                '}';
    }
    
    private String maskSensitive(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
