package com.erp.admin.wms.controller;

import com.erp.admin.wms.facade.ShipProdCalcFacade;
import com.erp.admin.wms.model.qo.ShipProdCalcQO;
import com.erp.admin.wms.model.vo.ShipProdCalcDetailVO;
import com.erp.admin.wms.model.vo.ShipProdCalcSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 发货生产测算 Controller（货主端）。按 model_core.py 口径：35 天发货 / 80 天生产 / 45 天备货。
 *
 * @author erp
 */
@Tag(name = "发货生产测算")
@RestController
@RequestMapping("/wms/ship-prod-calc")
@RequiredArgsConstructor
@PreAuthorize("@per.hasPermission('wms:ship-prod-calc:read')")
public class ShipProdCalcController {

    private final ShipProdCalcFacade shipProdCalcFacade;

    @Operation(summary = "测算汇总（分页 + 状态卡计数）")
    @GetMapping("/summary")
    public ApiResult<ShipProdCalcSummaryVO> getSummary(PageParam pageParam, ShipProdCalcQO qo) {
        return ApiResult.ok(shipProdCalcFacade.getSummary(pageParam, qo));
    }

    @Operation(summary = "单 SKU 测算详情（数据链）")
    @GetMapping("/detail")
    public ApiResult<ShipProdCalcDetailVO> getDetail(@RequestParam String skuCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
        return ApiResult.ok(shipProdCalcFacade.getDetail(skuCode, baseDate));
    }
}
