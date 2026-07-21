package com.erp.admin.wms.task;

import com.erp.admin.wms.facade.OzonFboStockSyncFacade;
import com.erp.admin.wms.model.enums.FboSyncType;
import com.erp.admin.wms.model.vo.FboSyncResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FBO 库存同步定时任务
 * <p>
 * Ozon FBO 库存数据每天 UTC 07:00 更新，因此定时任务设置为北京时间 15:00 执行。
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FboStockSyncScheduler {

    private final OzonFboStockSyncFacade ozonFboStockSyncFacade;

    /**
     * 每天北京时间 15:00 执行 FBO 库存同步
     */
    @Scheduled(cron = "0 0 15 * * ?")
    public void syncFboStock() {
        log.info("开始执行 FBO 库存定时同步任务");
        try {
            List<FboSyncResultVO> results = ozonFboStockSyncFacade.syncAllShops(FboSyncType.SCHEDULED.getCode());
            log.info("FBO 库存定时同步完成，共同步 {} 个店铺", results.size());
        } catch (Exception e) {
            log.error("FBO 库存定时同步异常", e);
        }
    }

}
