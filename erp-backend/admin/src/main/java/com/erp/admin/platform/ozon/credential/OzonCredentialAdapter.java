package com.erp.admin.platform.ozon.credential;

import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialFieldDefinition;
import com.erp.admin.platform.credential.CredentialMaskUtil;
import com.erp.admin.platform.credential.CredentialTestResult;
import com.erp.admin.platform.credential.PlatformCredentialAdapter;
import com.erp.admin.platform.ozon.OzonClient;
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
 * Ozon 凭证适配器
 * <p>
 * 合并原 OzonCredentialParser 的功能。
 * 凭证格式：{"client_id": "xxx", "api_key": "xxx"}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OzonCredentialAdapter implements PlatformCredentialAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final OzonClient ozonClient;

    @Override
    public String platform() {
        return PlatformEnum.Ozon.code();
    }

    @Override
    public OzonCredential parseCredential(String credentialJson) {
        if (!StringUtils.hasText(credentialJson)) {
            throw new IllegalArgumentException("凭证信息不能为空");
        }
        try {
            JsonNode node = MAPPER.readTree(credentialJson);
            String clientId = extractField(node, "client_id");
            String apiKey = extractField(node, "api_key");
            return new OzonCredential(clientId, apiKey);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("解析 Ozon 凭证失败: {}", e.getMessage());
            throw new IllegalArgumentException("凭证格式错误: " + e.getMessage(), e);
        }
    }

    @Override
    public CredentialTestResult validateAndFetchShopInfo(Map<String, String> credential) {
        ozonClient.validateCredential(credential);
        CredentialTestResult result = new CredentialTestResult();
        result.setPlatformShopId(credential.get("client_id"));
        result.setShopName(null); // Ozon API 不返回店铺名称
        result.setShopNameRequired(true);
        return result;
    }

    @Override
    public CredentialTestResult revalidate(String credentialJson) {
        OzonCredential cred = parseCredential(credentialJson);
        Map<String, String> credMap = new HashMap<>();
        credMap.put("client_id", cred.getClientId());
        credMap.put("api_key", cred.getApiKey());
        return validateAndFetchShopInfo(credMap);
    }

    @Override
    public List<CredentialFieldDefinition> getCredentialFields() {
        CredentialFieldDefinition clientId = new CredentialFieldDefinition();
        clientId.setKey("client_id");
        clientId.setLabel("Client ID");
        clientId.setType("string");
        clientId.setRequired(true);
        clientId.setSensitive(false);

        CredentialFieldDefinition apiKey = new CredentialFieldDefinition();
        apiKey.setKey("api_key");
        apiKey.setLabel("API Key");
        apiKey.setType("string");
        apiKey.setRequired(true);
        apiKey.setSensitive(true);

        return Arrays.asList(clientId, apiKey);
    }

    @Override
    public Map<String, String> maskCredential(Map<String, String> credential) {
        return CredentialMaskUtil.mask(credential);
    }

    private String extractField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull() || !StringUtils.hasText(fieldNode.asText())) {
            throw new IllegalArgumentException("缺失必要字段: " + fieldName);
        }
        return fieldNode.asText().trim();
    }
}