package com.erp.admin.platform;

import java.time.Duration;

/**
 * HTTP 超时配置常量
 * <p>
 * 定义平台 HTTP 客户端的默认超时和特殊场景超时配置
 *
 * @author system
 */
public final class HttpTimeoutConfig {

    private HttpTimeoutConfig() {
        // 工具类，禁止实例化
    }

    // ==================== 默认超时（普通 API 接口）====================

    /**
     * 默认连接超时：10 秒
     */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * 默认读取超时：10 秒
     */
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

    /**
     * 默认写入超时：10 秒
     */
    public static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(10);

    // ==================== 文件下载超时（面单等大文件）====================

    /**
     * 文件下载读取超时：60 秒
     * <p>
     * 用于面单拉取、PDF 下载等耗时较长的接口
     */
    public static final Duration FILE_DOWNLOAD_READ_TIMEOUT = Duration.ofSeconds(60);
}
