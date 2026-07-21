package com.erp.admin.wms.model.vo;

import com.erp.admin.wms.model.entity.FboSyncLog.AutoCreatedWarehouse;
import com.erp.admin.wms.model.entity.FboSyncLog.FboSyncFailDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * FBO同步日志详情VO
 *
 * @author erp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "FBO同步日志详情VO")
public class FboSyncLogDetailVO extends FboSyncLogPageVO {

    @Schema(title = "失败明细列表")
    private List<FboSyncFailDetail> failDetails;

    @Schema(title = "自动创建仓库数量")
    private Integer autoCreatedWarehouseCount;

    @Schema(title = "自动创建的仓库列表")
    private List<AutoCreatedWarehouse> autoCreatedWarehouses;

}
