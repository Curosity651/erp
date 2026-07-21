package com.erp.admin.product.converter;

import com.erp.admin.product.model.entity.Supplier;
import com.erp.admin.product.model.vo.SupplierPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 供应商模型转换器
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Mapper
public interface SupplierConverter {

	SupplierConverter INSTANCE = Mappers.getMapper(SupplierConverter.class);

	/**
	 * PO 转 PageVO
	 * @param supplier 供应商
	 * @return SupplierPageVO 供应商PageVO
	 */
	SupplierPageVO poToPageVo(Supplier supplier);

}
