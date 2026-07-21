package com.erp.admin.system.service;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.sts20150401.Client;
import com.aliyun.sts20150401.models.AssumeRoleRequest;
import com.aliyun.sts20150401.models.AssumeRoleResponse;
import com.aliyun.sts20150401.models.AssumeRoleResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.erp.admin.system.config.BucketConfig;
import com.erp.admin.system.exception.SystemBusinessException;
import com.erp.admin.system.model.dto.OssUrlOptions;
import com.erp.admin.system.model.vo.OssPostSignatureVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

/**
 * 阿里云OSS服务 - 支持多桶
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

	private final OssClientFactory ossClientFactory;

	private final ObjectMapper objectMapper;

	/**
	 * 服务端直传：将字节数组上传到指定桶
	 * @param bucketKey 桶别名
	 * @param data 文件数据
	 * @param objectKey 对象键
	 * @return objectKey
	 */
	public String putObject(String bucketKey, byte[] data, String objectKey) {
		BucketConfig config = ossClientFactory.getBucketConfig(bucketKey);
		OSS ossClient = ossClientFactory.getOssClient(bucketKey);

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
			PutObjectRequest putObjectRequest = new PutObjectRequest(
					config.getBucketName(), objectKey, inputStream);
			ossClient.putObject(putObjectRequest);
			log.info("上传文件成功, bucketKey={}, objectKey={}", bucketKey, objectKey);
			return objectKey;
		}
		catch (Exception e) {
			log.error("上传文件失败, bucketKey={}, objectKey={}", bucketKey, objectKey, e);
			throw SystemBusinessException.ossServiceUnavailable("上传文件失败: " + e.getMessage());
		}
	}

	/**
	 * 获取下载URL（通过 bucketKey）
	 * 公有桶返回直链（优先CDN），私有桶返回签名URL
	 * @param bucketKey 桶别名
	 * @param objectKey 对象键
	 * @return 下载URL
	 */
	public String getDownloadUrl(String bucketKey, String objectKey) {
		if (objectKey == null) {
			return null;
		}
		BucketConfig config = ossClientFactory.getBucketConfig(bucketKey);
		return getDownloadUrlByConfig(config, objectKey);
	}

	/**
	 * 获取下载URL（通过 bucketName 反向查找配置）
	 * 公有桶返回直链（优先CDN），私有桶返回签名URL
	 * @param bucketName 桶名称
	 * @param objectKey 对象键
	 * @return 下载URL
	 */
	public String getDownloadUrlByBucketName(String bucketName, String objectKey) {
		if (objectKey == null) {
			return null;
		}
		BucketConfig config = ossClientFactory.getBucketConfigByName(bucketName);
		return getDownloadUrlByConfig(config, objectKey);
	}

	/**
	 * 根据配置获取下载URL
	 * @param config 桶配置
	 * @param objectKey 对象键
	 * @return 下载URL
	 */
	private String getDownloadUrlByConfig(BucketConfig config, String objectKey) {
		if (config.isPublic()) {
			return config.getPublicUrl(objectKey);
		}
		else {
			return generateSignedUrlByConfig(config, objectKey);
		}
	}

	/**
	 * 生成签名URL（私有桶下载，通过 bucketKey）
	 * @param bucketKey 桶别名
	 * @param objectKey 对象键
	 * @return 签名URL
	 */
	public String generateSignedUrl(String bucketKey, String objectKey) {
		BucketConfig config = ossClientFactory.getBucketConfig(bucketKey);
		OSS ossClient = ossClientFactory.getOssClient(bucketKey);
		return doGenerateSignedUrl(config, ossClient, objectKey);
	}

	/**
	 * 生成签名URL（私有桶下载，通过 BucketConfig）
	 * @param config 桶配置
	 * @param objectKey 对象键
	 * @return 签名URL
	 */
	private String generateSignedUrlByConfig(BucketConfig config, String objectKey) {
		// 通过 bucketName 获取 OSSClient（需要从缓存中查找对应的 bucketKey）
		OSS ossClient = ossClientFactory.getOssClientByBucketName(config.getBucketName());
		return doGenerateSignedUrl(config, ossClient, objectKey);
	}

	/**
	 * 执行签名URL生成
	 */
	private String doGenerateSignedUrl(BucketConfig config, OSS ossClient, String objectKey) {
		try {
			Date expiration = new Date(System.currentTimeMillis()
					+ config.getSignedUrlExpireSeconds() * 1000);
			GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
					config.getBucketName(), objectKey, HttpMethod.GET);
			request.setExpiration(expiration);

			URL signedUrl = ossClient.generatePresignedUrl(request);
			log.debug("生成签名URL, bucketName={}, objectKey={}", config.getBucketName(), objectKey);
			return signedUrl.toString();
		}
		catch (Exception e) {
			log.error("生成签名URL失败, bucketName={}, objectKey={}", config.getBucketName(), objectKey, e);
			throw SystemBusinessException.ossSignatureGenerateFailed("生成签名URL失败", e);
		}
	}

	/**
	 * 获取OSS上传签名信息
	 * @param bucketKey 桶别名
	 * @return 签名信息VO
	 */
	public OssPostSignatureVO getPostSignature(String bucketKey) {
		try {
			BucketConfig config = ossClientFactory.getBucketConfig(bucketKey);
			AssumeRoleResponseBody.AssumeRoleResponseBodyCredentials stsData = getCredential(bucketKey);

			String accessKeyId = stsData.accessKeyId;
			String accessKeySecret = stsData.accessKeySecret;
			String securityToken = stsData.securityToken;
			String region = config.getRegion();
			String uploadDir = config.getPathPrefix();
			Long signExpireTime = config.getSignExpireSeconds();
			String host = config.getHostUrl();

			// 获取x-oss-credential里的date
			ZonedDateTime today = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
			String date = today.format(formatter);

			// 获取x-oss-date
			ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);
			DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
			String xOssDate = now.format(formatter2);

			// 步骤1：创建policy
			String xOssCredential = accessKeyId + "/" + date + "/" + region + "/oss/aliyun_v4_request";

			Map<String, Object> policy = new HashMap<>();
			policy.put("expiration", generateExpiration(signExpireTime));

			List<Object> conditions = new ArrayList<>();

			Map<String, String> bucketCondition = new HashMap<>();
			bucketCondition.put("bucket", config.getBucketName());
			conditions.add(bucketCondition);

			Map<String, String> securityTokenCondition = new HashMap<>();
			securityTokenCondition.put("x-oss-security-token", securityToken);
			conditions.add(securityTokenCondition);

			Map<String, String> signatureVersionCondition = new HashMap<>();
			signatureVersionCondition.put("x-oss-signature-version", "OSS4-HMAC-SHA256");
			conditions.add(signatureVersionCondition);

			Map<String, String> credentialCondition = new HashMap<>();
			credentialCondition.put("x-oss-credential", xOssCredential);
			conditions.add(credentialCondition);

			Map<String, String> dateCondition = new HashMap<>();
			dateCondition.put("x-oss-date", xOssDate);
			conditions.add(dateCondition);

			conditions.add(Arrays.asList("eq", "$success_action_status", "200"));
			conditions.add(Arrays.asList("starts-with", "$key", uploadDir));

			policy.put("conditions", conditions);

			String jsonPolicy = this.objectMapper.writeValueAsString(policy);

			// 步骤2：构造待签名字符串
			String stringToSign = Base64.encodeBase64String(jsonPolicy.getBytes(StandardCharsets.UTF_8));

			// 步骤3：计算SigningKey
			byte[] dateKey = hmacsha256(("aliyun_v4" + accessKeySecret).getBytes(StandardCharsets.UTF_8), date);
			byte[] dateRegionKey = hmacsha256(dateKey, region);
			byte[] dateRegionServiceKey = hmacsha256(dateRegionKey, "oss");
			byte[] signingKey = hmacsha256(dateRegionServiceKey, "aliyun_v4_request");

			// 步骤4：计算Signature
			byte[] result = hmacsha256(signingKey, stringToSign);
			String signature = BinaryUtil.toHex(result);

			// 构建返回结果
			OssPostSignatureVO response = new OssPostSignatureVO();
			response.setVersion("OSS4-HMAC-SHA256");
			response.setPolicy(stringToSign);
			response.setXOssCredential(xOssCredential);
			response.setXOssDate(xOssDate);
			response.setSignature(signature);
			response.setSecurityToken(securityToken);
			response.setDir(uploadDir);
			response.setHost(host);

			log.info("生成OSS上传签名成功, bucketKey={}, dir={}", bucketKey, uploadDir);
			return response;

		}
		catch (SystemBusinessException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("生成OSS上传签名失败, bucketKey={}", bucketKey, e);
			throw SystemBusinessException.ossSignatureGenerateFailed("生成OSS上传签名失败", e);
		}
	}

	/**
	 * 删除OSS文件（通过 bucketKey）
	 * @param bucketKey 桶别名
	 * @param objectKey 对象键
	 */
	public void deleteObject(String bucketKey, String objectKey) {
		BucketConfig config = ossClientFactory.getBucketConfig(bucketKey);
		OSS ossClient = ossClientFactory.getOssClient(bucketKey);
		doDeleteObject(config, ossClient, objectKey);
	}

	/**
	 * 删除OSS文件（通过 bucketName 反向查找配置）
	 * @param bucketName 桶名称
	 * @param objectKey 对象键
	 */
	public void deleteObjectByBucketName(String bucketName, String objectKey) {
		BucketConfig config = ossClientFactory.getBucketConfigByName(bucketName);
		OSS ossClient = ossClientFactory.getOssClientByBucketName(bucketName);
		doDeleteObject(config, ossClient, objectKey);
	}

	/**
	 * 执行删除OSS文件
	 */
	private void doDeleteObject(BucketConfig config, OSS ossClient, String objectKey) {
		try {
			ossClient.deleteObject(config.getBucketName(), objectKey);
			log.info("删除OSS文件成功, bucketName={}, objectKey={}", config.getBucketName(), objectKey);
		}
		catch (Exception e) {
			log.error("删除OSS文件失败, bucketName={}, objectKey={}", config.getBucketName(), objectKey, e);
			throw SystemBusinessException.ossServiceUnavailable("删除OSS文件失败");
		}
	}

	/**
	 * 获取桶配置
	 * @param bucketKey 桶别名
	 * @return 桶配置
	 */
	public BucketConfig getBucketConfig(String bucketKey) {
		return ossClientFactory.getBucketConfig(bucketKey);
	}

	/**
	 * 获取 URL（通用方法，支持各种选项）
	 * <p>
	 * 公有桶：根据选项构建 URL（支持样式、内网等）<br>
	 * 私有桶：返回签名 URL（暂不支持样式参数）
	 * </p>
	 *
	 * @param bucketKey 桶别名
	 * @param objectKey 对象键
	 * @param options   URL 构建选项（可为 null）
	 * @return URL 字符串
	 */
	public String getUrl(String bucketKey, String objectKey, OssUrlOptions options) {
		if (objectKey == null) {
			return null;
		}

		BucketConfig config = ossClientFactory.getBucketConfig(bucketKey);

		// 私有桶需要签名 URL（暂不支持样式参数）
		if (!config.isPublic()) {
			return generateSignedUrl(bucketKey, objectKey);
		}

		// 公有桶使用 buildUrl
		OssUrlOptions opts = (options != null) ? options : OssUrlOptions.none();
		return config.buildUrl(
				objectKey,
				opts.getStyle(),
				opts.getProcess(),
				opts.isPreferInternal(),
				opts.getInternalScheme()
		);
	}

	/**
	 * 获取 URL（简化版，无额外选项）
	 *
	 * @param bucketKey 桶别名
	 * @param objectKey 对象键
	 * @return URL 字符串
	 */
	public String getUrl(String bucketKey, String objectKey) {
		return getUrl(bucketKey, objectKey, null);
	}

	/**
	 * 获取STS临时凭证
	 */
	private AssumeRoleResponseBody.AssumeRoleResponseBodyCredentials getCredential(String bucketKey) throws Exception {
		BucketConfig config = ossClientFactory.getBucketConfig(bucketKey);
		Client client = ossClientFactory.createStsClient(bucketKey);

		AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
				.setRoleArn(config.getRoleArn())
				.setRoleSessionName(config.getRoleSessionName());
		RuntimeOptions runtime = new RuntimeOptions();

		try {
			AssumeRoleResponse response = client.assumeRoleWithOptions(assumeRoleRequest, runtime);
			return response.body.credentials;
		}
		catch (TeaException error) {
			log.error("STS获取临时凭证失败: {}", error.getMessage());
			throw SystemBusinessException.stsCredentialFailed("获取STS临时凭证失败", error);
		}
	}

	/**
	 * 生成过期时间
	 */
	private String generateExpiration(long seconds) {
		long now = Instant.now().getEpochSecond();
		long expirationTime = now + seconds;
		Instant instant = Instant.ofEpochSecond(expirationTime);
		ZoneId zone = ZoneOffset.UTC;
		ZonedDateTime zonedDateTime = instant.atZone(zone);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		return zonedDateTime.format(formatter);
	}

	/**
	 * HMAC-SHA256加密
	 */
	private byte[] hmacsha256(byte[] key, String data) {
		try {
			SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(secretKeySpec);
			return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception e) {
			throw SystemBusinessException.ossSignatureGenerateFailed("HMAC-SHA256计算失败", e);
		}
	}

}
