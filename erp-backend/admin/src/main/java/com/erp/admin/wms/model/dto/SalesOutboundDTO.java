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
 * 销售出库单DTO
 *
 * @author erp
 */
@Data
@Schema(title = "销售出库单DTO")
public class SalesOutboundDTO {

    @Null(groups = CreateGroup.class, message = "新建时ID必须为空")
    @NotNull(groups = UpdateGroup.class, message = "编辑时ID不能为空")
    @Schema(title = "主键ID")
    private Long id;

    @NotBlank(message = "平台不能为空")
    @Schema(title = "平台：wildberries/ozon/yandex")
    private String platform;

    @NotNull(message = "出库仓库不能为空")
    @Schema(title = "出库仓库ID")
    private Long warehouseId;

    @NotNull(message = "出库日期不能为空")
    @Schema(title = "出库日期")
    private LocalDate outboundDate;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(title = "备注")
    private String remark;

    @Schema(title = "物流产品ID(父服务商提供，选填；签出时按其单价生成物流费)")
    private Long logisticsProductId;

    @Valid
    @NotEmpty(message = "出库明细不能为空")
    @Schema(title = "出库明细")
    private List<SalesOutboundItemDTO> items;

}
