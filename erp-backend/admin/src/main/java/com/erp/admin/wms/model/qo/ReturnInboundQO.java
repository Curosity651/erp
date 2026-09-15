package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 退货入库单查询对象
 *
 * @author erp
 */
@Data
@Schema(title = "退货入库单查询对象")
public class ReturnInboundQO {

    @Schema(title = "退货单号")
    private String returnNo;

    @Schema(title = "平台订单号")
    private String platformOrderId;

    @Schema(title = "平台")
    private String platform;

    @Schema(title = "SKU编码")
    private String skuCode;

    @Schema(title = "入库仓库ID")
    private Long warehouseId;

    @Schema(title = "退货处理状态")
    private String returnStatus;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "退货日期开始")
    private LocalDate returnDateStart;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(title = "退货日期结束")
    private LocalDate returnDateEnd;

}
