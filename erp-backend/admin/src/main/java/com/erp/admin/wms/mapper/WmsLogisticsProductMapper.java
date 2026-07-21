package com.erp.admin.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.admin.wms.model.entity.WmsLogisticsProduct;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.Collection;
import java.util.List;

/**
 * 物流产品 Mapper。所有查询显式按 wms_tenant_id 收窄（服务商只见自己的产品）。
 *
 * @author erp
 */
public interface WmsLogisticsProductMapper extends ExtendMapper<WmsLogisticsProduct> {

    /** 服务商产品分页（名称/编码模糊 + 状态过滤） */
    default IPage<WmsLogisticsProduct> pageByTenant(PageParam pageParam, Long wmsTenantId, String keyword,
            Integer status) {
        LambdaQueryWrapperX<WmsLogisticsProduct> wrapper = WrappersX.lambdaQueryX(WmsLogisticsProduct.class)
            .eq(WmsLogisticsProduct::getWmsTenantId, wmsTenantId)
            .eqIfPresent(WmsLogisticsProduct::getStatus, status)
            .orderByDesc(WmsLogisticsProduct::getId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(WmsLogisticsProduct::getProductName, keyword)
                .or()
                .like(WmsLogisticsProduct::getProductCode, keyword));
        }
        IPage<WmsLogisticsProduct> page = this.prodPage(pageParam);
        return this.selectPage(page, wrapper);
    }

    /** 某服务商启用中的产品（货主建单选项用） */
    default List<WmsLogisticsProduct> listEnabledByTenant(Long wmsTenantId) {
        return this.selectList(WrappersX.lambdaQueryX(WmsLogisticsProduct.class)
            .eq(WmsLogisticsProduct::getWmsTenantId, wmsTenantId)
            .eq(WmsLogisticsProduct::getStatus, 1)
            .orderByAsc(WmsLogisticsProduct::getId));
    }

    /** 按 ID 集合批量取（收入页聚合补产品名/词条用；含已停用产品，历史流水仍可显示） */
    default List<WmsLogisticsProduct> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return this.selectList(WrappersX.lambdaQueryX(WmsLogisticsProduct.class)
            .in(WmsLogisticsProduct::getId, ids));
    }

}
