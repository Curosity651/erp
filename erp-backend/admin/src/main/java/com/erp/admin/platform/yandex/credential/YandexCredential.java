package com.erp.admin.platform.yandex.credential;

import org.springframework.util.StringUtils;

/**
 * Yandex Market API credential holder (immutable).
 * <p>
 * 凭证格式：{"api_key": "xxx", "business_id": 123, "campaign_id": 456}
 */
public final class YandexCredential {

	private final String apiKey;
	private final long businessId;
	private final long campaignId;

	public YandexCredential(String apiKey, long businessId, long campaignId) {
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException("apiKey不能为空");
		}
		if (businessId <= 0) {
			throw new IllegalArgumentException("businessId必须大于0");
		}
		if (campaignId <= 0) {
			throw new IllegalArgumentException("campaignId必须大于0");
		}
		this.apiKey = apiKey;
		this.businessId = businessId;
		this.campaignId = campaignId;
	}

	public String getApiKey() {
		return apiKey;
	}

	public long getBusinessId() {
		return businessId;
	}

	public long getCampaignId() {
		return campaignId;
	}

	@Override
	public String toString() {
		return "YandexCredential{" +
				"apiKey='" + maskSensitive(apiKey) + '\'' +
				", businessId=" + businessId +
				", campaignId=" + campaignId +
				'}';
	}

	private String maskSensitive(String value) {
		if (value == null || value.length() <= 8) {
			return "****";
		}
		return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
	}
}
