package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.SalesOutboundItemDTO;
import com.erp.admin.wms.model.entity.SalesOutboundOrderItem;
import com.erp.admin.wms.model.vo.SalesOutboundItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 销售出库单明细模型转换器
 *
 * @author erp
 */
@Mapper
public interface SalesOutboundItemConverter {

    SalesOutboundItemConverter INSTANCE = Mappers.getMapper(SalesOutboundItemConverter.class);

    /**
     * DTO 转 Entity
     * @param dto 销售出库单明细DTO
     * @return SalesOutboundOrderItem 销售出库单明细实体
     */
    SalesOutboundOrderItem dtoToEntity(SalesOutboundItemDTO dto);

    /**
     * DTO列表 转 Entity列表
     * @param dtoList 销售出库单明细DTO列表
     * @return List<SalesOutboundOrderItem> 销售出库单明细实体列表
     */
    List<SalesOutboundOrderItem> dtoListToEntityList(List<SalesOutboundItemDTO> dtoList);

	/**
     * Entity 转 VO
     * @param entity 销售出库单明细实体
     * @return SalesOutboundOrderItemVO 销售出库单明细VO
     */
	SalesOutboundItemVO entityToVO(SalesOutboundOrderItem entity);

	/**
     * Entity列表 转 VO列表
     * @param entityList 销售出库单明细实体列表
     * @return List<SalesOutboundOrderItemVO> 销售出库单明细VO列表
     */
	List<SalesOutboundItemVO> entityListToVOList(List<SalesOutboundOrderItem> entityList);

}
