package com.erp.admin.wms.task;

import com.erp.admin.common.mail.MailService;
import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.tenant.mapper.SysTenantMapper;
import com.erp.admin.wms.facade.InventoryForecastFacade;
import com.erp.admin.wms.mail.InventoryAlertMailBuilder;
import com.erp.admin.wms.model.entity.InventoryAlertConfig;
import com.erp.admin.wms.model.enums.ForecastStatus;
import com.erp.admin.wms.model.qo.ForecastSummaryQO;
import com.erp.admin.wms.model.vo.ForecastSummaryResult;
import com.erp.admin.wms.model.vo.ForecastSummaryVO;
import com.erp.admin.wms.service.InventoryConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.business.system.model.entity.SysUser;
import org.ballcat.business.system.service.SysUserService;
import org.ballcat.common.model.domain.PageParam;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存预警通知定时任务
 *
 * @author erp
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAlertTask {

    private final InventoryConfigService inventoryConfigService;
    private final InventoryForecastFacade inventoryForecastFacade;
    private final SysUserService sysUserService;
    private final MailService mailService;
    private final InventoryAlertMailBuilder mailBuilder;
    private final SysTenantMapper sysTenantMapper;

    /**
     * 每天早上9点执行库存预警检查。
     * <p>多租户：配置/预测/库存均按货主隔离，后台线程无请求上下文，须逐启用货主
     * {@code TenantContext.runAs} 建立上下文再检查（各货主读各自的全局配置与预测作用域）。
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkInventoryAlert() {
        log.info("开始执行库存预警检查定时任务（逐货主）");
        List<Long> erpTenantIds = sysTenantMapper.listEnabledErpTenantIds();
        for (Long erpTenantId : erpTenantIds) {
            try {
                TenantContext.runAs(erpTenantId, () -> {
                    checkForCurrentTenant(erpTenantId);
                    return null;
                });
            } catch (Exception e) {
                // 单货主失败不影响其余货主
                log.error("库存预警检查失败, erpTenantId={}", erpTenantId, e);
            }
        }
        log.info("库存预警检查定时任务完成，覆盖{}个货主", erpTenantIds.size());
    }

    /**
     * 检查单个货主（须已建立其 TenantContext）。
     */
    private void checkForCurrentTenant(Long erpTenantId) {
        // 获取该货主的全局配置
        InventoryAlertConfig globalConfig = inventoryConfigService.getGlobalAlertConfig();

        // 检查是否启用通知
        if (!Boolean.TRUE.equals(globalConfig.getNotifyEnabled())) {
            log.debug("库存预警通知未启用，跳过, erpTenantId={}", erpTenantId);
            return;
        }

        // 检查是否配置了通知人员
        if (CollectionUtils.isEmpty(globalConfig.getNotifyUserIds())) {
            log.debug("未配置通知人员，跳过, erpTenantId={}", erpTenantId);
            return;
        }

        // 查询需要预警的 SKU（紧急和断货状态；预测作用域随当前货主上下文隔离）
        ForecastSummaryQO qo = new ForecastSummaryQO();
        PageParam pageParam = new PageParam();
        pageParam.setPage(1);
        pageParam.setSize(1000); // 获取所有预警数据

        ForecastSummaryResult result = inventoryForecastFacade.getSummary(pageParam, qo);

        // 筛选告急和断货的 SKU
        List<ForecastSummaryVO> alertItems = result.getList().stream()
                .filter(vo -> vo.getStatus() == ForecastStatus.CRITICAL || vo.getStatus() == ForecastStatus.STOCKOUT)
                .collect(Collectors.toList());

        if (alertItems.isEmpty()) {
            log.info("库存预警检查完成，无预警项, erpTenantId={}", erpTenantId);
            return;
        }

        // 获取通知人员信息
        List<SysUser> notifyUsers = sysUserService.listByUserIds(globalConfig.getNotifyUserIds());

        // 发送邮件通知
        sendAlertNotification(alertItems, notifyUsers, globalConfig.getNotifyThresholdDays());

        log.info("库存预警检查完成，发现{}个预警项, erpTenantId={}", alertItems.size(), erpTenantId);
    }

    /**
     * 发送预警邮件通知
     */
    private void sendAlertNotification(List<ForecastSummaryVO> alertItems,
                                       List<SysUser> notifyUsers,
                                       int thresholdDays) {
        // 1. 提取有效邮箱
        List<String> validEmails = mailService.extractValidEmails(notifyUsers);

        if (validEmails.isEmpty()) {
            log.warn("没有有效的通知邮箱，跳过邮件发送");
            return;
        }

        // 2. 构建邮件内容
        String subject = mailBuilder.buildSubject(alertItems.size());
        String htmlContent = mailBuilder.buildHtmlContent(alertItems, thresholdDays);

        // 3. 发送邮件
        int successCount = mailService.sendHtmlMail(validEmails, subject, htmlContent);

        log.info("库存预警邮件发送完成，成功 {}/{} 封", successCount, validEmails.size());
    }

    /**
     * 手动触发库存预警检查（用于测试）
     */
    public void manualCheck() {
        log.info("手动触发库存预警检查");
        checkInventoryAlert();
    }
}
