package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsOutboundPickTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

public interface WmsOutboundPickTaskMapper extends ExtendMapper<WmsOutboundPickTask> {

    @Select("SELECT * FROM wms_outbound_pick_task WHERE id = #{id} FOR UPDATE")
    WmsOutboundPickTask selectByIdForUpdate(@Param("id") Long id);
}

