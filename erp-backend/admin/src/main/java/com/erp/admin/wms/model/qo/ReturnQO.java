package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 退货质检列表查询对象。
 *
 * @author erp
 */
@Data
@Schema(title = "退货质检列表查询对象")
public class ReturnQO {

    @Schema(title = "退货单号")
    private String returnNo;

    @Schema(title = "状态 RETURN_PENDING/QC_PENDING/COMPLETED")
    private String status;

    @Schema(title = "货主ID(平台看全部，非空才过滤)")
    private Long erpTenantId;

    @Schema(title = "WMS服务商筛选(按退货单归属货主的上级服务商过滤)")
    private Long wmsTenantId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "创建日期起始")
    private LocalDate createTimeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "创建日期结束")
    private LocalDate createTimeEnd;

    @Schema(title = "DB状态集合(service 组装)")
    private List<String> dbStatuses;

}
