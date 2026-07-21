package com.erp.admin.system.config;

/**
 * OSS 图片样式常量
 * <p>
 * 与 OSS 控制台配置的图片样式名称对应
 * </p>
 *
 * @author erp
 */
public final class OssImageStyles {

	private OssImageStyles() {
	}

	/**
	 * 导出缩略图样式
	 * <p>
	 * 用于 Excel 导出时嵌入的图片，减小文件体积
	 * </p>
	 */
	public static final String EXPORT_THUMBNAIL = "export-thumbnail";

	// 后续可扩展更多样式
	// public static final String WATERMARK = "watermark";
	// public static final String AVATAR_SMALL = "avatar-small";

}
