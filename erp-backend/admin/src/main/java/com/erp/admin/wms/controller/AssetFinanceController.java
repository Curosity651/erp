package com.erp.admin.wms.controller;

import java.util.List;

import com.erp.admin.wms.facade.AssetFinanceFacade;
import com.erp.admin.wms.model.vo.AssetFinanceOverviewVO;
import com.erp.admin.wms.model.vo.AssetLogisticsRowVO;
import com.erp.admin.wms.model.vo.AssetOverviewVO;
import com.erp.admin.wms.model.vo.AssetProcurementRowVO;
import com.erp.admin.wms.model.vo.PayableProviderVO;
import com.erp.admin.wms.model.vo.PayablesOverviewVO;
import com.erp.admin.wms.model.vo.PayableSupplierVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 货主资产与账务 Controller。资产=采购成本+物流附加（加权平均、分币种不折算）；
 * 账务=应付供应商（采购单预付/尾款）、应付物流商（物流单）。
 *
 * @author erp
 */
@Tag(name = "资产与账务")
@RestController
@RequestMapping("/wms/asset-finance")
@RequiredArgsConstructor
@PreAuthorize("@per.hasPermission('wms:asset-finance:read')")
public class AssetFinanceController {

    private final AssetFinanceFacade assetFinanceFacade;

    @Operation(summary = "总览（资产分币种 + 应付供应商/物流商）")
    @GetMapping("/overview")
    public ApiResult<AssetFinanceOverviewVO> getOverview() {
        return ApiResult.ok(assetFinanceFacade.getOverview());
    }

    @Operation(summary = "资产总览（库存位置、采购成本、物流资本化）")
    @GetMapping("/assets-overview")
    public ApiResult<AssetOverviewVO> getAssetsOverview() {
        return ApiResult.ok(assetFinanceFacade.getAssetsOverview());
    }

    @Operation(summary = "应付总览（供应商、国内物流商）")
    @GetMapping("/payables-overview")
    public ApiResult<PayablesOverviewVO> getPayablesOverview() {
        return ApiResult.ok(assetFinanceFacade.getPayablesOverview());
    }

    @Operation(summary = "采购成本明细（按 SKU × 币种）")
    @GetMapping("/procurement-detail")
    public ApiResult<List<AssetProcurementRowVO>> getProcurementDetail() {
        return ApiResult.ok(assetFinanceFacade.getProcurementDetail());
    }

    @Operation(summary = "物流附加成本明细（按 SKU，USD）")
    @GetMapping("/logistics-detail")
    public ApiResult<List<AssetLogisticsRowVO>> getLogisticsDetail() {
        return ApiResult.ok(assetFinanceFacade.getLogisticsDetail());
    }

    @Operation(summary = "应付供应商（汇总 + 下钻采购单）")
    @GetMapping("/payable-supplier")
    public ApiResult<List<PayableSupplierVO>> getPayableSupplier() {
        return ApiResult.ok(assetFinanceFacade.getPayableSupplier());
    }

    @Operation(summary = "应付物流商（汇总 + 下钻物流单）")
    @GetMapping("/payable-provider")
    public ApiResult<List<PayableProviderVO>> getPayableProvider() {
        return ApiResult.ok(assetFinanceFacade.getPayableProvider());
    }
}
