package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.PurchaseOrderDTO;
import com.erp.admin.wms.model.entity.PurchaseOrder;
import com.erp.admin.wms.model.vo.PurchaseOrderDetailVO;
import com.erp.admin.wms.model.vo.PurchaseOrderPageVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * 采购单模型转换器
 *
 * @author erp
 */
@Mapper
public interface PurchaseOrderConverter {

	PurchaseOrderConverter INSTANCE = Mappers.getMapper(PurchaseOrderConverter.class);

	/**
	 * DTO 转 Entity（新建）
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "totalAmount", ignore = true)
	@Mapping(target = "prepayAmount", ignore = true)
	@Mapping(target = "prepayStatus", ignore = true)
	@Mapping(target = "prepayTime", ignore = true)
	@Mapping(target = "balanceStatus", ignore = true)
	@Mapping(target = "balancePayTime", ignore = true)
	@Mapping(target = "orderStatus", ignore = true)
	@Mapping(target = "createBy", ignore = true)
	@Mapping(target = "createTime", ignore = true)
	@Mapping(target = "updateBy", ignore = true)
	@Mapping(target = "updateTime", ignore = true)
	@Mapping(target = "deleted", ignore = true)
	PurchaseOrder dtoToEntity(PurchaseOrderDTO dto);

	/**
	 * DTO 更新 Entity（编辑）
	 */
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	@Mapping(target = "orderNo", ignore = true)
	@Mapping(target = "totalAmount", ignore = true)
	@Mapping(target = "prepayAmount", ignore = true)
	@Mapping(target = "prepayStatus", ignore = true)
	@Mapping(target = "prepayTime", ignore = true)
	@Mapping(target = "balanceStatus", ignore = true)
	@Mapping(target = "balancePayTime", ignore = true)
	@Mapping(target = "orderStatus", ignore = true)
	@Mapping(target = "createBy", ignore = true)
	@Mapping(target = "createTime", ignore = true)
	@Mapping(target = "updateBy", ignore = true)
	@Mapping(target = "updateTime", ignore = true)
	@Mapping(target = "deleted", ignore = true)
	void updateEntityFromDto(PurchaseOrderDTO dto, @MappingTarget PurchaseOrder entity);

	/**
	 * Entity 转 PageVO
	 */
	@Mapping(target = "supplierName", ignore = true)
	@Mapping(target = "orderStatusDesc", ignore = true)
	@Mapping(target = "skuCount", ignore = true)
	@Mapping(target = "totalQuantity", ignore = true)
	@Mapping(target = "totalShippedQuantity", ignore = true)
	@Mapping(target = "totalReceivedQuantity", ignore = true)
	PurchaseOrderPageVO entityToPageVo(PurchaseOrder entity);

	/**
	 * Entity 转 DetailVO
	 */
	@Mapping(target = "supplierName", ignore = true)
	@Mapping(target = "orderStatusDesc", ignore = true)
	@Mapping(target = "items", ignore = true)
	@Mapping(target = "totalQuantity", ignore = true)
	@Mapping(target = "totalShippedQuantity", ignore = true)
	@Mapping(target = "totalReceivedQuantity", ignore = true)
	PurchaseOrderDetailVO entityToDetailVo(PurchaseOrder entity);

}
