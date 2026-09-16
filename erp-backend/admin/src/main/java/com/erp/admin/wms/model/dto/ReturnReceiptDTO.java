package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(title = "海外仓退货收货登记")
public class ReturnReceiptDTO {

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    private LocalDate returnDate;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    @Valid
    @NotEmpty(message = "退货商品不能为空")
    @Size(max = 500, message = "单次最多登记500个退货商品")
    private List<Line> items;

    @Data
    public static class Line {
        @NotBlank(message = "全局SKU不能为空")
        private String warehouseSkuCode;

        @Min(value = 1, message = "实收数量必须大于0")
        @NotNull(message = "实收数量不能为空")
        private Integer receivedQty;

        private String platformOrderId;

        private String returnReason;

        private List<Long> photoFileIds;
    }
}
