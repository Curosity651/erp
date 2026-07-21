package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.dto.WarehouseDTO;
import com.erp.admin.wms.model.entity.Warehouse;
import com.erp.admin.wms.model.vo.WarehouseOptionVO;
import com.erp.admin.wms.model.vo.WarehousePageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 仓库模型转换器
 *
 * @author erp
 */
@Mapper
public interface WarehouseConverter {

	WarehouseConverter INSTANCE = Mappers.getMapper(WarehouseConverter.class);

	/**
	 * DTO 转 Entity
	 * @param warehouseDto 仓库DTO
	 * @return Warehouse 仓库实体对象
	 */
	Warehouse dtoToEntity(WarehouseDTO warehouseDto);

	/**
	 * PO 转 PageVO
	 * @param warehouse 仓库实体
	 * @return WarehousePageVO 仓库分页视图对象
	 */
	WarehousePageVO poToPageVo(Warehouse warehouse);

	/**
	 * PO 转 OptionVO
	 * @param warehouse 仓库实体
	 * @return WarehouseOptionVO 仓库下拉选项视图对象
	 */
	WarehouseOptionVO poToOptionVo(Warehouse warehouse);

}