package com.erp.admin.sync.service;

import java.time.LocalDateTime;

import com.erp.admin.sync.mapper.SyncCursorMapper;
import com.erp.admin.sync.model.entity.SyncCursor;
import com.erp.admin.sync.model.enums.SyncTaskTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncCursorService {

    private final SyncCursorMapper syncCursorMapper;

    /**
     * 获取游标时间（UTC），不存在返回 null（表示首次同步）
     */
    public LocalDateTime getCursor(Long shopId, String platform, SyncTaskTypeEnum taskType) {
        SyncCursor cursor = syncCursorMapper.selectByShopPlatformTask(shopId, platform, taskType.name());
        return cursor != null ? cursor.getCursorTime() : null;
    }

    /**
     * 推进游标（仅当 newCursorTime > 当前游标时才更新，防止回退）
     *
     * @param shopId        店铺 ID
     * @param platform      平台
     * @param taskType      任务类型
     * @param cursorTimeUtc 新的游标时间（UTC）
     */
    public void advanceCursor(Long shopId, String platform, SyncTaskTypeEnum taskType, LocalDateTime cursorTimeUtc) {
        SyncCursor existing = syncCursorMapper.selectByShopPlatformTask(shopId, platform, taskType.name());

        if (existing == null) {
            // 首次：插入
            SyncCursor cursor = new SyncCursor();
            cursor.setShopId(shopId);
            cursor.setPlatform(platform);
            cursor.setTaskType(taskType.name());
            cursor.setCursorTime(cursorTimeUtc);
            syncCursorMapper.insert(cursor);
            log.info("[SYNC_CURSOR] 初始化游标: shopId={}, platform={}, taskType={}, cursor={}",
                    shopId, platform, taskType.name(), cursorTimeUtc);
        } else if (cursorTimeUtc.isAfter(existing.getCursorTime())) {
            // 仅前进，不回退
            existing.setCursorTime(cursorTimeUtc);
            syncCursorMapper.updateById(existing);
            log.debug("[SYNC_CURSOR] 推进游标: shopId={}, platform={}, taskType={}, cursor={}",
                    shopId, platform, taskType.name(), cursorTimeUtc);
        } else {
            log.debug("[SYNC_CURSOR] 游标未推进（新值 {} <= 当前值 {}）",
                    cursorTimeUtc, existing.getCursorTime());
        }
    }
}
