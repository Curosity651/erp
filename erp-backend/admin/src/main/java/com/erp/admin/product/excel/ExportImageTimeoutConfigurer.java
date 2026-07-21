package com.erp.admin.product.excel;

import javax.annotation.PostConstruct;

import com.erp.admin.system.config.AliyunOssProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 将导出图片的连接/读取超时从配置注入到转换器。
 */
@Configuration
public class ExportImageTimeoutConfigurer {

	private final AliyunOssProperties properties;

	public ExportImageTimeoutConfigurer(AliyunOssProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	public void init() {
		if (this.properties.getExport() != null) {
			if (this.properties.getExport().getConnectTimeoutMs() != null) {
				SafeUrlImageConverter.urlConnectTimeout = this.properties.getExport().getConnectTimeoutMs();
			}
			if (this.properties.getExport().getReadTimeoutMs() != null) {
				SafeUrlImageConverter.urlReadTimeout = this.properties.getExport().getReadTimeoutMs();
			}
		}
	}

}
