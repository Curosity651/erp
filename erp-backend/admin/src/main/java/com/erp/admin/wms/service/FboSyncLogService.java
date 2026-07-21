package com.erp.admin.wms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.mapper.FboSyncLogMapper;
import com.erp.admin.wms.model.entity.FboSyncLog;
import com.erp.admin.wms.model.qo.FboSyncLogQO;
import com.erp.admin.wms.model.vo.FboSyncLogDetailVO;
import com.erp.admin.wms.model.vo.FboSyncLogPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * FBO 同步日志服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FboSyncLogService extends ExtendServiceImpl<FboSyncLogMapper, FboSyncLog> {

    private static final DateTimeFormatter LOG_NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 分页查询同步日志
     */
    public PageResult<FboSyncLogPageVO> queryPage(PageParam pageParam, FboSyncLogQO qo) {
        IPage<FboSyncLogPageVO> page = baseMapper.queryPage(PageUtil.prodPage(pageParam), qo);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    /**
     * 获取同步日志详情
     */
    public FboSyncLogDetailVO getDetail(Long id) {
        return baseMapper.selectDetail(id);
    }

    /**
     * 生成日志编号
     * 格式：FBO-SYNC-{yyyyMMdd}-{序号}
     */
    public String generateLogNo() {
        String dateStr = LocalDate.now().format(LOG_NO_DATE_FORMAT);
        String prefix = "FBO-SYNC-" + dateStr + "-";

        // 查询当天日志数量
        int count = baseMapper.countTodayLogs(prefix);
        int nextSeq = count + 1;

        return String.format("%s%03d", prefix, nextSeq);
    }

    /**
     * 创建同步日志（开始）
     */
    public FboSyncLog createLog(String platform, Long shopId, String syncType) {
        FboSyncLog syncLog = new FboSyncLog();
        syncLog.setLogNo(generateLogNo());
        syncLog.setPlatform(platform);
        syncLog.setShopId(shopId);
        syncLog.setSyncType(syncType);
        syncLog.setSyncStatus("RUNNING");
        syncLog.setSyncTime(LocalDateTime.now());
        syncLog.setTotalCount(0);
        syncLog.setSuccessCount(0);
        syncLog.setFailCount(0);
        syncLog.setUnmappedCount(0);
        syncLog.setAutoCreatedWarehouseCount(0);
        this.save(syncLog);
        return syncLog;
    }

}
