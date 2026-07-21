package com.erp.admin.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SKU文件类型枚举
 *
 * @author system
 */
@Getter
@AllArgsConstructor
public enum SkuFileType {

	/**
	 * 实物图片
	 */
	ACTUAL_IMAGE("actual_image", "实物图片", true),

	/**
	 * 平台图片
	 */
	PLATFORM_IMAGE("platform_image", "平台图片", true),

	/**
	 * 说明书
	 */
	MANUAL("manual", "说明书", false),

	/**
	 * 标签
	 */
	LABEL("label", "标签", false),

	/**
	 * 安装视频
	 */
	INSTALLATION_VIDEO("installation_video", "安装视频", false),

	/**
	 * 质检报告
	 */
	QUALITY_REPORT("quality_report", "质检报告", false),

	/**
	 * 规格书
	 */
	SPECIFICATION("specification", "规格书", false),

	/**
	 * 演示视频
	 */
	DEMO_VIDEO("demo_video", "演示视频", false),

	/**
	 * 包装信息
	 */
	PACKAGING_INFO("packaging_info", "包装信息", false),

	/**
	 * 安全信息
	 */
	SAFETY_INFO("safety_info", "安全信息", false);

	/**
	 * 文件类型代码
	 */
	private final String code;

	/**
	 * 文件类型名称
	 */
	private final String name;

	/**
	 * 是否为必需文件（实物图片和平台图片至少要有一张）
	 */
	private final boolean required;

	/**
	 * 根据代码获取枚举
	 */
	public static SkuFileType fromCode(String code) {
		for (SkuFileType type : values()) {
			if (type.getCode().equals(code)) {
				return type;
			}
		}
		return null;
	}

}
