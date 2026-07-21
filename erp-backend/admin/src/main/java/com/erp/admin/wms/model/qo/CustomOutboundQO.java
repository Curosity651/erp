package com.erp.admin.wms.model.qo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 自定义出库单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "自定义出库单查询对象")
public class CustomOutboundQO {

    @Schema(title = "出库单号")
    private String outboundNo;

    @Schema(title = "出库类型：OFFLINE_ORDER/SAMPLE_SEND/SCRAP/RETURN_TO_SUPPLIER/OTHER")
    private String customType;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "出库仓库ID")
    private Long warehouseId;

    @Schema(title = "单据状态")
    private String orderStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "出库日期开始")
    private LocalDate outboundDateStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "出库日期结束")
    private LocalDate outboundDateEnd;

}
