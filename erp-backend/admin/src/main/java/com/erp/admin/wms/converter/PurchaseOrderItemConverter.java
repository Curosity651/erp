package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.PurchaseOrderItemDTO;
import com.erp.admin.wms.model.entity.PurchaseOrderItem;
import com.erp.admin.wms.model.vo.PurchaseOrderItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 采购单明细模型转换器
 *
 * @author erp
 */
@Mapper
public interface PurchaseOrderItemConverter {

	PurchaseOrderItemConverter INSTANCE = Mappers.getMapper(PurchaseOrderItemConverter.class);

	/**
	 * DTO 转 Entity
	 */
	@Mapping(target = "purchaseOrderId", ignore = true)
	@Mapping(target = "amount", ignore = true)
	@Mapping(target = "shippedQuantity", ignore = true)
	@Mapping(target = "receivedQuantity", ignore = true)
	@Mapping(target = "createTime", ignore = true)
	@Mapping(target = "updateTime", ignore = true)
	PurchaseOrderItem dtoToEntity(PurchaseOrderItemDTO dto);

	/**
	 * DTO列表 转 Entity列表
	 */
	List<PurchaseOrderItem> dtoListToEntityList(List<PurchaseOrderItemDTO> dtoList);

	/**
	 * Entity 转 VO
	 */
	PurchaseOrderItemVO entityToVo(PurchaseOrderItem entity);

	/**
	 * Entity列表 转 VO列表
	 */
	List<PurchaseOrderItemVO> entityListToVoList(List<PurchaseOrderItem> entityList);

}
