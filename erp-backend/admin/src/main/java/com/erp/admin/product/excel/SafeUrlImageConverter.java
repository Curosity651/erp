package com.erp.admin.product.excel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.idev.excel.util.IoUtils;

/**
 * A safe URL->image converter for EasyExcel that won't break the whole export when an
 * image fails to load. It returns empty image data on any exception.
 */
public class SafeUrlImageConverter implements Converter<URL> {

	public static int urlConnectTimeout = 1000;

	public static int urlReadTimeout = 5000;

	@Override
	public Class<?> supportJavaTypeKey() {
		return URL.class;
	}

	@Override
	public WriteCellData<?> convertToExcelData(URL value, ExcelContentProperty contentProperty,
			GlobalConfiguration globalConfiguration) throws IOException {
		if (value == null) {
			return new WriteCellData<>(new byte[0]);
		}

		InputStream inputStream = null;
		try {
			URLConnection urlConnection = value.openConnection();
			urlConnection.setConnectTimeout(urlConnectTimeout);
			urlConnection.setReadTimeout(urlReadTimeout);
			inputStream = urlConnection.getInputStream();
			byte[] bytes = IoUtils.toByteArray(inputStream);
			return new WriteCellData<>(bytes);
		}
		catch (Exception ignore) {
			// swallow and return empty so that export continues
			return new WriteCellData<>(new byte[0]);
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException ignored) {
				}
			}
		}
	}

}
