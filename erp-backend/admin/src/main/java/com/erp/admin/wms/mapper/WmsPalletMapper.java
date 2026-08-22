package com.erp.admin.wms.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.erp.admin.wms.model.entity.WmsPallet;
import com.erp.admin.wms.model.vo.PutawayReceiptLineVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

@Mapper
public interface WmsPalletMapper extends ExtendMapper<WmsPallet> {
    @Select("SELECT * FROM wms_pallet WHERE id = #{id} FOR UPDATE")
    WmsPallet selectForUpdate(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM wms_pallet "
            + "WHERE warehouse_id = #{warehouseId} AND current_slot_id IS NOT NULL "
            + "AND actual_weight_kg IS NOT NULL AND actual_weight_kg > #{maxWeightKg}")
    long countOverweightByWarehouse(@Param("warehouseId") Long warehouseId,
            @Param("maxWeightKg") BigDecimal maxWeightKg);

    @Select("SELECT DISTINCT p.* FROM wms_pallet p "
            + "INNER JOIN wms_physical_inventory pi ON pi.pallet_id = p.id "
            + "INNER JOIN wms_purchase_inbound_order_item item ON item.id = pi.inbound_item_id "
            + "WHERE item.inbound_order_id = #{inboundOrderId} "
            + "ORDER BY p.slot_code, p.pallet_no")
    List<WmsPallet> selectByInboundOrderId(@Param("inboundOrderId") Long inboundOrderId);

    @Select("SELECT p.id AS pallet_id, p.pallet_no, p.slot_code, "
            + "pi.erp_tenant_id, pi.sku_code, pi.quality, SUM(pi.quantity) AS quantity "
            + "FROM wms_physical_inventory pi "
            + "INNER JOIN wms_pallet p ON p.id = pi.pallet_id "
            + "INNER JOIN wms_purchase_inbound_order_item item ON item.id = pi.inbound_item_id "
            + "WHERE item.inbound_order_id = #{inboundOrderId} "
            + "GROUP BY p.id, p.pallet_no, p.slot_code, pi.erp_tenant_id, pi.sku_code, pi.quality "
            + "ORDER BY p.slot_code, p.pallet_no, pi.sku_code")
    List<PutawayReceiptLineVO> selectReceiptLinesByInboundOrderId(
            @Param("inboundOrderId") Long inboundOrderId);

    default long countActiveByWarehouse(Long warehouseId) {
        return selectCount(WrappersX.lambdaQueryX(WmsPallet.class)
                .eq(WmsPallet::getWarehouseId, warehouseId)
                .isNotNull(WmsPallet::getCurrentSlotId)
                .ne(WmsPallet::getPalletStatus, "CLOSED"));
    }
}
