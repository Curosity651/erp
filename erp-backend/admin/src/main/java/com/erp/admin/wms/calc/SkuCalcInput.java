package com.erp.admin.wms.calc;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 单 SKU 测算输入（移植自 model_core.SkuInputs）。
 * <p>
 * 方案甲口径：D 工厂成品暂并入 E 在制（ERP 无"成品完工"状态），故 factoryDone 恒为 0，
 * producing 承载采购单未发货批次。
 *
 * @author erp
 */
@Data
@Builder
public class SkuCalcInput {

    /** SKU 编码 */
    private String sku;

    /** A 海外仓库存 */
    @Builder.Default
    private int overseas = 0;

    /** B FBO 仓库存（ERP 暂未单列，合并进 overseas，恒 0） */
    @Builder.Default
    private int fbo = 0;

    /** C 在途批次（物流单已发未收，带 ETA） */
    @Builder.Default
    private List<CalcBatch> transit = Collections.emptyList();

    /** D 工厂成品库存（方案甲恒 0） */
    @Builder.Default
    private int factoryDone = 0;

    /** E 在制批次（采购单未发货，ETA=预计交货日） */
    @Builder.Default
    private List<CalcBatch> producing = Collections.emptyList();
}
