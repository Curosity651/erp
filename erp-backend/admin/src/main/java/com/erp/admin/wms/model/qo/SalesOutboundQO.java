package com.erp.admin.wms.model.qo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 销售出库单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "销售出库单查询对象")
public class SalesOutboundQO {

    @Schema(title = "出库单号")
    private String outboundNo;

    @Schema(title = "平台：wildberries/ozon/yandex")
    private String platform;

    @Schema(title = "平台订单号")
    private String platformOrderId;

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
