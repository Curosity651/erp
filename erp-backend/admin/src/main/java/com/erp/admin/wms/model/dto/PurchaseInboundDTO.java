package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.ballcat.common.core.validation.group.CreateGroup;
import org.ballcat.common.core.validation.group.UpdateGroup;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购入库单数据传输对象
 *
 * @author erp
 */
@Data
@Schema(title = "采购入库单数据传输对象")
public class PurchaseInboundDTO {

    @Null(groups = CreateGroup.class, message = "新建时ID必须为空")
    @NotNull(groups = UpdateGroup.class, message = "编辑时ID不能为空")
    @Schema(title = "主键ID")
    private Long id;

    @NotBlank(groups = CreateGroup.class, message = "入库单号不能为空")
    @Size(max = 50, message = "入库单号长度不能超过50")
    @Schema(title = "入库单号（手动输入）")
    private String inboundNo;

    @NotNull(message = "关联物流单不能为空")
    @Schema(title = "关联物流单ID")
    private Long shippingOrderId;

    @NotNull(message = "入库仓库不能为空")
    @Schema(title = "入库仓库ID")
    private Long warehouseId;

    @NotNull(message = "入库日期不能为空")
    @Schema(title = "入库日期")
    private LocalDate inboundDate;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(title = "备注")
    private String remark;

    @Valid
    @NotEmpty(groups = CreateGroup.class, message = "入库明细不能为空")
    @Schema(title = "入库明细列表")
    private List<PurchaseInboundItemDTO> items;

}
