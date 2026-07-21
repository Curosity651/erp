package com.erp.admin.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixer.io API配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "fixer.api")
public class FixerApiProperties {

	/**
	 * Fixer.io API Keys (支持配置多个Key)
	 */
	private List<String> keys = new ArrayList<>();

	/**
	 * Fixer.io API 基础URL
	 */
	private String baseUrl = "http://data.fixer.io/api";

}