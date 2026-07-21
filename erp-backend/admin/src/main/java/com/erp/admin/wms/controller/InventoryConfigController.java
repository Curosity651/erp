package com.erp.admin.wms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.product.service.SkuBriefService;
import com.erp.admin.wms.mapper.InventoryConfigMapper;
import com.erp.admin.wms.model.converter.InventoryConfigConverter;
import com.erp.admin.wms.model.dto.GlobalInventoryConfigDTO;
import com.erp.admin.wms.model.dto.InventoryConfigDTO;
import com.erp.admin.wms.model.entity.InventoryConfig;
import com.erp.admin.wms.model.qo.InventoryConfigQO;
import com.erp.admin.wms.model.vo.GlobalInventoryConfigVO;
import com.erp.admin.wms.model.vo.InventoryConfigVO;
import com.erp.admin.wms.service.InventoryConfigService;
import com.erp.admin.wms.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.common.model.result.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存配置控制器
 *
 * @author erp
 */
@RestController
@RequestMapping("/wms/inventory-config")
@RequiredArgsConstructor
@Tag(name = "库存配置", description = "库存配置管理")
public class InventoryConfigController {

    private final InventoryConfigService inventoryConfigService;
    private final InventoryConfigMapper inventoryConfigMapper;
    private final RegionService regionService;
    private final SkuBriefService skuBriefService;

    // ========== 全局配置 ==========

    @GetMapping("/global")
    @Operation(summary = "获取全局配置")
    @PreAuthorize("@per.hasPermission('wms:inventory-config:read')")
    public ApiResult<GlobalInventoryConfigVO> getGlobalConfig() {
        return ApiResult.ok(inventoryConfigService.getGlobalConfig());
    }

    @PutMapping("/global")
    @Operation(summary = "保存全局配置")
    @PreAuthorize("@per.hasPermission('wms:inventory-config:edit')")
    public ApiResult<Void> saveGlobalConfig(@Validated @RequestBody GlobalInventoryConfigDTO dto) {
        inventoryConfigService.saveGlobalConfig(dto);
        return ApiResult.ok();
    }

    // ========== SKU 独立配置 ==========

    @GetMapping("/sku")
    @Operation(summary = "分页查询SKU配置")
    @PreAuthorize("@per.hasPermission('wms:inventory-config:read')")
    public ApiResult<PageResult<InventoryConfigVO>> pageSkuConfig(PageParam pageParam, InventoryConfigQO qo) {
        IPage<InventoryConfig> page = this.inventoryConfigMapper.queryPage(pageParam, qo);
        if (page.getRecords().isEmpty()) {
            return ApiResult.ok(new PageResult<>());
        }

        List<InventoryConfigVO> recordVOs = InventoryConfigConverter.INSTANCE.entityToVos(page.getRecords());

        // 填充 SKU 简要信息
        skuBriefService.enrichForQuery(
                recordVOs,
                InventoryConfigVO::getSkuCode,
                InventoryConfigVO::setSkuBrief
        );

        // 填充区域名称
        Set<Long> regionIds = recordVOs.stream()
                .map(InventoryConfigVO::getRegionId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (!regionIds.isEmpty()) {
            Map<Long, String> regionNameMap = regionService.getNameMapByIds(regionIds);
            recordVOs.forEach(vo ->
                    vo.setRegionName(regionNameMap.get(vo.getRegionId())));
        }

        // 填充预警阈值显示文本
        GlobalInventoryConfigVO globalConfig = inventoryConfigService.getGlobalConfig();
        recordVOs.forEach(vo ->
                vo.setNotifyThresholdDisplay(vo.getNotifyThresholdDays() != null
                        ? vo.getNotifyThresholdDays() + "天"
                        : "使用全局(" + globalConfig.getNotifyThresholdDays() + "天)"));

        // 分页响应数据
        PageResult<InventoryConfigVO> result = new PageResult<>(recordVOs, page.getTotal());
        return ApiResult.ok(result);
    }

    @PutMapping("/sku")
    @Operation(summary = "保存SKU配置")
    @PreAuthorize("@per.hasPermission('wms:inventory-config:edit')")
    public ApiResult<Void> saveSkuConfig(@Validated @RequestBody InventoryConfigDTO dto) {
        inventoryConfigService.saveSkuConfig(dto);
        return ApiResult.ok();
    }

    @DeleteMapping("/sku")
    @Operation(summary = "删除SKU配置")
    @PreAuthorize("@per.hasPermission('wms:inventory-config:edit')")
    public ApiResult<Void> deleteSkuConfig(@RequestParam Long id) {
        inventoryConfigService.deleteSkuConfig(id);
        return ApiResult.ok();
    }
}
