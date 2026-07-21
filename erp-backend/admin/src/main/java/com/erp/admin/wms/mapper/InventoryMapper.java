package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.model.dto.RegionSkuStockDTO;
import com.erp.admin.wms.model.entity.Inventory;
import com.erp.admin.wms.model.qo.InventoryQO;
import com.erp.admin.wms.model.dto.WarehouseAggregateDTO;
import com.erp.admin.wms.model.vo.AvailableStockVO;
import com.erp.admin.wms.model.vo.InventorySummaryVO;
import com.erp.admin.wms.model.vo.SkuSummaryVO;
import com.erp.admin.wms.model.vo.WarehouseSummaryVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 库存 Mapper
 *
 * @author erp
 */
public interface InventoryMapper extends ExtendMapper<Inventory> {

    // ==================== 查询方法（default 实现）====================

    /**
     * 根据仓库ID和SKU编码查询库存
     */
    default Inventory selectByWarehouseAndSku(Long warehouseId, String skuCode) {
        return selectOne(WrappersX.<Inventory>lambdaQueryX()
                .eq(Inventory::getWarehouseId, warehouseId)
                .eq(Inventory::getSkuCode, skuCode));
    }

    /**
     * 按聚合键（货主×服务商×仓×SKU）查询聚合快照（方案②降级后唯一键）。
     */
    default Inventory selectByTenantKey(Long wmsTenantId, Long erpTenantId, Long warehouseId, String skuCode) {
        return selectOne(WrappersX.<Inventory>lambdaQueryX()
                .eq(Inventory::getWmsTenantId, wmsTenantId)
                .eq(Inventory::getErpTenantId, erpTenantId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .eq(Inventory::getSkuCode, skuCode));
    }

    /**
     * 按 (货主, 仓, SKU) 定位聚合快照——供过账引擎按货主命中/新建库存行。
     * <p>
     * 一个货主在一个仓的货固定落在其父服务商的货架上（1 货主→1 父服务商），故 (erp,仓,SKU)
     * 唯一；用 selectList + findFirst 而非 selectOne，忽略 wms_tenant_id 维度，既能命中聚合器
     * 建的行（无论其 wms 值），又避免多行时抛 TooManyResults。
     */
    default Inventory selectByErpWarehouseSku(Long erpTenantId, Long warehouseId, String skuCode) {
        return selectList(WrappersX.<Inventory>lambdaQueryX()
                .eq(Inventory::getErpTenantId, erpTenantId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .eq(Inventory::getSkuCode, skuCode)
                .orderByAsc(Inventory::getId))
                .stream().findFirst().orElse(null);
    }

    /**
     * 按 WMS 服务商查询其名下所有货主的库存聚合快照（服务商只读汇总，数据级可见性 ③）。
     * <p>C3：M-1 后 {@code wms_inventory.wms_tenant_id} 恒为 0，不能再按 wms 维过滤（原 selectByWmsTenant 恒空）。
     * 改经 {@code sys_tenant.parent_wms_tenant_id = 当前服务商} 圈出其名下全部货主，再按 {@code erp_tenant_id}
     * （M-1 后仍准确）取这些货主的库存行。{@code wms_inventory}/{@code sys_tenant} 均不在租户注入/数据权限白名单，
     * 此 JOIN 不被自动过滤。
     */
    @Select("SELECT i.* FROM wms_inventory i "
            + "INNER JOIN sys_tenant t ON t.id = i.erp_tenant_id AND t.parent_wms_tenant_id = #{wmsTenantId} "
            + "ORDER BY i.id DESC")
    List<Inventory> selectByOperatorOwnership(@Param("wmsTenantId") Long wmsTenantId);

    /**
     * 根据仓库ID查询所有有库存的记录（库存>0）
     */
    default List<Inventory> selectByWarehouseId(Long warehouseId) {
        return selectList(WrappersX.<Inventory>lambdaQueryX()
                .eq(Inventory::getWarehouseId, warehouseId)
                .and(w -> w.gt(Inventory::getAvailableQuantity, 0)
                        .or().gt(Inventory::getReservedQuantity, 0)
                        .or().gt(Inventory::getInTransitQuantity, 0)));
    }

    /**
     * 根据仓库ID查询所有有记录的库存（含数量为0的）
     * 用于盘点时需要覆盖所有曾经有库存记录的SKU
     */
    default List<Inventory> selectAllByWarehouseId(Long warehouseId) {
        return selectList(WrappersX.<Inventory>lambdaQueryX()
                .eq(Inventory::getWarehouseId, warehouseId));
    }

    /**
     * 根据仓库ID和SKU编码列表查询库存
     */
    default List<Inventory> selectByWarehouseAndSkuCodes(Long warehouseId, List<String> skuCodes) {
        return selectList(WrappersX.<Inventory>lambdaQueryX()
                .eq(Inventory::getWarehouseId, warehouseId)
                .in(Inventory::getSkuCode, skuCodes));
    }

    /**
     * 按 (货主, 仓, SKU 列表) 批量查询库存——供出库/校验按货主取可用量，避免多货主同仓同 SKU
     * 被 toMap 折叠成任意一行。
     */
    default List<Inventory> selectByErpWarehouseAndSkuCodes(Long erpTenantId, Long warehouseId, List<String> skuCodes) {
        return selectList(WrappersX.<Inventory>lambdaQueryX()
                .eq(Inventory::getErpTenantId, erpTenantId)
                .eq(Inventory::getWarehouseId, warehouseId)
                .in(Inventory::getSkuCode, skuCodes));
    }

    /**
     * 乐观锁更新库存（使用 Wrapper 指定 set 字段）
     *
     * @param id 库存记录ID
     * @param availableQuantity 新的可用库存
     * @param reservedQuantity 新的占用库存
     * @param inTransitQuantity 新的在途库存
     * @param damagedQuantity 新的残品库存
     * @param oldVersion 原版本号（乐观锁条件）
     * @return 影响行数（0表示版本冲突）
     */
    default int updateWithVersion(Long id, Integer availableQuantity, Integer reservedQuantity,
                                  Integer inTransitQuantity, Integer damagedQuantity, Integer oldVersion) {
        return update(null, Wrappers.<Inventory>lambdaUpdate()
                .set(Inventory::getAvailableQuantity, availableQuantity)
                .set(Inventory::getReservedQuantity, reservedQuantity)
                .set(Inventory::getInTransitQuantity, inTransitQuantity)
                .set(Inventory::getDamagedQuantity, damagedQuantity)
                .set(Inventory::getVersion, oldVersion + 1)
                .setSql("update_time = NOW()")
                .eq(Inventory::getId, id)
                .eq(Inventory::getVersion, oldVersion));
    }

    // ==================== 分页查询（XML 实现，含 JOIN）====================

    /**
     * 库存明细分页查询（含仓库类型筛选，需 JOIN wms_warehouse）
     */
    IPage<Inventory> queryPage(IPage<Inventory> page, @Param("qo") InventoryQO qo);

    // ==================== 统计查询（XML 实现，含聚合/JOIN）====================

    /**
     * 获取库存汇总统计（按货主作用域）
     */
    InventorySummaryVO selectSummary(@Param("erpTenantId") Long erpTenantId);

    /**
     * 按仓库汇总（按货主作用域）
     */
    List<WarehouseSummaryVO> selectSummaryByWarehouse(@Param("warehouseType") String warehouseType,
            @Param("erpTenantId") Long erpTenantId);

    /**
     * 按SKU汇总分页（按货主作用域）
     */
    IPage<SkuSummaryVO> selectSummaryBySku(IPage<SkuSummaryVO> page, @Param("keyword") String keyword,
            @Param("stockStatus") String stockStatus, @Param("erpTenantId") Long erpTenantId);

    /**
     * 分页查询可调拨库存
     */
    IPage<AvailableStockVO> selectAvailableStockPage(IPage<AvailableStockVO> page, @Param("warehouseId") Long warehouseId, @Param("keyword") String keyword);

    /**
     * 根据仓库ID集合和SKU关键词查询库存
     * <p>
     * 用于库存预测汇总查询
     *
     * @param warehouseIds 仓库ID集合
     * @param skuKeyword SKU关键词（模糊匹配）
     * @return 库存列表
     */
    default List<Inventory> selectByWarehousesAndKeyword(Set<Long> warehouseIds, String skuKeyword) {
        return selectList(WrappersX.<Inventory>lambdaQueryX()
                .in(Inventory::getWarehouseId, warehouseIds)
                .likeIfPresent(Inventory::getSkuCode, skuKeyword));
    }

    /**
     * 按区域ID批量查询仓库库存聚合（仅自有仓）。
     * @param erpTenantIds 货主作用域（空=不过滤，平台看全部；否则按 erp_tenant_id IN 收窄）
     */
    List<WarehouseAggregateDTO> selectAggregateByRegionIds(@Param("regionIds") Collection<Long> regionIds,
            @Param("erpTenantIds") Collection<Long> erpTenantIds);

    /**
     * 按区域和 SKU 聚合库存数据（仅自有仓）。
     *
     * @param regionIds  区域 ID 集合（null 或空则查询所有区域）
     * @param skuKeyword SKU 关键字（可选）
     * @param erpTenantIds 货主作用域（空=不过滤）
     * @return 区域 SKU 库存聚合列表
     */
    List<RegionSkuStockDTO> selectAggregateByRegionsAndKeyword(
            @Param("regionIds") Collection<Long> regionIds,
            @Param("skuKeyword") String skuKeyword,
            @Param("erpTenantIds") Collection<Long> erpTenantIds);

    /**
     * 查询单个区域单个 SKU 的库存聚合。
     * @param erpTenantIds 货主作用域（空=不过滤）
     */
    RegionSkuStockDTO selectAggregateByRegionAndSku(
            @Param("regionId") Long regionId,
            @Param("skuCode") String skuCode,
            @Param("erpTenantIds") Collection<Long> erpTenantIds);

}
