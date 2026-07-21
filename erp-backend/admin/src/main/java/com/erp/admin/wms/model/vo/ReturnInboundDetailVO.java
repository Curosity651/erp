package com.erp.admin.wms.model.vo;

import com.erp.admin.product.model.vo.SkuBriefVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 退货入库单详情VO
 *
 * @author erp
 */
@Data
@Schema(title = "退货入库单详情VO")
public class ReturnInboundDetailVO {

    @Schema(title = "主键ID")
    private Long id;

    @Schema(title = "退货单号")
    private String returnNo;

    @Schema(title = "ERP订单ID")
    private Long erpOrderId;

    @Schema(title = "平台订单号")
    private String platformOrderId;

    @Schema(title = "平台")
    private String platform;

	@Schema(title = "SKU编码")
	private String skuCode;

	@Schema(title = "SKU简要信息")
	private SkuBriefVO skuBrief;

    @Schema(title = "入库仓库ID")
    private Long warehouseId;

    @Schema(title = "入库仓库名称")
    private String warehouseName;

    @Schema(title = "退货日期")
    private LocalDate returnDate;

    @Schema(title = "退货原因")
    private String returnReason;

    @Schema(title = "退货总数量")
    private Integer totalQuantity;

    @Schema(title = "合格入库总数量")
    private Integer qualifiedQuantity;

    @Schema(title = "不合格总数量")
    private Integer unqualifiedQuantity;

    @Schema(title = "转残品数量")
    private Integer toDamagedQuantity;

    @Schema(title = "直接报废数量")
    private Integer scrapQuantity;

    @Schema(title = "创建时可退数量快照")
    private Integer returnableQuantity;

    @Schema(title = "退货状态")
    private String returnStatus;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createBy;

    @Schema(title = "创建人名称")
    private String createByName;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

}
