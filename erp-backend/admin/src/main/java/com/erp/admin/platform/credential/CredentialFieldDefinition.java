package com.erp.admin.platform.credential;

import lombok.Data;

/**
 * 凭证字段定义（驱动前端动态表单）
 */
@Data
public class CredentialFieldDefinition {

    /** 字段 key，如 "api_key", "client_id" */
    private String key;

    /** 显示标签，如 "API Key", "Client ID" */
    private String label;

    /** 字段类型: "string", "number" */
    private String type;

    /** 是否必填 */
    private boolean required;

    /** 是否敏感（前端做密码输入 + 掩码展示） */
    private boolean sensitive;
}