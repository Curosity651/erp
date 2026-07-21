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
 * 自定义退货单数据传输对象
 * <p>
 * 用于不挂靠平台订单的退货入库（平台批量退货混包、无单退件、样品收回、发错召回等）。 与采购入库单共用
 * {@code wms_purchase_inbound_order} 表，{@code source_type=CUSTOM_RETURN}，无关联物流单/采购单。
 * </p>
 *
 * @author erp
 */
@Data
@Schema(title = "自定义退货单数据传输对象")
public class CustomReturnDTO {

    @Null(groups = CreateGroup.class, message = "新建时ID必须为空")
    @NotNull(groups = UpdateGroup.class, message = "编辑时ID不能为空")
    @Schema(title = "主键ID")
    private Long id;

    @NotBlank(groups = CreateGroup.class, message = "退货单号不能为空")
    @Size(max = 50, message = "退货单号长度不能超过50")
    @Schema(title = "退货单号（手动输入）")
    private String inboundNo;

    @NotBlank(message = "退货类型不能为空")
    @Size(max = 32, message = "退货类型长度不能超过32")
    @Schema(title = "退货类型: PLATFORM_BATCH平台批量退货 / NO_ORDER无单退件 / SAMPLE_BACK样品收回 / WRONG_SHIPMENT发错召回 / OTHER其他")
    private String returnType;

    @Size(max = 100, message = "关联单号长度不能超过100")
    @Schema(title = "关联单号（选填，纯文本参考: 平台订单号/物流追踪号等）")
    private String refNo;

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
    @NotEmpty(groups = CreateGroup.class, message = "退货明细不能为空")
    @Schema(title = "退货明细列表")
    private List<CustomReturnItemDTO> items;

}
