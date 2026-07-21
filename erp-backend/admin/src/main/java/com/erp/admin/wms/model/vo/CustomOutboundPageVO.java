package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自定义出库单分页VO
 *
 * @author erp
 */
@Data
@Schema(title = "自定义出库单分页VO")
public class CustomOutboundPageVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "出库单号")
    private String outboundNo;

    @Schema(title = "出库类型：OFFLINE_ORDER/SAMPLE_SEND/SCRAP/RETURN_TO_SUPPLIER/OTHER")
    private String customType;

    @Schema(title = "关联单号文本")
    private String refNo;

    @Schema(title = "出库仓库ID")
    private Long warehouseId;

    @Schema(title = "出库仓库名称")
    private String warehouseName;

    @Schema(title = "出库日期")
    private LocalDate outboundDate;

    @Schema(title = "SKU数量")
    private Integer skuCount;

    @Schema(title = "出库总数量（应出）")
    private Integer totalQuantity;

    @Schema(title = "单据状态")
    private String orderStatus;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createBy;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

}
