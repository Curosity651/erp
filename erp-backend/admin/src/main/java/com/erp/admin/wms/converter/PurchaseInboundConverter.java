package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.PurchaseInboundDTO;
import com.erp.admin.wms.model.entity.PurchaseInboundOrder;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * 采购入库单模型转换器
 *
 * @author erp
 */
@Mapper
public interface PurchaseInboundConverter {

    PurchaseInboundConverter INSTANCE = Mappers.getMapper(PurchaseInboundConverter.class);

    /**
     * DTO 转 Entity
     * @param dto 采购入库单DTO
     * @return PurchaseInboundOrder 采购入库单实体
     */
    PurchaseInboundOrder dtoToEntity(PurchaseInboundDTO dto);

    /**
     * 更新实体（忽略空值）
     * @param dto 采购入库单DTO
     * @param entity 采购入库单实体
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PurchaseInboundDTO dto, @MappingTarget PurchaseInboundOrder entity);

}
