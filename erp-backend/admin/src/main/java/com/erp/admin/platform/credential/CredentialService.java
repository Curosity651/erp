package com.erp.admin.platform.credential;

import com.erp.admin.shop.model.entity.Shop;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一凭证服务
 * <p>
 * 替代 WbCredentialService + 各处散落的 Parser 直接调用。
 * 根据平台自动分发到对应的 PlatformCredentialAdapter 实现。
 */
@Service
public class CredentialService {

    private final Map<String, PlatformCredentialAdapter> adapterMap;

    /**
     * Spring 注入所有 PlatformCredentialAdapter 实现，按 platform() 建索引
     */
    public CredentialService(List<PlatformCredentialAdapter> adapters) {
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(
                        PlatformCredentialAdapter::platform,
                        Function.identity()
                ));
    }

    /**
     * 根据平台获取适配器
     */
    public PlatformCredentialAdapter getAdapter(String platform) {
        PlatformCredentialAdapter adapter = adapterMap.get(platform);
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的平台: " + platform);
        }
        return adapter;
    }

    /**
     * 从 Shop 实体解析凭证（泛型返回，调用方自行强转）
     */
    @SuppressWarnings("unchecked")
    public <T> T parseCredential(Shop shop) {
        return (T) getAdapter(shop.getPlatform()).parseCredential(shop.getCredential());
    }

    /**
     * 验证凭证并获取店铺信息
     */
    public CredentialTestResult validate(String platform, Map<String, String> credential) {
        return getAdapter(platform).validateAndFetchShopInfo(credential);
    }

    /**
     * 重新验证已有凭证
     */
    public CredentialTestResult revalidate(Shop shop) {
        return getAdapter(shop.getPlatform()).revalidate(shop.getCredential());
    }

    /**
     * 凭证掩码
     */
    public Map<String, String> mask(String platform, Map<String, String> credential) {
        return getAdapter(platform).maskCredential(credential);
    }

    /**
     * 获取凭证字段定义
     */
    public List<CredentialFieldDefinition> getFields(String platform) {
        return getAdapter(platform).getCredentialFields();
    }
}