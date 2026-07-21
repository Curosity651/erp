package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.qo.StockPostingItemQO;
import com.erp.admin.wms.model.qo.StockPostingQO;
import com.erp.admin.wms.model.vo.StockPostingDetailVO;
import com.erp.admin.wms.model.vo.StockPostingItemVO;
import com.erp.admin.wms.model.vo.StockPostingPageVO;
import com.erp.admin.wms.service.StockPostingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 库存过账单查询控制器
 *
 * @author erp
 */
@Tag(name = "库存过账单查询")
@RestController
@RequestMapping("/wms/stock-posting")
@RequiredArgsConstructor
public class StockPostingQueryController {

    private final StockPostingQueryService stockPostingQueryService;

    @Operation(summary = "过账单分页查询")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('wms:stock-flow:read')")
    public ApiResult<PageResult<StockPostingPageVO>> queryPage(PageParam pageParam, StockPostingQO qo) {
        return ApiResult.ok(stockPostingQueryService.queryPage(pageParam, qo));
    }

    @Operation(summary = "过账单详情（含明细）")
    @GetMapping("/detail")
    @PreAuthorize("hasAuthority('wms:stock-flow:read')")
    public ApiResult<StockPostingDetailVO> getDetail(@RequestParam Long id) {
        return ApiResult.ok(stockPostingQueryService.getDetail(id));
    }

    @Operation(summary = "过账单明细分页查询")
    @GetMapping("/item/page")
    @PreAuthorize("hasAuthority('wms:stock-flow:read')")
    public ApiResult<PageResult<StockPostingItemVO>> queryItemPage(PageParam pageParam, @Valid StockPostingItemQO qo) {
        return ApiResult.ok(stockPostingQueryService.queryItemPage(pageParam, qo));
    }

}
