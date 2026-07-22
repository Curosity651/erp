package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.FboSyncLog;
import com.erp.admin.wms.model.qo.FboSyncLogQO;
import com.erp.admin.wms.model.vo.FboSyncLogDetailVO;
import com.erp.admin.wms.model.vo.FboSyncLogPageVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.mapper.ExtendMapper;

/**
 * FBO同步日志 Mapper
 *
 * @author erp
 */
public interface FboSyncLogMapper extends ExtendMapper<FboSyncLog> {

    /**
     * 分页查询
     */
    IPage<FboSyncLogPageVO> queryPage(IPage<FboSyncLogPageVO> page, @Param("tenantId") Long tenantId,
            @Param("qo") FboSyncLogQO qo);

    /**
     * 统计今日日志数量（用于生成日志编号）
     */
    int countTodayLogs(@Param("prefix") String prefix);

    /**
     * 查询详情
     */
    FboSyncLogDetailVO selectDetail(@Param("id") Long id, @Param("tenantId") Long tenantId);

}
