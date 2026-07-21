package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.AdjustmentItemDTO;
import com.erp.admin.wms.model.entity.AdjustmentOrderItem;
import com.erp.admin.wms.model.vo.AdjustmentItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 调整单明细模型转换器
 *
 * @author erp
 */
@Mapper
public interface AdjustmentItemConverter {

	AdjustmentItemConverter INSTANCE = Mappers.getMapper(AdjustmentItemConverter.class);

	/**
	 * DTO 转 Entity
	 * @param dto 调整单明细DTO
	 * @return AdjustmentOrderItem 调整单明细实体
	 */
	AdjustmentOrderItem dtoToEntity(AdjustmentItemDTO dto);

	/**
	 * DTO列表 转 Entity列表
	 * @param dtoList 调整单明细DTO列表
	 * @return List<AdjustmentOrderItem> 调整单明细实体列表
	 */
	List<AdjustmentOrderItem> dtoListToEntityList(List<AdjustmentItemDTO> dtoList);

	/**
	 * Entity 转 VO
	 * @param entity 调整单明细实体
	 * @return AdjustmentItemVO 调整单明细VO
	 */
	AdjustmentItemVO entityToVO(AdjustmentOrderItem entity);

	/**
	 * Entity列表 转 VO列表
	 * @param entityList 调整单明细实体列表
	 * @return List<AdjustmentItemVO> 调整单明细VO列表
	 */
	List<AdjustmentItemVO> entityListToVOList(List<AdjustmentOrderItem> entityList);

}
