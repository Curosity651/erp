package com.erp.admin.platform.credential;

import java.util.List;
import java.util.Map;

/**
 * 平台凭证适配器（策略接口）
 * <p>
 * 每个平台实现此接口，由 CredentialService 统一分发。
 * 新增平台只需实现此接口并加 @Component 注解。
 */
public interface PlatformCredentialAdapter {

    /**
     * 支持的平台 code（与 PlatformEnum.code() 一致）
     */
    String platform();

    /**
     * 从 Shop.credential JSON 解析为平台强类型凭证
     *
     * @param credentialJson 数据库中存储的 JSON 凭证字符串
     * @return 具体平台 Credential 对象（WbCredential / OzonCredential / YandexCredential）
     */
    Object parseCredential(String credentialJson);

    /**
     * 验证凭证有效性 + 获取店铺基础信息
     *
     * @param credential 前端提交的原始凭证 map
     * @return 验证结果（含 shopName, platformShopId）
     */
    CredentialTestResult validateAndFetchShopInfo(Map<String, String> credential);

    /**
     * 重新验证已有店铺凭证（入参是已存储的 JSON）
     *
     * @param credentialJson 已存储的凭证 JSON
     * @return 验证结果
     */
    CredentialTestResult revalidate(String credentialJson);

    /**
     * 定义该平台凭证需要的字段列表（前端动态表单渲染用）
     */
    List<CredentialFieldDefinition> getCredentialFields();

    /**
     * 对凭证 map 做掩码处理（详情展示用）
     */
    Map<String, String> maskCredential(Map<String, String> credential);
}