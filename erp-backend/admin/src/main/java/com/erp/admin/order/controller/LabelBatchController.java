package com.erp.admin.order.controller;

import com.erp.admin.order.model.qo.LabelBatchQO;
import com.erp.admin.order.model.vo.LabelBatchPageVO;
import com.erp.admin.order.model.vo.LabelBatchVO;
import com.erp.admin.order.service.LabelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 面单批次管理控制器
 * <p>
 * 职责：
 * - 提供面单批次的查询接口（分页列表、详情）
 * - 支持 WB 和 Ozon 平台的批次历史查询
 *
 * @author erp
 */
@RestController
@RequestMapping("/order/label-batch")
@Tag(name = "面单批次管理")
@RequiredArgsConstructor
@Slf4j
public class LabelBatchController {

    private final LabelService labelService;

    /**
     * 分页查询面单批次列表
     *
     * @param pageParam 分页参数
     * @param qo        查询条件（平台、状态、时间范围等）
     * @return 分页结果（不包含文件列表和失败订单项）
     */
    @GetMapping("/page")
    @PreAuthorize("@per.hasPermission('order:erp-order:read')")
    @Operation(summary = "分页查询面单批次列表")
    public ApiResult<PageResult<LabelBatchPageVO>> page(
            PageParam pageParam,
            LabelBatchQO qo) {
        PageResult<LabelBatchPageVO> result = labelService.pageBatches(pageParam, qo);
        return ApiResult.ok(result);
    }

    /**
     * 获取批次详情
     * <p>
     * 返回批次基本信息、文件列表（含下载链接）、失败订单项列表
     *
     * @param batchId 批次ID
     * @return 批次详情VO
     */
    @GetMapping("/{batchId}")
    @PreAuthorize("@per.hasPermission('order:erp-order:read')")
    @Operation(summary = "获取批次详情")
    public ApiResult<LabelBatchVO> getDetail(
            @Parameter(description = "批次ID") @PathVariable Long batchId) {
        log.info("[LABEL_BATCH] 查询批次详情: batchId={}", batchId);
        LabelBatchVO batchVO = labelService.getBatchVO(batchId);
        return ApiResult.ok(batchVO);
    }
}
