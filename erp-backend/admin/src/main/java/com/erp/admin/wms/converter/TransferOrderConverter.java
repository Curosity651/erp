package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.TransferOrderDTO;
import com.erp.admin.wms.model.entity.TransferOrder;
import com.erp.admin.wms.model.vo.TransferOrderDetailVO;
import com.erp.admin.wms.model.vo.TransferOrderPageVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 调拨单模型转换器
 *
 * @author erp
 */
@Mapper
public interface TransferOrderConverter {

	TransferOrderConverter INSTANCE = Mappers.getMapper(TransferOrderConverter.class);

	/**
	 * DTO 转 Entity
	 * @param dto 调拨单DTO
	 * @return TransferOrder 调拨单实体
	 */
	TransferOrder dtoToEntity(TransferOrderDTO dto);

	/**
	 * 更新实体（忽略空值）
	 * @param dto 调拨单DTO
	 * @param entity 调拨单实体
	 */
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntity(TransferOrderDTO dto, @MappingTarget TransferOrder entity);

	/**
	 * Entity 转 PageVO
	 */
	@Named("entityToPageVo")
	TransferOrderPageVO entityToPageVo(TransferOrder entity);

	/**
	 * Entity List 转 PageVO List
	 */
	@IterableMapping(qualifiedByName = "entityToPageVo")
	List<TransferOrderPageVO> entityListToPageVoList(List<TransferOrder> entities);

	/**
	 * Entity 转 DetailVO
	 */
	TransferOrderDetailVO entityToDetailVo(TransferOrder entity);

}
