package com.erp.admin.sync.mapper;

import com.erp.admin.sync.model.entity.SyncCursor;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

public interface SyncCursorMapper extends ExtendMapper<SyncCursor> {

    /**
     * 按 shopId + platform + taskType 查询游标（唯一索引）
     */
    default SyncCursor selectByShopPlatformTask(Long shopId, String platform, String taskType) {
        return this.selectOne(WrappersX.lambdaQueryX(SyncCursor.class)
                .eq(SyncCursor::getShopId, shopId)
                .eq(SyncCursor::getPlatform, platform)
                .eq(SyncCursor::getTaskType, taskType));
    }
}
