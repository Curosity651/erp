package com.erp.admin.system.service;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.sts20150401.Client;
import com.aliyun.teaopenapi.models.Config;
import com.erp.admin.system.config.AliyunOssProperties;
import com.erp.admin.system.config.BucketConfig;
import com.erp.admin.system.exception.SystemBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OSS Client 工厂
 * <p>
 * 管理 OSSClient 和 StsClient 的创建与缓存，支持多桶配置
 * </p>
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssClientFactory {

	private final AliyunOssProperties properties;

	/**
	 * OSSClient 缓存，按 bucketKey 区分
	 */
	private final ConcurrentHashMap<String, OSS> ossClientCache = new ConcurrentHashMap<>();

	/**
	 * 获取桶配置（通过 bucketKey）
	 *
	 * @param bucketKey 桶别名
	 * @return 桶配置
	 * @throws SystemBusinessException 桶不存在时抛出
	 */
	public BucketConfig getBucketConfig(String bucketKey) {
		BucketConfig config = properties.getBucket(bucketKey);
		if (config == null) {
			log.error("未配置的存储桶: bucketKey={}", bucketKey);
			throw SystemBusinessException.ossConfigError("未配置的存储桶: " + bucketKey);
		}
		return config;
	}

	/**
	 * 获取桶配置（通过 bucketName 反向查找）
	 *
	 * @param bucketName 桶名称
	 * @return 桶配置
	 * @throws SystemBusinessException 桶不存在时抛出
	 */
	public BucketConfig getBucketConfigByName(String bucketName) {
		BucketConfig config = properties.getBucketByName(bucketName);
		if (config == null) {
			log.error("未找到存储桶配置: bucketName={}", bucketName);
			throw SystemBusinessException.ossConfigError("未找到存储桶配置: " + bucketName);
		}
		return config;
	}

	/**
	 * 获取 OSSClient（线程安全，自动缓存复用）
	 * <p>
	 * 首次调用时创建 Client 并缓存，后续调用直接返回缓存实例。
	 * Client 内部维护连接池，可安全并发使用。
	 * </p>
	 *
	 * @param bucketKey 桶别名
	 * @return OSSClient 实例（缓存的）
	 */
	public OSS getOssClient(String bucketKey) {
		return ossClientCache.computeIfAbsent(bucketKey, this::createOssClient);
	}

	/**
	 * 获取 OSSClient（通过 bucketName 反向查找）
	 *
	 * @param bucketName 桶名称
	 * @return OSSClient 实例（缓存的）
	 */
	public OSS getOssClientByBucketName(String bucketName) {
		BucketConfig config = getBucketConfigByName(bucketName);
		// 使用 bucketName 作为缓存 key（因为不知道原始的 bucketKey）
		return ossClientCache.computeIfAbsent(bucketName, key -> createOssClientByConfig(config));
	}

	/**
	 * 创建 OSSClient（通过 bucketKey）
	 *
	 * @param bucketKey 桶别名
	 * @return 新创建的 OSSClient 实例
	 */
	private OSS createOssClient(String bucketKey) {
		BucketConfig config = getBucketConfig(bucketKey);
		return createOssClientByConfig(config);
	}

	/**
	 * 创建 OSSClient（通过 BucketConfig）
	 *
	 * @param config 桶配置
	 * @return 新创建的 OSSClient 实例
	 */
	private OSS createOssClientByConfig(BucketConfig config) {
		DefaultCredentialProvider credentialsProvider = CredentialsProviderFactory
				.newDefaultCredentialProvider(
						properties.getAccessKeyId(),
						properties.getAccessKeySecret()
				);

		ClientBuilderConfiguration clientConfig = new ClientBuilderConfiguration();
		clientConfig.setSignatureVersion(SignVersion.V4);
		clientConfig.setProtocol(config.getEnableHttps() ? Protocol.HTTPS : Protocol.HTTP);

		OSS client = OSSClientBuilder.create()
				.endpoint(config.getEndpoint())
				.credentialsProvider(credentialsProvider)
				.clientConfiguration(clientConfig)
				.region(config.getRegion())
				.build();

		log.info("创建 OSSClient: bucketName={}, endpoint={}", config.getBucketName(), config.getEndpoint());
		return client;
	}

	/**
	 * 创建 STS Client
	 *
	 * @param bucketKey 桶别名
	 * @return STS Client 实例
	 * @throws Exception 创建失败时抛出
	 */
	public Client createStsClient(String bucketKey) throws Exception {
		BucketConfig config = getBucketConfig(bucketKey);

		Config stsConfig = new Config()
				.setAccessKeyId(properties.getAccessKeyId())
				.setAccessKeySecret(properties.getAccessKeySecret());
		stsConfig.endpoint = config.getStsEndpoint();

		return new Client(stsConfig);
	}

	/**
	 * 获取公共凭证的 AccessKeyId
	 */
	public String getAccessKeyId() {
		return properties.getAccessKeyId();
	}

	/**
	 * 获取公共凭证的 AccessKeySecret
	 */
	public String getAccessKeySecret() {
		return properties.getAccessKeySecret();
	}

	/**
	 * 关闭所有缓存的 OSSClient（应用关闭时自动调用）
	 */
	@PreDestroy
	public void shutdown() {
		log.info("正在关闭所有 OSSClient，共 {} 个", ossClientCache.size());
		ossClientCache.forEach((key, client) -> {
			try {
				client.shutdown();
				log.info("关闭 OSSClient 成功: bucketKey={}", key);
			} catch (Exception e) {
				log.warn("关闭 OSSClient 失败: bucketKey={}, error={}", key, e.getMessage());
			}
		});
		ossClientCache.clear();
	}

}
