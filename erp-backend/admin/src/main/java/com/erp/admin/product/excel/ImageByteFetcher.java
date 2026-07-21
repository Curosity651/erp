package com.erp.admin.product.excel;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 图片字节获取器，负责多线程并行下载图片并返回字节数据。
 * <p>
 * 使用 OkHttp 异步请求并行下载图片，提高导出性能。下载的图片字节数据
 * 直接返回，无需存储到临时文件。
 * <p>
 * 特性：
 * <ul>
 *   <li>跨批次缓存：相同 URL 在整个导出过程中只下载一次</li>
 *   <li>容量限制：缓存大小超过阈值时自动清理最早的条目</li>
 *   <li>URL 去重：同一批次内相同 URL 只下载一次</li>
 *   <li>容错性：单张图片下载失败不影响其他图片</li>
 *   <li>超时控制：批次总超时时间可配置</li>
 * </ul>
 *
 * @see ImageData
 * @see ImageAwareDataFetcher
 */
@Slf4j
public class ImageByteFetcher {

	/**
	 * 批次总超时时间（秒）
	 */
	private static final long BATCH_TIMEOUT_SECONDS = 60;

	/**
	 * 默认缓存容量上限（字节），默认 50MB
	 */
	private static final long DEFAULT_MAX_CACHE_SIZE = 50 * 1024 * 1024L;

	private final OkHttpClient okHttpClient;

	/**
	 * 跨批次图片缓存，使用 LRU 策略
	 * Key: URL 字符串
	 * Value: 图片字节数据
	 */
	private final Map<String, byte[]> cache;

	/**
	 * 当前缓存大小（字节）
	 */
	private final AtomicLong cacheSize = new AtomicLong(0);

	/**
	 * 缓存容量上限（字节）
	 */
	private final long maxCacheSize;

	/**
	 * 创建图片字节获取器，使用默认缓存容量（50MB）。
	 *
	 * @param okHttpClient OkHttp 客户端（建议使用 Spring Bean 复用连接池）
	 */
	public ImageByteFetcher(OkHttpClient okHttpClient) {
		this(okHttpClient, DEFAULT_MAX_CACHE_SIZE);
	}

	/**
	 * 创建图片字节获取器，指定缓存容量上限。
	 *
	 * @param okHttpClient OkHttp 客户端（建议使用 Spring Bean 复用连接池）
	 * @param maxCacheSize 缓存容量上限（字节）
	 */
	public ImageByteFetcher(OkHttpClient okHttpClient, long maxCacheSize) {
		this.okHttpClient = okHttpClient;
		this.maxCacheSize = maxCacheSize;
		// 使用 LinkedHashMap 实现 LRU，accessOrder=true 表示按访问顺序排序
		this.cache = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true));
	}

	/**
	 * 批量下载图片并返回 URL -> 字节数据的映射。
	 * <p>
	 * 优先从缓存获取，缓存未命中时才发起网络请求。
	 * 下载失败的 URL 对应空字节数组（不缓存失败结果）。
	 *
	 * @param urls 图片 URL 列表，可包含 null 和重复值
	 * @return URL 字符串 -> 图片字节数据的映射，不会返回 null
	 */
	public Map<String, byte[]> fetchAll(List<URL> urls) {
		if (urls == null || urls.isEmpty()) {
			return Collections.emptyMap();
		}

		// 过滤 null 并去重
		Set<String> uniqueUrls = urls.stream()
			.filter(Objects::nonNull)
			.map(URL::toString)
			.collect(Collectors.toSet());

		if (uniqueUrls.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, byte[]> result = new ConcurrentHashMap<>();
		
		// 分离缓存命中和未命中的 URL
		Set<String> urlsToFetch = uniqueUrls.stream()
			.filter(urlStr -> {
				byte[] cached = cache.get(urlStr);
				if (cached != null) {
					result.put(urlStr, cached);
					return false; // 缓存命中，不需要下载
				}
				return true; // 缓存未命中，需要下载
			})
			.collect(Collectors.toSet());

		int cacheHits = uniqueUrls.size() - urlsToFetch.size();
		if (cacheHits > 0) {
			log.debug("Cache hits: {}, need to fetch: {}", cacheHits, urlsToFetch.size());
		}

		if (urlsToFetch.isEmpty()) {
			return result;
		}

		// 下载缓存未命中的图片
		fetchAndCache(urlsToFetch, result);

		log.debug("Fetched {} images, cache size: {} bytes", result.size(), cacheSize.get());
		return result;
	}

	/**
	 * 下载图片并缓存结果。
	 */
	private void fetchAndCache(Set<String> urlsToFetch, Map<String, byte[]> result) {
		CountDownLatch latch = new CountDownLatch(urlsToFetch.size());

		for (String urlStr : urlsToFetch) {
			Request request = new Request.Builder()
				.url(urlStr)
				.build();

			okHttpClient.newCall(request).enqueue(new Callback() {
				@Override
				public void onFailure(Call call, IOException e) {
					log.warn("Failed to fetch image: {}, error: {}", urlStr, e.getMessage());
					result.put(urlStr, new byte[0]); // 失败不缓存
					latch.countDown();
				}

				@Override
				public void onResponse(Call call, Response response) {
					try (ResponseBody body = response.body()) {
						if (response.isSuccessful() && body != null) {
							byte[] bytes = body.bytes();
							result.put(urlStr, bytes);
							// 只缓存成功下载的非空图片
							if (bytes.length > 0) {
								putToCache(urlStr, bytes);
							}
							log.debug("Fetched image: {}, size: {} bytes", urlStr, bytes.length);
						}
						else {
							log.warn("Failed to fetch image: {}, status: {}", urlStr, response.code());
							result.put(urlStr, new byte[0]); // 失败不缓存
						}
					}
					catch (Exception e) {
						log.warn("Failed to read image bytes: {}, error: {}", urlStr, e.getMessage());
						result.put(urlStr, new byte[0]); // 失败不缓存
					}
					finally {
						latch.countDown();
					}
				}
			});
		}

		try {
			boolean completed = latch.await(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!completed) {
				log.warn("Image fetch batch timeout after {} seconds, {} images may not be downloaded",
						BATCH_TIMEOUT_SECONDS, latch.getCount());
				// 为超时未完成的 URL 设置空字节数组
				for (String urlStr : urlsToFetch) {
					result.putIfAbsent(urlStr, new byte[0]);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Image fetch interrupted");
			// 为中断时未完成的 URL 设置空字节数组
			for (String urlStr : urlsToFetch) {
				result.putIfAbsent(urlStr, new byte[0]);
			}
		}
	}

	/**
	 * 将图片放入缓存，如果超过容量上限则清理最早的条目。
	 */
	private void putToCache(String urlStr, byte[] bytes) {
		// 先检查是否需要清理
		while (cacheSize.get() + bytes.length > maxCacheSize && !cache.isEmpty()) {
			evictOldest();
		}

		// 如果单张图片就超过上限，不缓存
		if (bytes.length > maxCacheSize) {
			log.debug("Image too large to cache: {}, size: {} bytes", urlStr, bytes.length);
			return;
		}

		byte[] previous = cache.put(urlStr, bytes);
		if (previous != null) {
			// 替换已有条目，调整大小
			cacheSize.addAndGet(bytes.length - previous.length);
		}
		else {
			cacheSize.addAndGet(bytes.length);
		}
	}

	/**
	 * 清理最早的缓存条目（LRU）。
	 */
	private void evictOldest() {
		synchronized (cache) {
			java.util.Iterator<Map.Entry<String, byte[]>> iterator = cache.entrySet().iterator();
			if (iterator.hasNext()) {
				Map.Entry<String, byte[]> entry = iterator.next();
				iterator.remove();
				cacheSize.addAndGet(-entry.getValue().length);
				log.debug("Evicted cache entry: {}, freed: {} bytes", entry.getKey(), entry.getValue().length);
			}
		}
	}

	/**
	 * 获取当前缓存大小（字节）。
	 *
	 * @return 缓存大小
	 */
	public long getCacheSize() {
		return cacheSize.get();
	}

	/**
	 * 获取当前缓存条目数。
	 *
	 * @return 缓存条目数
	 */
	public int getCacheEntryCount() {
		return cache.size();
	}

	/**
	 * 清空缓存。
	 */
	public void clearCache() {
		cache.clear();
		cacheSize.set(0);
		log.debug("Cache cleared");
	}

}
