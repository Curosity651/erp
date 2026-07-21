package com.erp.admin.wms.controller;

import com.erp.admin.wms.model.dto.PackDTO;
import com.erp.admin.wms.model.dto.ShipDTO;
import com.erp.admin.wms.model.qo.PackShipQO;
import com.erp.admin.wms.model.vo.LogisticsChannelVO;
import com.erp.admin.wms.model.vo.PackShipOrderVO;
import com.erp.admin.wms.model.vo.ShipResultVO;
import com.erp.admin.wms.service.OutboundShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 海外仓平台出库作业·打包签出控制器（业务需求 1.3.2）。仅平台身份（service 内二次校验）。
 *
 * @author erp
 */
@Tag(name = "出库作业·打包签出")
@RestController
@RequestMapping("/wms/outbound-shipping")
@RequiredArgsConstructor
public class OutboundShippingController {

    private final OutboundShippingService outboundShippingService;

    @Operation(summary = "分页 拣货中/已打包/已发货 订单")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<PageResult<PackShipOrderVO>> page(PageParam pageParam, PackShipQO qo) {
        return ApiResult.ok(outboundShippingService.page(pageParam, qo));
    }

    @Operation(summary = "订单详情(含明细)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<PackShipOrderVO> detail(@PathVariable("id") Long id) {
        return ApiResult.ok(outboundShippingService.getDetail(id));
    }

    @Operation(summary = "打包 PICKING→PACKED")
    @PostMapping("/pack")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<Void> pack(@Validated @RequestBody PackDTO dto) {
        outboundShippingService.pack(dto);
        return ApiResult.ok();
    }

    @Operation(summary = "签出 PACKED→SHIPPED(扣库存+释放锁定+计费)")
    @PostMapping("/ship")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<ShipResultVO> ship(@Validated @RequestBody ShipDTO dto) {
        return ApiResult.ok(outboundShippingService.ship(dto));
    }

    @Operation(summary = "物流渠道选项")
    @GetMapping("/channels")
    @PreAuthorize("hasAuthority('wms:outbound-exec:oper')")
    public ApiResult<List<LogisticsChannelVO>> channels() {
        return ApiResult.ok(outboundShippingService.listChannels());
    }

}
