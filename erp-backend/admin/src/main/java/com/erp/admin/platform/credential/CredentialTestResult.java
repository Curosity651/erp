package com.erp.admin.platform.credential;

import lombok.Data;

/**
 * 凭证验证结果值对象
 */
@Data
public class CredentialTestResult {

    /** 平台侧店铺 ID（WB=sid, Ozon=client_id, Yandex=campaign_id） */
    private String platformShopId;

    /** 店铺名称（部分平台 API 可返回，不可返回则为 null） */
    private String shopName;

    /** 是否需要用户手动填写店铺名称（Ozon=true, WB=false） */
    private boolean shopNameRequired;
}