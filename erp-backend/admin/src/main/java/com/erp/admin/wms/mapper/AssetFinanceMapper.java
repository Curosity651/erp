package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.dto.PayableProviderRowDTO;
import com.erp.admin.wms.model.dto.PayableSupplierRowDTO;
import com.erp.admin.wms.model.dto.PurchaseCostAggDTO;
import com.erp.admin.wms.model.dto.PurchaseUnshippedBatchDTO;
import com.erp.admin.wms.model.dto.ShippingCostLineDTO;
import com.erp.admin.wms.model.dto.AssetLocationStockDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 货主资产与账务取数 Mapper（只读聚合）。
 * <p>
 * WMS 表不经过 ERP tenant_id 自动注入，所有查询必须显式传入并过滤 erpTenantId。
 *
 * @author erp
 */
public interface AssetFinanceMapper {

	/** 当前货主的逻辑库位库存资产桶。 */
	List<AssetLocationStockDTO> selectLocationHeldQuantity(@Param("erpTenantId") Long erpTenantId);

    /** 采购成本聚合（按 SKU × 币种）：非草稿非取消采购单，用于加权平均采购单价。 */
    List<PurchaseCostAggDTO> selectPurchaseCostAgg(@Param("erpTenantId") Long erpTenantId);

	/** 当前货主已确认采购单中尚未发货的数量。 */
	List<PurchaseUnshippedBatchDTO> selectPurchaseUnshippedBatches(@Param("erpTenantId") Long erpTenantId);

    /** 应付供应商原始行（每采购单一行，含供应商名/币种/预付尾款状态）：非草稿非取消。 */
    List<PayableSupplierRowDTO> selectPayableSupplierRows(@Param("erpTenantId") Long erpTenantId);

    /** 物流成本原始行（物流单 × 明细 SKU）：仅已发运（status != PENDING）。 */
    List<ShippingCostLineDTO> selectShippingCostLines(@Param("erpTenantId") Long erpTenantId);

    /** 应付物流商原始行（每物流单一行，含物流商名/USD 总额/付款状态）：全部非删除。 */
    List<PayableProviderRowDTO> selectPayableProviderRows(@Param("erpTenantId") Long erpTenantId);
}
