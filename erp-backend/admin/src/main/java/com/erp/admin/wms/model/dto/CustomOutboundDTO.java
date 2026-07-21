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
 * 自定义出库单数据传输对象
 * <p>
 * 用于不挂平台订单的出库（线下/独立站订单发货、样品寄送、销毁报废、退供应商等）。
 * 与销售出库单共用 {@code wms_sales_outbound_order} 表，{@code source_type=CUSTOM}，
 * 明细不关联电商订单，直接 SKU + 数量。
 * </p>
 *
 * @author erp
 */
@Data
@Schema(title = "自定义出库单数据传输对象")
public class CustomOutboundDTO {

    @Null(groups = CreateGroup.class, message = "新建时ID必须为空")
    @NotNull(groups = UpdateGroup.class, message = "编辑时ID不能为空")
    @Schema(title = "主键ID")
    private Long id;

    @NotNull(message = "出库仓库不能为空")
    @Schema(title = "出库仓库ID")
    private Long warehouseId;

    @NotNull(message = "出库日期不能为空")
    @Schema(title = "出库日期")
    private LocalDate outboundDate;

    @NotBlank(message = "出库类型不能为空")
    @Schema(title = "出库类型：OFFLINE_ORDER/SAMPLE_SEND/SCRAP/RETURN_TO_SUPPLIER/OTHER")
    private String customType;

    @Size(max = 100, message = "关联单号长度不能超过100")
    @Schema(title = "关联单号文本（线下订单号/退供单号等，可空）")
    private String refNo;

    @Size(max = 100, message = "收件人姓名长度不能超过100")
    @Schema(title = "收件人姓名（可空，销毁类无收货人）")
    private String receiverName;

    @Size(max = 50, message = "收件人电话长度不能超过50")
    @Schema(title = "收件人电话（可空）")
    private String receiverPhone;

    @Size(max = 500, message = "收货地址长度不能超过500")
    @Schema(title = "收货地址（可空）")
    private String receiverAddress;

    @Schema(title = "物流产品ID（需要运输和计费时选择；销毁、报废等可不选）")
    private Long logisticsProductId;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(title = "备注")
    private String remark;

    @Valid
    @NotEmpty(message = "出库明细不能为空")
    @Schema(title = "出库明细列表")
    private List<CustomOutboundItemDTO> items;

}
