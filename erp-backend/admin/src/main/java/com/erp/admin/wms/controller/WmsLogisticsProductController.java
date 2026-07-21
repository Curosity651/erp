package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.LogisticsProductDTO;
import com.erp.admin.wms.model.vo.LogisticsProductVO;
import com.erp.admin.wms.service.WmsLogisticsProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WMS 服务商物流产品控制器。管理端仅服务商（service 内身份校验，与货主管理页同口径不设权限码）；
 * /options 供货主建单拉取父服务商启用产品。
 *
 * @author erp
 */
@Tag(name = "服务商物流产品")
@RestController
@RequestMapping("/wms/logistics-product")
@RequiredArgsConstructor
public class WmsLogisticsProductController {

    private final WmsLogisticsProductService logisticsProductService;

    @Operation(summary = "产品分页(服务商)")
    @GetMapping("/page")
    @PreAuthorize("@per.hasPermission('wms:logistics-product:read')")
    public ApiResult<PageResult<LogisticsProductVO>> page(PageParam pageParam,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) Integer status) {
        return ApiResult.ok(logisticsProductService.page(pageParam, keyword, status));
    }

    @Operation(summary = "新建/编辑产品(服务商)")
    @PostMapping
    @PreAuthorize("@per.hasPermission('wms:logistics-product:add')")
    public ApiResult<Void> save(@Validated @RequestBody LogisticsProductDTO dto) {
        logisticsProductService.saveProduct(dto);
        return ApiResult.ok();
    }

    @Operation(summary = "启用/停用产品(服务商)")
    @PutMapping("/{id}/status")
    @PreAuthorize("@per.hasPermission('wms:logistics-product:edit')")
    public ApiResult<Void> updateStatus(@PathVariable("id") Long id, @RequestParam Integer status) {
        logisticsProductService.updateStatus(id, status);
        return ApiResult.ok();
    }

    @Operation(summary = "删除产品(服务商)")
    @DeleteMapping("/{id}")
    @PreAuthorize("@per.hasPermission('wms:logistics-product:del')")
    public ApiResult<Void> delete(@PathVariable("id") Long id) {
        logisticsProductService.deleteProduct(id);
        return ApiResult.ok();
    }

    // 注意：/options 由【货主】建单时调用（拉父服务商启用产品），故【不设服务商权限码】——
    // 否则货主(无 wms:logistics-product:* 码)会被 403 挡住建单；service.listOptionsForOwner 对非货主返回空列表兜底。
    @Operation(summary = "货主可选产品(父服务商启用中)")
    @GetMapping("/options")
    public ApiResult<List<LogisticsProductVO>> options() {
        return ApiResult.ok(logisticsProductService.listOptionsForOwner());
    }

}
