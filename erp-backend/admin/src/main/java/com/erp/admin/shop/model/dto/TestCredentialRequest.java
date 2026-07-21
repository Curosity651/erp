package com.erp.admin.shop.model.dto;

import java.util.Map;

import lombok.Data;

// ====== 请求/响应模型（后端内部使用，与前端类型匹配） ======
@Data
public class TestCredentialRequest {

	private String platform;

	private Map<String, String> credential;

}
