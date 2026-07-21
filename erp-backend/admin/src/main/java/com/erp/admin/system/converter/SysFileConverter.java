package com.erp.admin.system.converter;

import com.erp.admin.system.model.dto.SysFileUploadDTO;
import com.erp.admin.system.model.entity.SysFile;
import com.erp.admin.system.model.vo.SysFileVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 系统文件模型转换器
 *
 * @author erp
 */
@Mapper
public interface SysFileConverter {

	SysFileConverter INSTANCE = Mappers.getMapper(SysFileConverter.class);

	/**
	 * DTO 转 Entity
	 * @param dto 上传数据传输对象
	 * @return SysFile 实体
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "bucketName", ignore = true)
	@Mapping(target = "createBy", ignore = true)
	@Mapping(target = "createTime", ignore = true)
	SysFile dtoToEntity(SysFileUploadDTO dto);

	/**
	 * Entity 转 VO
	 * @param entity 实体
	 * @return SysFileVO 视图对象
	 */
	@Mapping(target = "url", ignore = true)
	SysFileVO entityToVo(SysFile entity);

}
