package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.model.dto.RegionInTransitDTO;
import com.erp.admin.wms.model.dto.RegionReservedDTO;
import com.erp.admin.wms.model.entity.RegionInventory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface RegionInventoryMapper extends ExtendMapper<RegionInventory> {

    default RegionInventory selectByRegionAndSku(Long regionId, String skuCode) {
        LambdaQueryWrapperX<RegionInventory> wrapper = WrappersX.lambdaQueryX(RegionInventory.class)
                .eq(RegionInventory::getRegionId, regionId)
                .eq(RegionInventory::getSkuCode, skuCode);
        return this.selectOne(wrapper);
    }

    /**
     * 按 (货主, 区域, SKU) 精确查询区域库存——区域库存按货主隔离的唯一键
     * (uk_tenant_region_sku)。必须显式传 erpTenantId：平台作业上下文(-1)下
     * 数据权限处理器不注入过滤，靠此显式条件命中唯一行，避免 selectOne 抛
     * TooManyResults。
     */
    default RegionInventory selectByTenantRegionSku(Long erpTenantId, Long regionId, String skuCode) {
        LambdaQueryWrapperX<RegionInventory> wrapper = WrappersX.lambdaQueryX(RegionInventory.class)
                .eq(RegionInventory::getErpTenantId, erpTenantId)
                .eq(RegionInventory::getRegionId, regionId)
                .eq(RegionInventory::getSkuCode, skuCode);
        return this.selectOne(wrapper);
    }

    /**
     * 批量查询区域库存
     */
    default List<RegionInventory> selectByRegionIdsAndSkuCodes(
            Collection<Long> regionIds,
            Collection<String> skuCodes) {
        if (regionIds == null || regionIds.isEmpty() || skuCodes == null || skuCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(Wrappers.<RegionInventory>lambdaQuery()
                .in(RegionInventory::getRegionId, regionIds)
                .in(RegionInventory::getSkuCode, skuCodes));
    }

    /**
     * 乐观锁更新预占数量
     * @return 受影响行数（0 表示版本冲突或约束不满足）
     */
    @Update("UPDATE wms_region_inventory " +
            "SET reserved_quantity = #{newReserved}, " +
            "    version = version + 1, " +
            "    update_time = NOW() " +
            "WHERE id = #{id} " +
            "  AND version = #{version} " +
            "  AND #{newReserved} >= 0")
    int updateReservedWithVersion(@Param("id") Long id,
                                  @Param("newReserved") Integer newReserved,
                                  @Param("version") Integer version);

    /**
     * 乐观锁更新区域库存（预占 + 在途）
     */
    @Update("UPDATE wms_region_inventory " +
            "SET reserved_quantity = #{newReserved}, " +
            "    in_transit_quantity = #{newInTransit}, " +
            "    version = version + 1, " +
            "    update_time = NOW() " +
            "WHERE id = #{id} " +
            "  AND version = #{version} " +
            "  AND #{newReserved} >= 0 " +
            "  AND #{newInTransit} >= 0")
    int updateWithVersion(@Param("id") Long id,
                          @Param("newReserved") Integer newReserved,
                          @Param("newInTransit") Integer newInTransit,
                          @Param("version") Integer version);

    /**
     * 按区域ID批量查询预占汇总（类型安全）。
     * <p>M-3 加固：显式传货主作用域 {@code erpTenantIds}（{@code null}=平台看全部；非空=IN 过滤；
     * 空作用域由 readScope 给占位 -999 命中 0 行），不再单靠数据权限拦截器自动注入 erp_tenant_id 兜底。
     */
    List<RegionReservedDTO> selectReservedByRegionIds(@Param("regionIds") Collection<Long> regionIds,
            @Param("erpTenantIds") Collection<Long> erpTenantIds);

    /**
     * 按区域ID批量查询区域级在途汇总。
     * <p>M-3 加固：同上，显式传 {@code erpTenantIds} 作用域过滤。
     */
    @Select("<script>" +
            "SELECT region_id AS regionId, SUM(in_transit_quantity) AS totalInTransit " +
            "FROM wms_region_inventory " +
            "WHERE region_id IN " +
            "<foreach collection='regionIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "<if test='erpTenantIds != null and erpTenantIds.size() > 0'>" +
            "AND erp_tenant_id IN " +
            "<foreach collection='erpTenantIds' item='e' open='(' separator=',' close=')'>#{e}</foreach> " +
            "</if>" +
            "GROUP BY region_id" +
            "</script>")
    List<RegionInTransitDTO> selectInTransitByRegionIds(@Param("regionIds") Collection<Long> regionIds,
            @Param("erpTenantIds") Collection<Long> erpTenantIds);
}
