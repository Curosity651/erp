package com.erp.admin.product.converter;

import com.erp.admin.product.model.entity.SkuMapping;
import com.erp.admin.product.model.vo.SkuMappingPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * SKU映射模型转换器
 *
 * @author erp 2025-09-22 21:32:36
 */
@Mapper
public interface SkuMappingConverter {

	SkuMappingConverter INSTANCE = Mappers.getMapper(SkuMappingConverter.class);

	/**
	 * PO 转 PageVO
	 * @param skuMapping SKU映射
	 * @return SkuMappingPageVO SKU映射PageVO
	 */
	SkuMappingPageVO poToPageVo(SkuMapping skuMapping);

}
