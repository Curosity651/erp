package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.entity.StocktakeOrderItem;
import com.erp.admin.wms.model.vo.StocktakeItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 盘点单明细模型转换器
 *
 * @author erp
 */
@Mapper
public interface StocktakeItemConverter {

	StocktakeItemConverter INSTANCE = Mappers.getMapper(StocktakeItemConverter.class);

	/**
	 * Entity 转 VO
	 * @param entity 盘点单明细实体
	 * @return StocktakeItemVO 盘点单明细VO
	 */
	StocktakeItemVO entityToVO(StocktakeOrderItem entity);

	/**
	 * Entity列表 转 VO列表
	 * @param entityList 盘点单明细实体列表
	 * @return List<StocktakeItemVO> 盘点单明细VO列表
	 */
	List<StocktakeItemVO> entityListToVOList(List<StocktakeOrderItem> entityList);

}
