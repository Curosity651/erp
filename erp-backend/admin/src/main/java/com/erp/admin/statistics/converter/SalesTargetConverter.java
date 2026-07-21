package com.erp.admin.statistics.converter;

import com.erp.admin.statistics.model.SalesTarget;
import com.erp.admin.statistics.model.vo.SalesTargetPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 销售目标模型转换器
 *
 * @author erp 2025-10-25 21:40:57
 */
@Mapper
public interface SalesTargetConverter {

	SalesTargetConverter INSTANCE = Mappers.getMapper(SalesTargetConverter.class);

	/**
	 * PO 转 PageVO
	 * @param salesTarget 销售目标
	 * @return SalesTargetPageVO 销售目标PageVO
	 */
	SalesTargetPageVO poToPageVo(SalesTarget salesTarget);

}
