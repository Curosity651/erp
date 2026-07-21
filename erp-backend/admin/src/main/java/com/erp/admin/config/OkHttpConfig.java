package com.erp.admin.config;

import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OkHttp 客户端配置。
 * <p>
 * 提供全局共享的 OkHttpClient Bean，支持连接池和并发控制。
 */
@Configuration
public class OkHttpConfig {

	@Value("${okhttp.max-requests:20}")
	private int maxRequests;

	@Value("${okhttp.max-requests-per-host:10}")
	private int maxRequestsPerHost;

	@Value("${okhttp.connect-timeout:3000}")
	private int connectTimeout;

	@Value("${okhttp.read-timeout:5000}")
	private int readTimeout;

	@Value("${okhttp.pool.max-idle:10}")
	private int maxIdleConnections;

	@Value("${okhttp.pool.keep-alive:300}")
	private int keepAliveDuration;

	@Bean
	public OkHttpClient okHttpClient() {
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequests(maxRequests);
		dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);

		ConnectionPool connectionPool = new ConnectionPool(
			maxIdleConnections,
			keepAliveDuration,
			TimeUnit.SECONDS
		);

		return new OkHttpClient.Builder()
			.dispatcher(dispatcher)
			.connectionPool(connectionPool)
			.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
			.readTimeout(readTimeout, TimeUnit.MILLISECONDS)
			.build();
	}

}
