package com.erp.admin.product.converter;

import com.erp.admin.product.model.entity.Brand;
import com.erp.admin.product.model.vo.BrandPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 品牌管理模型转换器
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Mapper
public interface BrandConverter {

	BrandConverter INSTANCE = Mappers.getMapper(BrandConverter.class);

	/**
	 * PO 转 PageVO
	 * @param brand 品牌管理
	 * @return BrandPageVO 品牌管理PageVO
	 */
	BrandPageVO poToPageVo(Brand brand);

}
