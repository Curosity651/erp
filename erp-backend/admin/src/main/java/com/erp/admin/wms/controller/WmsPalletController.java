package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.PalletCapacityDTO;
import com.erp.admin.wms.model.entity.WmsPallet;
import com.erp.admin.wms.model.vo.PalletSlotVO;
import com.erp.admin.wms.model.vo.PalletSummaryVO;
import com.erp.admin.wms.config.WmsCoreModeGuard;
import com.erp.admin.wms.service.WmsPalletService;
import com.erp.admin.wms.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Pallet management")
@RestController
@RequestMapping("/wms/pallets")
@RequiredArgsConstructor
public class WmsPalletController {

    private final WmsPalletService palletService;

    private final WarehouseService warehouseService;

    private final WmsCoreModeGuard coreModeGuard;

    @GetMapping
    @PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
    public ApiResult<List<PalletSummaryVO>> list(@RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long erpTenantId,
            @RequestParam(required = false) Long wmsTenantId,
            @RequestParam(required = false) String palletNo,
            @RequestParam(required = false) String slotCode,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(palletService.listPallets(warehouseId, erpTenantId, wmsTenantId,
                palletNo, slotCode, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
    public ApiResult<PalletSummaryVO> detail(@PathVariable Long id) {
        return ApiResult.ok(palletService.getDetail(id));
    }

    @GetMapping("/slots")
    @PreAuthorize("@per.hasPermission('wms:warehouse:edit') or hasAuthority('wms:inbound-exec:oper')")
    public ApiResult<List<PalletSlotVO>> slots(@RequestParam Long warehouseId) {
        warehouseService.validateOperableOwnWarehouse(warehouseId);
        return ApiResult.ok(palletService.listSlots(warehouseId));
    }

    @PatchMapping("/capacity")
    @PreAuthorize("hasAuthority('wms:inbound-exec:oper')")
    public ApiResult<WmsPallet> calibrate(@Validated @RequestBody PalletCapacityDTO dto) {
        coreModeGuard.assertLegacyWriteAllowed("托盘容量修改");
        return ApiResult.ok(palletService.calibrate(dto));
    }
}
