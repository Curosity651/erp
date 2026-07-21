package com.erp.admin.system.config;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 阿里云 OSS 配置属性 - 支持多桶
 *
 * @author erp
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

	/**
	 * 访问密钥ID
	 */
	private String accessKeyId;

	/**
	 * 访问密钥Secret
	 */
	private String accessKeySecret;

	/**
	 * 多桶配置：key 为桶别名
	 */
	private Map<String, BucketConfig> buckets = new LinkedHashMap<>();

	/**
	 * 反向索引：bucketName -> BucketConfig（启动时构建）
	 */
	@Getter
	private Map<String, BucketConfig> bucketsByName;

	/**
	 * 导出相关配置
	 */
	private Export export = new Export();

	/**
	 * 初始化反向索引
	 */
	@PostConstruct
	public void init() {
		bucketsByName = buckets.values().stream()
				.collect(Collectors.toMap(
						BucketConfig::getBucketName,
						Function.identity(),
						(existing, replacement) -> {
							log.warn("存在多个配置指向同一个桶: {}", existing.getBucketName());
							return existing;
						}
				));
		log.info("OSS桶配置初始化完成, 共 {} 个桶, 反向索引 {} 条", buckets.size(), bucketsByName.size());
	}

	/**
	 * 获取桶配置（通过 bucketKey）
	 * @param bucketKey 桶别名
	 * @return 桶配置，不存在则返回 null
	 */
	public BucketConfig getBucket(String bucketKey) {
		return buckets.get(bucketKey);
	}

	/**
	 * 获取桶配置（通过 bucketName）
	 * @param bucketName 桶名称
	 * @return 桶配置，不存在则返回 null
	 */
	public BucketConfig getBucketByName(String bucketName) {
		return bucketsByName.get(bucketName);
	}

	/**
	 * 导出图片配置
	 */
	@Data
	public static class Export {

		/**
		 * 导出是否优先使用内网域名
		 */
		private Boolean useInternalDomain = Boolean.FALSE;

		/**
		 * 导出图片样式名
		 */
		private String imageStyle = "export-thumbnail";

		/**
		 * 内网访问协议
		 */
		private String imageScheme = "http";

		/**
		 * 图片下载连接超时（毫秒）
		 */
		private Integer connectTimeoutMs = 1000;

		/**
		 * 图片下载读取超时（毫秒）
		 */
		private Integer readTimeoutMs = 5000;

	}

}
