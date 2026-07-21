package com.erp.admin.platform.yandex.credential;

import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialFieldDefinition;
import com.erp.admin.platform.credential.CredentialMaskUtil;
import com.erp.admin.platform.credential.CredentialTestResult;
import com.erp.admin.platform.credential.PlatformCredentialAdapter;
import com.erp.admin.platform.yandex.YandexClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Yandex Market 凭证适配器
 * <p>
 * 合并原 YandexCredentialParser 的功能。
 * 凭证格式：{"api_key": "xxx", "business_id": 123, "campaign_id": 456}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YandexCredentialAdapter implements PlatformCredentialAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final YandexClient yandexClient;

    @Override
    public String platform() {
        return PlatformEnum.Yandex.code();
    }

    @Override
    public YandexCredential parseCredential(String credentialJson) {
        if (!StringUtils.hasText(credentialJson)) {
            throw new IllegalArgumentException("凭证信息不能为空");
        }
        try {
            JsonNode node = MAPPER.readTree(credentialJson);
            String apiKey = extractStringField(node, "api_key");
            long businessId = extractLongField(node, "business_id");
            long campaignId = extractLongField(node, "campaign_id");
            return new YandexCredential(apiKey, businessId, campaignId);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("解析 Yandex 凭证失败: {}", e.getMessage());
            throw new IllegalArgumentException("凭证格式错误: " + e.getMessage(), e);
        }
    }

    @Override
    public CredentialTestResult validateAndFetchShopInfo(Map<String, String> credential) {
        String apiKey = credential.get("api_key");
        String businessIdStr = credential.get("business_id");
        String campaignIdStr = credential.get("campaign_id");

        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("api_key 不能为空");
        }
        if (!StringUtils.hasText(businessIdStr) || !StringUtils.hasText(campaignIdStr)) {
            throw new IllegalArgumentException("business_id 和 campaign_id 不能为空");
        }

        long businessId;
        long campaignId;
        try {
            businessId = Long.parseLong(businessIdStr);
            campaignId = Long.parseLong(campaignIdStr);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("business_id / campaign_id 必须是数字");
        }

        // 通过 getCampaigns 间接验证凭证有效性
        YandexCredential cred = new YandexCredential(apiKey, businessId, campaignId);
        yandexClient.getCampaigns(cred);

        CredentialTestResult result = new CredentialTestResult();
        result.setPlatformShopId(String.valueOf(campaignId));
        result.setShopName(null); // Yandex API 不直接返回店铺名称
        result.setShopNameRequired(true);
        return result;
    }

    @Override
    public CredentialTestResult revalidate(String credentialJson) {
        YandexCredential cred = parseCredential(credentialJson);
        Map<String, String> credMap = new HashMap<>();
        credMap.put("api_key", cred.getApiKey());
        credMap.put("business_id", String.valueOf(cred.getBusinessId()));
        credMap.put("campaign_id", String.valueOf(cred.getCampaignId()));
        return validateAndFetchShopInfo(credMap);
    }

    @Override
    public List<CredentialFieldDefinition> getCredentialFields() {
        CredentialFieldDefinition apiKey = new CredentialFieldDefinition();
        apiKey.setKey("api_key");
        apiKey.setLabel("API Key");
        apiKey.setType("string");
        apiKey.setRequired(true);
        apiKey.setSensitive(true);

        CredentialFieldDefinition businessId = new CredentialFieldDefinition();
        businessId.setKey("business_id");
        businessId.setLabel("Business ID");
        businessId.setType("number");
        businessId.setRequired(true);
        businessId.setSensitive(false);

        CredentialFieldDefinition campaignId = new CredentialFieldDefinition();
        campaignId.setKey("campaign_id");
        campaignId.setLabel("Campaign ID");
        campaignId.setType("number");
        campaignId.setRequired(true);
        campaignId.setSensitive(false);

        return Arrays.asList(apiKey, businessId, campaignId);
    }

    @Override
    public Map<String, String> maskCredential(Map<String, String> credential) {
        return CredentialMaskUtil.mask(credential);
    }

    private String extractStringField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !StringUtils.hasText(fieldNode.asText())) {
            throw new IllegalArgumentException("缺失必要字段: " + fieldName);
        }
        return fieldNode.asText().trim();
    }

    private long extractLongField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            throw new IllegalArgumentException("缺失必要字段: " + fieldName);
        }
        if (fieldNode.isNumber()) {
            return fieldNode.asLong();
        }
        try {
            return Long.parseLong(fieldNode.asText().trim());
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 必须是数字");
        }
    }
}