package com.erp.admin.product.excel;

import java.net.URL;

/**
 * 图片数据接口，VO 类实现此接口以支持图片字节数据的获取和设置。
 * <p>
 * 配合 {@link ImageAwareDataFetcher} 使用，在数据获取阶段自动下载图片
 * 并将字节数据填充到 VO 对象中。
 *
 * @see ImageAwareDataFetcher
 * @see ImageByteFetcher
 */
public interface ImageData {

	/**
	 * 获取图片 URL。
	 *
	 * @return 图片 URL，可能为 null
	 */
	URL getImageUrl();

	/**
	 * 设置图片字节数据。
	 *
	 * @param bytes 图片字节数据，下载失败时为空数组
	 */
	void setImageBytes(byte[] bytes);

}
