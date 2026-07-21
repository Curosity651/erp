package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可用库位（上架分配用）。仅返回空闲且分区与品质匹配的库位。
 *
 * @author erp
 */
@Data
@Schema(title = "可用库位")
public class AvailableLocationVO {

    @Schema(title = "库位ID")
    private Long locationId;

    @Schema(title = "库位编码 A1-03")
    private String locationCode;

    @Schema(title = "所属分区ID")
    private Long zoneId;

    @Schema(title = "分区名称")
    private String zoneName;

    @Schema(title = "分区类型 STANDARD/DEFECTIVE")
    private String zoneType;

    @Schema(title = "排号")
    private String rackNo;

    @Schema(title = "列号")
    private Integer columnNo;

    @Schema(title = "是否虚拟库位(1=收纳积压货用，服务商不可见)")
    private Integer isVirtual;

}
