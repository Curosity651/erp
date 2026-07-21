package com.erp.admin.system.config;

/**
 * OSS 桶别名常量
 * <p>
 * 定义系统中使用的所有 OSS 桶别名，与 application.yml 中的 buckets 配置对应
 * </p>
 *
 * @author erp
 */
public final class OssBucketKeys {

	private OssBucketKeys() {
	}

	/**
	 * 公有桶 - 用于存放可公开访问的文件
	 * <p>
	 * 使用场景：SKU图片、面单PDF、导出文件等
	 * </p>
	 */
	public static final String PUBLIC_FILES = "public-files";

	/**
	 * 私有桶 - 用于存放需要签名访问的文件
	 * <p>
	 * 使用场景：敏感文档、合同文件等
	 * </p>
	 */
	public static final String PRIVATE_FILES = "private-files";

}
