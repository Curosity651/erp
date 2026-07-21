package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.entity.StockFlow;
import com.erp.admin.wms.model.vo.StockFlowDetailVO;
import com.erp.admin.wms.model.vo.StockFlowPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 库存流水模型转换器
 *
 * @author erp
 */
@Mapper
public interface StockFlowConverter {

	StockFlowConverter INSTANCE = Mappers.getMapper(StockFlowConverter.class);


	/**
	 * Entity 转 PageVO
	 * @param entity 库存流水实体
	 * @return StockFlowPageVO 分页VO
	 */
	StockFlowPageVO entityToPageVO(StockFlow entity);

	/**
	 * Entity列表 转 PageVO列表
	 * @param entityList 库存流水实体列表
	 * @return List<StockFlowPageVO> 分页VO列表
	 */
	List<StockFlowPageVO> entityListToPageVOList(List<StockFlow> entityList);

	/**
	 * Entity 转 DetailVO
	 * @param entity 库存流水实体
	 * @return StockFlowDetailVO 详情VO
	 */
	StockFlowDetailVO entityToDetailVO(StockFlow entity);


	/**
	 * Entity列表 转 DetailVO列表
	 * @param entityList 库存流水实体列表
	 * @return List<StockFlowDetailVO> 库存流水VO列表
	 */
	List<StockFlowDetailVO> entityToDetailVOList(List<StockFlow> entityList);

}
