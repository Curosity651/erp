package com.erp.admin.platform.wildberries.credential;

import com.erp.admin.platform.PlatformEnum;
import com.erp.admin.platform.credential.CredentialFieldDefinition;
import com.erp.admin.platform.credential.CredentialMaskUtil;
import com.erp.admin.platform.credential.CredentialTestResult;
import com.erp.admin.platform.credential.PlatformCredentialAdapter;
import com.erp.admin.platform.wildberries.WildberriesClient;
import com.erp.admin.platform.wildberries.model.response.seller.WbSellerInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wildberries 凭证适配器
 * <p>
 * 合并原 WbCredentialParser + WbCredentialService 的功能。
 * 凭证格式：{"api_key": "xxx"}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WbCredentialAdapter implements PlatformCredentialAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WildberriesClient wildberriesClient;

    @Override
    public String platform() {
        return PlatformEnum.Wildberries.code();
    }

    @Override
    public WbCredential parseCredential(String credentialJson) {
        if (credentialJson == null || credentialJson.trim().isEmpty()) {
            throw new IllegalArgumentException("凭证信息不能为空");
        }
        try {
            JsonNode node = MAPPER.readTree(credentialJson);
            JsonNode apiKeyNode = node.get("api_key");
            if (apiKeyNode == null || apiKeyNode.isNull()) {
                throw new IllegalArgumentException("缺失 api_key 字段");
            }
            String apiKey = apiKeyNode.asText().trim();
            if (!StringUtils.hasText(apiKey)) {
                throw new IllegalArgumentException("api_key 不能为空");
            }
            return new WbCredential(apiKey);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("解析 WB 凭证失败: {}", e.getMessage());
            throw new IllegalArgumentException("无效的 WB 凭证: " + e.getMessage(), e);
        }
    }

    @Override
    public CredentialTestResult validateAndFetchShopInfo(Map<String, String> credential) {
        WbSellerInfo sellerInfo = wildberriesClient.validateCredentialAndGetShopInfo(credential);
        if (!StringUtils.hasText(sellerInfo.getSid())) {
            throw new IllegalArgumentException("获取店铺信息失败: 缺少 sid 字段");
        }
        CredentialTestResult result = new CredentialTestResult();
        result.setPlatformShopId(sellerInfo.getSid());
        result.setShopName(resolveShopName(sellerInfo));
        result.setShopNameRequired(false);
        return result;
    }

    @Override
    public CredentialTestResult revalidate(String credentialJson) {
        WbCredential cred = parseCredential(credentialJson);
        Map<String, String> credMap = new HashMap<>();
        credMap.put("api_key", cred.getApiKey());
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
        return Collections.singletonList(apiKey);
    }

    @Override
    public Map<String, String> maskCredential(Map<String, String> credential) {
        return CredentialMaskUtil.mask(credential);
    }

    private String resolveShopName(WbSellerInfo info) {
        if (StringUtils.hasText(info.getTradeMark())) {
            return info.getTradeMark();
        }
        if (StringUtils.hasText(info.getName())) {
            return info.getName();
        }
        return "WB-" + info.getSid().substring(0, Math.min(6, info.getSid().length()));
    }
}