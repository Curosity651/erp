package com.erp.admin.shop.converter;

import com.erp.admin.shop.model.entity.Shop;
import com.erp.admin.shop.model.vo.ShopPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 店铺（含凭证信息）模型转换器
 *
 * @author erp 2025-09-21 21:11:47
 */
@Mapper
public interface ShopConverter {

	ShopConverter INSTANCE = Mappers.getMapper(ShopConverter.class);

	/**
	 * PO 转 PageVO
	 * @param shop 店铺（含凭证信息）
	 * @return ShopPageVO 店铺（含凭证信息）PageVO
	 */
	ShopPageVO poToPageVo(Shop shop);

}
