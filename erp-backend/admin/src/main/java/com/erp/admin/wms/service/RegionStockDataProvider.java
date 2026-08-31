package com.erp.admin.wms.service;

import com.erp.admin.wms.model.dto.RegionSkuStockDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 区域库存数据提供者
 * <p>
 * 封装区域维度的库存聚合查询，供库存预测模块使用。
 */
@Service
@RequiredArgsConstructor
public class RegionStockDataProvider {

    private final OwnerInventoryQueryService ownerInventoryQueryService;

    /**
     * 批量查询区域 SKU 库存聚合数据
     *
     * @param regionIds  区域 ID 集合（null 或空则查询所有区域）
     * @param skuKeyword SKU 关键字（可选）
     * @return 区域 SKU 库存聚合列表
     */
    public List<RegionSkuStockDTO> getRegionSkuStocks(Collection<Long> regionIds, String skuKeyword) {
        // 数据级可见性：按当前身份货主作用域过滤（平台=null 看全部；货主=自身；服务商=名下）
        return ownerInventoryQueryService.getRegionSkuStocks(regionIds, skuKeyword);
    }

    /**
     * 查询单个区域单个 SKU 的库存聚合
     */
    public RegionSkuStockDTO getRegionSkuStock(Long regionId, String skuCode) {
        RegionSkuStockDTO dto = ownerInventoryQueryService.getRegionSkuStocks(
                Collections.singleton(regionId), skuCode).stream()
                .filter(row -> skuCode.equals(row.getSkuCode())).findFirst().orElse(null);
        return dto != null ? dto : RegionSkuStockDTO.empty(regionId, skuCode);
    }

    /**
     * 批量查询区域预占数量
     *
     * @param regionIds 区域 ID 集合
     * @param skuCodes  SKU 编码集合
     * @return Map<"regionId:skuCode", reservedQuantity>
     */
    public Map<String, Integer> getRegionReservedMap(Set<Long> regionIds, Set<String> skuCodes) {
        if (regionIds.isEmpty() || skuCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<RegionSkuStockDTO> list = ownerInventoryQueryService.getRegionSkuStocks(regionIds, null);
        return list.stream()
                .filter(row -> skuCodes.contains(row.getSkuCode()))
                .collect(Collectors.toMap(
                row -> row.getRegionId() + ":" + row.getSkuCode(),
                row -> row.getTotalReserved() == null ? 0 : row.getTotalReserved(),
                (a, b) -> a
        ));
    }

    /**
     * 查询单个区域单个 SKU 的预占数量（区域汇总口径）。
     * <p>
     * 区域库存按货主隔离后，同一 (区域, SKU) 可能存在多个货主的记录，故用 selectList
     * 求和而非 selectOne——既避免 TooManyResults，又给出区域级合计；数据权限处理器会按
     * 查看者身份收窄（货主只看自己，平台看全部）。
     */
    public int getRegionReserved(Long regionId, String skuCode) {
        RegionSkuStockDTO stock = getRegionSkuStock(regionId, skuCode);
        return stock.getTotalReserved() == null ? 0 : stock.getTotalReserved();
    }
}
