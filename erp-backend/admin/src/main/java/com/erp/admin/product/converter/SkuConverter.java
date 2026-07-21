package com.erp.admin.product.converter;

import com.erp.admin.product.model.dto.SkuCreateDTO;
import com.erp.admin.product.model.entity.Sku;
import com.erp.admin.product.model.vo.SkuExportVO;
import com.erp.admin.product.model.vo.SkuPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * SKU管理表模型转换器
 *
 * @author ballcat 2025-07-27 02:02:06
 */
@Mapper
public interface SkuConverter {

	SkuConverter INSTANCE = Mappers.getMapper(SkuConverter.class);

	/**
	 * PO 转 PageVO
	 * @param sku SKU管理表
	 * @return SkuPageVO SKU管理表PageVO
	 */
	SkuPageVO poToPageVo(Sku sku);

	/**
	 * PO 转 ExportVO
	 * @param sku SKU管理表
	 * @return SkuPageVO SKU管理表ExportVO
	 */
	SkuExportVO poToExportVO(Sku sku);

	/**
	 * CreateDTO 转 PO
	 * @param createDTO SKU创建DTO
	 * @return Sku SKU实体
	 */
	Sku createDtoToPo(SkuCreateDTO createDTO);

	/**
	 * PO 转 DetailVO
	 * @param sku SKU实体
	 * @return SkuDetailVO SKU详情VO
	 */
	com.erp.admin.product.model.vo.SkuDetailVO poToDetailVo(Sku sku);

}
