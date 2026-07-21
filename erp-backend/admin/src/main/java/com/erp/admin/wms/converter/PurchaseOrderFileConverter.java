package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.entity.PurchaseOrderFile;
import com.erp.admin.wms.model.vo.PurchaseOrderFileVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 采购单附件转换器
 *
 * @author erp
 */
@Mapper
public interface PurchaseOrderFileConverter {

	PurchaseOrderFileConverter INSTANCE = Mappers.getMapper(PurchaseOrderFileConverter.class);

	/**
	 * 实体转VO
	 */
	@Mapping(target = "fileTypeDesc", ignore = true)
	@Mapping(target = "fileName", ignore = true)
	@Mapping(target = "fileSize", ignore = true)
	@Mapping(target = "contentType", ignore = true)
	PurchaseOrderFileVO entityToVo(PurchaseOrderFile entity);

}
