package com.erp.admin.wms.converter;

import com.erp.admin.wms.model.entity.FboSyncLog;
import com.erp.admin.wms.model.vo.FboSyncLogPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * FBO同步日志转换器
 *
 * @author erp
 */
@Mapper
public interface FboSyncLogConverter {

    FboSyncLogConverter INSTANCE = Mappers.getMapper(FboSyncLogConverter.class);

    /**
     * Entity 转 PageVO
     * @param entity FBO同步日志实体
     * @return FboSyncLogPageVO 分页视图对象
     */
    FboSyncLogPageVO entityToPageVO(FboSyncLog entity);

}
