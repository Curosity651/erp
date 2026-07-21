package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.TransferOrderItemDTO;
import com.erp.admin.wms.model.entity.TransferOrderItem;
import com.erp.admin.wms.model.vo.TransferOrderItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 调拨单明细模型转换器
 *
 * @author erp
 */
@Mapper
public interface TransferOrderItemConverter {

	TransferOrderItemConverter INSTANCE = Mappers.getMapper(TransferOrderItemConverter.class);

	/**
	 * DTO 转 Entity
	 * @param dto 调拨单明细DTO
	 * @return TransferOrderItem 调拨单明细实体
	 */
	TransferOrderItem dtoToEntity(TransferOrderItemDTO dto);

	/**
	 * DTO列表 转 Entity列表
	 * @param dtoList 调拨单明细DTO列表
	 * @return List<TransferOrderItem> 调拨单明细实体列表
	 */
	List<TransferOrderItem> dtoListToEntityList(List<TransferOrderItemDTO> dtoList);

	/**
	 * Entity 转 VO
	 * @param entity 调拨单明细实体
	 * @return TransferOrderItemVO 调拨单明细VO
	 */
	TransferOrderItemVO entityToVo(TransferOrderItem entity);

	/**
	 * Entity列表 转 VO列表
	 * @param entityList 调拨单明细实体列表
	 * @return List<TransferOrderItemVO> 调拨单明细VO列表
	 */
	List<TransferOrderItemVO> entityListToVoList(List<TransferOrderItem> entityList);

}
