package com.erp.admin.system.model.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * OSS URL 构建选项
 * <p>
 * 各选项相互独立，可任意组合：
 * <ul>
 *     <li>style: OSS 图片样式（在控制台配置）</li>
 *     <li>process: 自定义图片处理参数</li>
 *     <li>preferInternal: 是否优先内网</li>
 * </ul>
 * </p>
 *
 * @author erp
 */
@Getter
@Builder
public class OssUrlOptions {

	/**
	 * OSS 图片样式名称（在 OSS 控制台配置的样式）
	 * <p>
	 * 例如: "export-thumbnail", "watermark", "resize-200"
	 * </p>
	 */
	private String style;

	/**
	 * 自定义 x-oss-process 参数（与 style 互斥，优先使用 process）
	 * <p>
	 * 例如: "image/resize,w_200/quality,q_80"
	 * </p>
	 */
	private String process;

	/**
	 * 是否优先使用内网域名
	 * <ul>
	 *     <li>true: 优先内网（节省流量费，需要服务器在同区域 VPC）</li>
	 *     <li>false: 使用外网/CDN（默认）</li>
	 * </ul>
	 */
	@Builder.Default
	private boolean preferInternal = false;

	/**
	 * 内网协议（仅 preferInternal=true 时生效）
	 */
	@Builder.Default
	private String internalScheme = "http";

	// ========== 常用预设 ==========

	/**
	 * 无额外选项
	 */
	public static OssUrlOptions none() {
		return OssUrlOptions.builder().build();
	}

	/**
	 * 仅指定样式
	 *
	 * @param style 样式名称
	 */
	public static OssUrlOptions withStyle(String style) {
		return OssUrlOptions.builder().style(style).build();
	}

	/**
	 * 仅使用内网
	 */
	public static OssUrlOptions internal() {
		return OssUrlOptions.builder().preferInternal(true).build();
	}

	/**
	 * 内网 + 样式
	 *
	 * @param style  样式名称
	 * @param scheme 内网协议 (http/https)
	 */
	public static OssUrlOptions internalWithStyle(String style, String scheme) {
		return OssUrlOptions.builder()
				.style(style)
				.preferInternal(true)
				.internalScheme(scheme)
				.build();
	}

	/**
	 * 内网（指定协议）
	 *
	 * @param scheme 内网协议 (http/https)
	 */
	public static OssUrlOptions internal(String scheme) {
		return OssUrlOptions.builder()
				.preferInternal(true)
				.internalScheme(scheme)
				.build();
	}

}
