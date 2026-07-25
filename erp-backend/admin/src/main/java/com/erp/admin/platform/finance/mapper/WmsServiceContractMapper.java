package com.erp.admin.platform.finance.mapper;

import com.erp.admin.platform.finance.model.entity.WmsServiceContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

@Mapper
public interface WmsServiceContractMapper extends ExtendMapper<WmsServiceContract> {
    @Select("SELECT * FROM wms_service_contract WHERE id=#{id} FOR UPDATE")
    WmsServiceContract selectByIdForUpdate(@Param("id") Long id);
}

