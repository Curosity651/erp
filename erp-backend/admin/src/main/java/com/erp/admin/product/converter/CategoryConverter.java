package com.erp.admin.product.converter;

import com.erp.admin.product.model.entity.Category;
import com.erp.admin.product.model.vo.CategoryPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 商品品类模型转换器
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Mapper
public interface CategoryConverter {

	CategoryConverter INSTANCE = Mappers.getMapper(CategoryConverter.class);

	/**
	 * PO 转 PageVO
	 * @param category 商品品类
	 * @return CategoryPageVO 商品品类PageVO
	 */
	CategoryPageVO poToPageVo(Category category);

}
