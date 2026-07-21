package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.dto.PurchaseUnshippedBatchDTO;
import com.erp.admin.wms.model.dto.SkuDailySalesDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 发货生产测算取数 Mapper（只读聚合）。
 * <p>
 * 租户隔离由拦截器自动注入：{@code erp_order} 走 tenant_id 行级隔离，
 * {@code wms_purchase_order} 走 erp_tenant_id 货主隔离，均无需显式传租户。
 *
 * @author erp
 */
public interface ShipProdCalcMapper {

    /**
     * FBS 逐日销量（全平台汇总，按 SKU × 日期聚合）。
     * @param skuCodes  SKU 编码集合（空=全部）
     * @param startTime 起始时间（含）
     * @param endTime   结束时间（不含）
     * @return 逐日销量行
     */
    List<SkuDailySalesDTO> selectFbsDailySales(@Param("skuCodes") Collection<String> skuCodes,
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 采购单未发货批次（状态 ∈ 已确认/生产中，未发货量&gt;0），映射为 E 在制批次。
     * @param skuCodes SKU 编码集合（空=全部）
     * @return 未发货批次行
     */
    List<PurchaseUnshippedBatchDTO> selectPurchaseUnshippedBatches(
            @Param("skuCodes") Collection<String> skuCodes);
}
