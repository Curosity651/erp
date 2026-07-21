package com.erp.admin.shop.model.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Data;

// ====== 新增: 凭证测试缓存结构 ======
@Data
public class TestCacheItem {

	private String platform;

	private Map<String, String> credential; // 原始凭证

	private String shopName;

	private String platformShopId;

	private LocalDateTime testedAt;

}
