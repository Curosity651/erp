package com.erp.admin.wms.model.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 打包签出列表查询对象。
 *
 * @author erp
 */
@Data
@Schema(title = "打包签出列表查询对象")
public class PackShipQO {

    @Schema(title = "出库单号")
    private String outboundNo;

    @Schema(title = "状态 PICKING/PACKED/SHIPPED/COMPLETED")
    private String status;

    @Schema(title = "货主ID(平台看全部，非空才过滤)")
    private Long erpTenantId;

    @Schema(title = "WMS服务商筛选(按出库单归属货主的上级服务商过滤)")
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
