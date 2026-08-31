package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.RegionInventoryMapper;
import com.erp.admin.wms.mapper.RegionMapper;
import com.erp.admin.wms.model.dto.WarehouseAggregateDTO;
import com.erp.admin.wms.model.entity.Region;
import com.erp.admin.wms.model.entity.RegionInventory;
import com.erp.admin.wms.model.vo.RegionInventoryStatsVO;
import com.erp.admin.wms.model.vo.RegionSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 区域库存服务
 */
@Slf4j
@Service
public class RegionInventoryService extends ExtendServiceImpl<RegionInventoryMapper, RegionInventory> {

    private final RegionMapper regionMapper;
    private final OwnerInventoryQueryService ownerInventoryQueryService;

    public RegionInventoryService(RegionMapper regionMapper, OwnerInventoryQueryService ownerInventoryQueryService) {
        this.regionMapper = regionMapper;
        this.ownerInventoryQueryService = ownerInventoryQueryService;
    }

    /**
     * 获取或创建区域库存记录（按货主隔离）。
     * <p>
     * 区域库存按 (货主, 区域, SKU) 唯一。erpTenantId 必填——平台作业上下文(-1)下无法
     * 从上下文推导货主，且数据权限处理器对平台不注入过滤，故必须由调用方（上游 facade
     * 从单据货主）显式传入，否则会把不同货主的同名 SKU 混算到一条记录。
     */
    public RegionInventory getOrCreate(Long erpTenantId, Long regionId, String skuCode) {
        if (erpTenantId == null) {
            throw new IllegalArgumentException("区域库存必须指定货主 erpTenantId，regionId=" + regionId + ", skuCode=" + skuCode);
        }
        RegionInventory inv = baseMapper.selectByTenantRegionSku(erpTenantId, regionId, skuCode);
        if (inv == null) {
            inv = new RegionInventory();
            inv.setErpTenantId(erpTenantId);
            inv.setRegionId(regionId);
            inv.setSkuCode(skuCode);
            inv.setReservedQuantity(0);
            inv.setInTransitQuantity(0);
            inv.setVersion(0);
            this.save(inv);
            log.info("创建区域库存记录: erpTenantId={}, regionId={}, skuCode={}", erpTenantId, regionId, skuCode);
        }
        return inv;
    }

    /**
     * 查询某货主在某区域某 SKU 的预占数量
     */
    public int getReservedQuantity(Long erpTenantId, Long regionId, String skuCode) {
        RegionInventory inv = baseMapper.selectByTenantRegionSku(erpTenantId, regionId, skuCode);
        return inv != null && inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0;
    }

    /**
     * 乐观锁更新预占数量
     * @return 是否成功
     */
    public boolean updateReservedWithVersion(Long id, Integer newReserved, Integer version) {
        return baseMapper.updateReservedWithVersion(id, newReserved, version) > 0;
    }

    /**
     * 乐观锁更新区域库存（预占 + 在途）
     * @return 是否成功
     */
    public boolean updateWithVersion(Long id, Integer newReserved, Integer newInTransit, Integer version) {
        return baseMapper.updateWithVersion(id, newReserved, newInTransit, version) > 0;
    }

    /**
     * 获取区域库存汇总列表（性能优化版：分步查询 + 内存组装）
     */
    public List<RegionSummaryVO> getRegionSummaryList() {
        // Step 1: 查询所有启用区域
        List<Region> regions = regionMapper.selectEnabledList();
        if (regions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> regionIds = regions.stream()
                .map(Region::getId)
                .collect(Collectors.toSet());

        // Step 2: 批量查询各区域的仓库库存聚合（单次查询）
        Map<Long, WarehouseAggregateDTO> warehouseAggMap = ownerInventoryQueryService
                .getRegionAggregates(regionIds)
                .stream()
                .collect(Collectors.toMap(
                        WarehouseAggregateDTO::getRegionId,
                        dto -> dto,
                        (a, b) -> a
                ));

        // Step 3: 内存组装。库存总览只使用 OWN 海外仓快照；平台订单不参与库存计算。
        return regions.stream().map(r -> {
            RegionSummaryVO vo = new RegionSummaryVO();
            vo.setRegionId(r.getId());
            vo.setRegionCode(r.getRegionCode());
            vo.setRegionName(r.getRegionName());

            WarehouseAggregateDTO agg = warehouseAggMap.getOrDefault(
                    r.getId(), WarehouseAggregateDTO.empty());
            vo.setOwnWarehouseCount(agg.getWarehouseCount());
            vo.setRegionAvailable(agg.getTotalAvailable());
            vo.setRegionReserved(agg.getTotalReserved());
            vo.setRegionInTransit(agg.getTotalInTransit());
            vo.setRegionDamaged(agg.getTotalDamaged());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取区域库存统计（全局汇总，性能优化版）
     */
    public RegionInventoryStatsVO getRegionStats() {
        List<RegionSummaryVO> list = getRegionSummaryList();

        RegionInventoryStatsVO stats = new RegionInventoryStatsVO();
        stats.setRegionCount(list.size());
        stats.setTotalRegionAvailable(list.stream()
                .mapToInt(RegionSummaryVO::getRegionAvailable).sum());
        stats.setTotalRegionReserved(list.stream()
                .mapToInt(RegionSummaryVO::getRegionReserved).sum());
        stats.setTotalRegionInTransit(list.stream()
                .mapToInt(RegionSummaryVO::getRegionInTransit).sum());
        stats.setTotalRegionDamaged(list.stream()
                .mapToInt(RegionSummaryVO::getRegionDamaged).sum());
        return stats;
    }
}
