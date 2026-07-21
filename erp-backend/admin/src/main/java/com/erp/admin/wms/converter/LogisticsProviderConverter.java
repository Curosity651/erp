package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.LogisticsProviderDTO;
import com.erp.admin.wms.model.dto.WarehouseDTO;
import com.erp.admin.wms.model.entity.LogisticsProvider;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.vo.LogisticsProviderOptionVO;
import com.erp.admin.wms.model.vo.LogisticsProviderPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 物流商模型转换器
 *
 * @author erp
 */
@Mapper
public interface LogisticsProviderConverter {

	LogisticsProviderConverter INSTANCE = Mappers.getMapper(LogisticsProviderConverter.class);

	/**
	 * DTO 转 Entity
	 * @param dto DTO
	 * @return LogisticsProvider 实体对象
	 */
	LogisticsProvider dtoToEntity(LogisticsProviderDTO dto);


	/**
	 * PO 转 PageVO
	 * @param provider 物流商实体
	 * @return LogisticsProviderPageVO 物流商分页视图对象
	 */
	LogisticsProviderPageVO poToPageVo(LogisticsProvider provider);

	/**
	 * PO 转 OptionVO
	 * @param provider 物流商实体
	 * @return LogisticsProviderOptionVO 物流商下拉选项视图对象
	 */
	LogisticsProviderOptionVO poToOptionVo(LogisticsProvider provider);

}
