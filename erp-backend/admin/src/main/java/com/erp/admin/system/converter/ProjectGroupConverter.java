package com.erp.admin.system.converter;

import com.erp.admin.system.model.entity.ProjectGroup;
import com.erp.admin.system.model.vo.ProjectGroupPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 项目组管理表模型转换器
 *
 * @author ballcat 2025-07-26 15:27:25
 */
@Mapper
public interface ProjectGroupConverter {

	ProjectGroupConverter INSTANCE = Mappers.getMapper(ProjectGroupConverter.class);

	/**
	 * PO 转 PageVO
	 * @param projectGroup 项目组管理表
	 * @return ProjectGroupPageVO 项目组管理表PageVO
	 */
	ProjectGroupPageVO poToPageVo(ProjectGroup projectGroup);

}
