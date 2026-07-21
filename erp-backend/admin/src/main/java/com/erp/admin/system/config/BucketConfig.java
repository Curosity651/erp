package com.erp.admin.system.config;

import lombok.Data;

/**
 * 单个 OSS 桶配置
 *
 * @author erp
 */
@Data
public class BucketConfig {

    /**
     * 桶名称
     */
    private String bucketName;

    /**
     * 存储类型: PUBLIC / PRIVATE
     */
    private String storageType;

    /**
     * 端点
     */
    private String endpoint;

    /**
     * 区域
     */
    private String region;

    /**
     * 路径前缀
     */
    private String pathPrefix = "";

    /**
     * CDN域名（可选）
     */
    private String cdnDomain;

    /**
     * 是否启用HTTPS
     */
    private Boolean enableHttps = true;

    /**
     * 内网端点（可选）
     */
    private String internalEndpoint;

    /**
     * STS端点
     */
    private String stsEndpoint;

    /**
     * STS角色ARN
     */
    private String roleArn;

    /**
     * STS会话名称
     */
    private String roleSessionName = "oss-upload-session";

    /**
     * 签名有效期（秒），用于前端直传
     */
    private Long signExpireSeconds = 1800L;

    /**
     * 签名URL有效期（秒），用于私有桶下载
     */
    private Long signedUrlExpireSeconds = 1800L;

    /**
     * 判断是否为公有桶
     */
    public boolean isPublic() {
        return "PUBLIC".equals(this.storageType);
    }

    /**
     * 获取 Host URL
     */
    public String getHostUrl() {
        String protocol = Boolean.TRUE.equals(this.enableHttps) ? "https" : "http";
        return protocol + "://" + this.bucketName + "." + this.endpoint;
    }

    /**
     * 获取公开访问 URL（优先 CDN）
     */
    public String getPublicUrl(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        String protocol = Boolean.TRUE.equals(this.enableHttps) ? "https" : "http";
        if (this.cdnDomain != null && !this.cdnDomain.isEmpty()) {
            return protocol + "://" + this.cdnDomain + "/" + objectKey;
        }
        return protocol + "://" + this.bucketName + "." + this.endpoint + "/" + objectKey;
    }

    /**
     * 构建完整 URL（支持样式、内网等选项）
     *
     * @param objectKey      对象键
     * @param style          OSS 样式名称（可选）
     * @param process        自定义 process 参数（可选，与 style 互斥，优先使用 process）
     * @param preferInternal 是否优先内网
     * @param internalScheme 内网协议
     * @return 完整 URL
     */
    public String buildUrl(String objectKey, String style, String process,
                           boolean preferInternal, String internalScheme) {
        if (objectKey == null) {
            return null;
        }

        // 1. 确定域名和协议
        String protocol;
        String host;

        if (preferInternal && hasText(this.internalEndpoint)) {
            // 内网域名
            protocol = hasText(internalScheme) ? internalScheme : "http";
            host = this.bucketName + "." + this.internalEndpoint;
        } else if (hasText(this.cdnDomain)) {
            // CDN 域名
            protocol = Boolean.TRUE.equals(this.enableHttps) ? "https" : "http";
            host = this.cdnDomain;
        } else {
            // 外网域名
            protocol = Boolean.TRUE.equals(this.enableHttps) ? "https" : "http";
            host = this.bucketName + "." + this.endpoint;
        }

        // 2. 构建基础 URL
        String path = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        StringBuilder url = new StringBuilder();
        url.append(protocol).append("://").append(host).append("/").append(path);

        // 3. 添加图片处理参数（process 优先于 style）
        String separator = objectKey.contains("?") ? "&" : "?";
        if (hasText(process)) {
            url.append(separator).append("x-oss-process=").append(process);
        } else if (hasText(style)) {
            url.append(separator).append("x-oss-process=style/").append(style);
        }

        return url.toString();
    }

    private boolean hasText(String str) {
        return str != null && !str.isEmpty();
    }

}
