package com.erp.admin.order.service.common;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.erp.admin.order.common.OrderQueryPreprocessor;
import com.erp.admin.order.model.qo.ErpOrderQO;
import com.erp.admin.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订单查询通用 Helper
 * <p>
 * 封装 queryPreprocessor + 批量数据填充（shop）。
 * 各平台 QueryService 按需调用。
 */
@Component
@RequiredArgsConstructor
public class OrderQueryHelper {

	private final OrderQueryPreprocessor queryPreprocessor;
	private final ShopService shopService;

	/**
	 * 预处理查询对象（设置平台、skuCode → skuCodes 转换）
	 */
	public ErpOrderQO preprocess(ErpOrderQO qo, String platformCode) {
		return queryPreprocessor.preprocess(qo, platformCode);
	}

	/**
	 * 批量填充店铺名称
	 */
	public <T> void enrichShopName(List<T> voList, Function<T, Long> shopIdGetter, BiConsumer<T, String> shopNameSetter) {
		shopService.enrichShopName(voList, shopIdGetter, shopNameSetter);
	}

}
