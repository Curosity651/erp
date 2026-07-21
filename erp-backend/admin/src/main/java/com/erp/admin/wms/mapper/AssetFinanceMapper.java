package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.dto.PayableProviderRowDTO;
import com.erp.admin.wms.model.dto.PayableSupplierRowDTO;
import com.erp.admin.wms.model.dto.PurchaseCostAggDTO;
import com.erp.admin.wms.model.dto.ShippingCostLineDTO;

import java.util.List;

/**
 * 货主资产与账务取数 Mapper（只读聚合）。
 * <p>
 * 全部经拦截器按当前货主自动隔离：wms_purchase_order / wms_shipping_order /
 * wms_logistics_provider 走 erp_tenant_id 货主隔离，supplier 走 tenant_id 行级隔离。
 *
 * @author erp
 */
public interface AssetFinanceMapper {

    /** 采购成本聚合（按 SKU × 币种）：非草稿非取消采购单，用于加权平均采购单价。 */
    List<PurchaseCostAggDTO> selectPurchaseCostAgg();

    /** 应付供应商原始行（每采购单一行，含供应商名/币种/预付尾款状态）：非草稿非取消。 */
    List<PayableSupplierRowDTO> selectPayableSupplierRows();

    /** 物流成本原始行（物流单 × 明细 SKU）：仅已发运（status != PENDING）。 */
    List<ShippingCostLineDTO> selectShippingCostLines();

    /** 应付物流商原始行（每物流单一行，含物流商名/USD 总额/付款状态）：全部非删除。 */
    List<PayableProviderRowDTO> selectPayableProviderRows();
}
