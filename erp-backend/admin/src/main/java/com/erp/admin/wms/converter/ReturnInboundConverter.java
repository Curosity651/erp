package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.ReturnInboundDTO;
import com.erp.admin.wms.model.entity.ReturnInboundOrder;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 退货入库单模型转换器
 * 重构后：简化转换逻辑，不再支持编辑
 *
 * @author erp
 */
@Mapper
public interface ReturnInboundConverter {

    ReturnInboundConverter INSTANCE = Mappers.getMapper(ReturnInboundConverter.class);

    /**
     * DTO 转 Entity
     * @param dto 退货入库单DTO
     * @return ReturnInboundOrder 退货入库单实体
     */
    ReturnInboundOrder dtoToEntity(ReturnInboundDTO dto);

}
