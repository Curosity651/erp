package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 签出入参。
 *
 * @author erp
 */
@Data
@Schema(title = "签出入参")
public class ShipDTO {

    @NotNull(message = "出库单ID不能为空")
    @Schema(title = "出库单ID")
    private Long outboundOrderId;

    @NotBlank(message = "物流渠道不能为空")
    @Schema(title = "渠道代码；AUTO=自动选择")
    private String channel;

    @Schema(title = "面单跟踪号（选填，不影响签出和计费）")
    private String trackingNo;

    @NotNull(message = "称重不能为空")
    @Schema(title = "称重kg")
    private BigDecimal weight;

    @Schema(title = "照片数量(needPhoto 时必传>0)")
    private Integer photoCount;

    @Schema(title = "确认按整托计费的托盘ID；空值表示采用系统识别的全部整托")
    private List<Long> fullPalletIds;

    @Schema(title = "非整托中的大箱件数")
    private Integer largeBoxCount;

    @Schema(title = "非整托中的小件散货件数")
    private Integer smallItemCount;

}
