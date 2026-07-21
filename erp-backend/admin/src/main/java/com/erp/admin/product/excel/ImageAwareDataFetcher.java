package com.erp.admin.product.excel;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.ballcat.fastexcel.handler.DataFetcher;

/**
 * 图片感知数据获取器，装饰原有 DataFetcher，在获取数据后自动下载图片并填充到 VO。
 * <p>
 * 使用装饰器模式包装原有的 DataFetcher，在每批数据返回前自动下载图片
 * 并将字节数据填充到 VO 对象中。
 * <p>
 * 特性：
 * <ul>
 *   <li>无 ThreadLocal：图片数据直接存储在 VO 对象中</li>
 *   <li>无临时文件：图片字节数据直接写入 Excel</li>
 *   <li>内存友好：图片数据随 VO 对象一起被 GC 回收</li>
 * </ul>
 *
 * @param <T> 数据类型，必须实现 {@link ImageData} 接口
 * @see ImageData
 * @see ImageByteFetcher
 */
@Slf4j
public class ImageAwareDataFetcher<T extends ImageData> implements DataFetcher<T> {

	/**
	 * 原始数据获取器
	 */
	private final DataFetcher<T> delegate;

	/**
	 * 图片字节获取器
	 */
	private final ImageByteFetcher imageFetcher;

	/**
	 * 创建图片感知数据获取器。
	 *
	 * @param delegate 原始数据获取器
	 * @param imageFetcher 图片字节获取器
	 */
	public ImageAwareDataFetcher(DataFetcher<T> delegate, ImageByteFetcher imageFetcher) {
		this.delegate = delegate;
		this.imageFetcher = imageFetcher;
	}

	@Override
	public List<T> get() {
		// 获取数据
		List<T> data = delegate.get();

		if (data == null || data.isEmpty()) {
			return data;
		}

		// 提取所有图片 URL
		List<URL> urls = data.stream()
			.map(ImageData::getImageUrl)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		if (urls.isEmpty()) {
			log.debug("No image URLs found in batch of {} items", data.size());
			return data;
		}

		// 批量下载图片
		Map<String, byte[]> imageMap = imageFetcher.fetchAll(urls);

		// 填充图片字节数据到 VO
		for (T item : data) {
			URL url = item.getImageUrl();
			if (url != null) {
				byte[] bytes = imageMap.getOrDefault(url.toString(), new byte[0]);
				item.setImageBytes(bytes);
			}
		}

		log.debug("Processed batch of {} items with {} images", data.size(), urls.size());
		return data;
	}

}
