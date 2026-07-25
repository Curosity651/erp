package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.WmsOutboundPickTaskLine;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

public interface WmsOutboundPickTaskLineMapper extends ExtendMapper<WmsOutboundPickTaskLine> {

    @Select("SELECT * FROM wms_outbound_pick_task_line WHERE id = #{id} FOR UPDATE")
    WmsOutboundPickTaskLine selectByIdForUpdate(@Param("id") Long id);

    default List<WmsOutboundPickTaskLine> selectByTaskId(Long taskId) {
        return selectList(WrappersX.lambdaQueryX(WmsOutboundPickTaskLine.class)
                .eq(WmsOutboundPickTaskLine::getTaskId, taskId)
                .orderByAsc(WmsOutboundPickTaskLine::getLocationCode)
                .orderByAsc(WmsOutboundPickTaskLine::getSlotCode)
                .orderByAsc(WmsOutboundPickTaskLine::getSkuCode));
    }
}
