package com.erp.admin.system.converter;

import com.erp.admin.system.model.entity.Position;
import com.erp.admin.system.model.vo.PositionPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 岗位管理表模型转换器
 *
 * @author ballcat 2025-07-26 15:27:26
 */
@Mapper
public interface PositionConverter {

	PositionConverter INSTANCE = Mappers.getMapper(PositionConverter.class);

	/**
	 * PO 转 PageVO
	 * @param position 岗位管理表
	 * @return PositionPageVO 岗位管理表PageVO
	 */
	PositionPageVO poToPageVo(Position position);

}
