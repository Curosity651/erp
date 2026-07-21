package com.erp.admin.system.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * OSS POST上传签名视图对象
 *
 * @author ballcat
 */
@Data
@Schema(title = "OSS POST上传签名视图对象")
public class OssPostSignatureVO {

	/**
	 * 签名版本
	 */
	@Schema(title = "签名版本", example = "OSS4-HMAC-SHA256")
	private String version;

	/**
	 * 上传策略
	 */
	@Schema(title = "上传策略")
	private String policy;

	/**
	 * OSS凭证
	 */
	@JsonProperty("xOssCredential")
	@Schema(title = "OSS凭证")
	private String xOssCredential;

	/**
	 * OSS日期
	 */
	@JsonProperty("xOssDate")
	@Schema(title = "OSS日期")
	private String xOssDate;

	/**
	 * 签名
	 */
	@Schema(title = "签名")
	private String signature;

	/**
	 * 安全令牌
	 */
	@Schema(title = "安全令牌")
	private String securityToken;

	/**
	 * 上传目录
	 */
	@Schema(title = "上传目录")
	private String dir;

	/**
	 * 主机地址
	 */
	@Schema(title = "主机地址")
	private String host;

}
