package com.erp.admin.platform.yandex.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Yandex Market API 分页信息
 * <p>
 * 用于 page_token 分页模式
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexPaging {

	/** 下一页的 token，为 null 表示没有更多数据 */
	private String nextPageToken;

	/** 上一页的 token */
	private String prevPageToken;
}
