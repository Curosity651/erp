package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.PurchaseInboundItemDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 采购入库单明细模型转换器
 *
 * @author erp
 */
@Mapper
public interface PurchaseInboundItemConverter {

    PurchaseInboundItemConverter INSTANCE = Mappers.getMapper(PurchaseInboundItemConverter.class);

    /**
     * DTO 转 Entity
     * @param dto 采购入库单明细DTO
     * @return PurchaseInboundOrderItem 采购入库单明细实体
     */
    PurchaseInboundOrderItem dtoToEntity(PurchaseInboundItemDTO dto);

    /**
     * DTO列表 转 Entity列表
     * @param dtoList 采购入库单明细DTO列表
     * @return List<PurchaseInboundOrderItem> 采购入库单明细实体列表
     */
    List<PurchaseInboundOrderItem> dtoListToEntityList(List<PurchaseInboundItemDTO> dtoList);

}
