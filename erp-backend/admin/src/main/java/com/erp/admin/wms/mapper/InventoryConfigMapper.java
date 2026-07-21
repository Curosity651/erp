package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.InventoryConfig;
import com.erp.admin.wms.model.qo.InventoryConfigQO;
import org.apache.ibatis.annotations.Mapper;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.List;

/**
 * 库存配置 Mapper
 *
 * @author erp
 */
@Mapper
public interface InventoryConfigMapper extends ExtendMapper<InventoryConfig> {

    /**
     * 分页查询 SKU 配置（区域维度）
     */
    default IPage<InventoryConfig> queryPage(PageParam pageParam, InventoryConfigQO qo) {
        LambdaQueryWrapperX<InventoryConfig> wrapper = WrappersX.lambdaQueryX(InventoryConfig.class)
                .eqIfPresent(InventoryConfig::getRegionId, qo.getRegionId())
                .likeIfPresent(InventoryConfig::getSkuCode, qo.getSkuKeyword())
                .orderByDesc(InventoryConfig::getId);
		IPage<InventoryConfig> page = PageUtil.prodPage(pageParam);
		return this.selectPage(page, wrapper);
    }

    /**
     * 按 (货主, 区域, SKU) 三键查询配置（uk_tenant_region_sku 唯一命中）。
     * <p>
     * 显式带 erp_tenant_id：不依赖数据权限 handler（后台任务无上下文时 handler 不过滤，
     * 旧两键版本会静默拿到别的货主的配置）。erpTenantId 为空时按 -1 兜底 → 命中 0 行，
     * 上层回退全局默认值，fail-closed。
     */
    default InventoryConfig selectByRegionAndSku(Long erpTenantId, Long regionId, String skuCode) {
        LambdaQueryWrapperX<InventoryConfig> wrapper = WrappersX.lambdaQueryX(InventoryConfig.class)
                .eq(InventoryConfig::getErpTenantId, erpTenantId == null ? -1L : erpTenantId)
                .eq(InventoryConfig::getRegionId, regionId)
                .eq(InventoryConfig::getSkuCode, skuCode)
                .orderByAsc(InventoryConfig::getId);
        return this.selectList(wrapper).stream().findFirst().orElse(null);
    }

    /**
     * 根据区域ID和SKU编码列表批量查询配置
     */
    default List<InventoryConfig> selectByRegionAndSkuCodes(Long regionId, Collection<String> skuCodes) {
        LambdaQueryWrapperX<InventoryConfig> wrapper = WrappersX.lambdaQueryX(InventoryConfig.class)
                .eq(InventoryConfig::getRegionId, regionId)
                .in(InventoryConfig::getSkuCode, skuCodes);
        return this.selectList(wrapper);
    }
}
