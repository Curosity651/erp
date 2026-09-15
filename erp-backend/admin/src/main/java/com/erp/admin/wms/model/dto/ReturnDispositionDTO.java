package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class ReturnDispositionDTO {
    @NotNull(message = "退货处理单ID不能为空")
    private Long returnOrderId;

    @Valid
    @NotEmpty(message = "处置明细不能为空")
    private List<Line> items;

    @Data
    public static class Line {
        @NotNull(message = "退货明细ID不能为空")
        private Long itemId;

        @Min(value = 0, message = "上架数量不能为负数")
        private Integer restockQty;

        @Min(value = 0, message = "返工数量不能为负数")
        private Integer reworkQty;

        @Min(value = 0, message = "销毁数量不能为负数")
        private Integer scrapQty;

        @Size(max = 500, message = "处置说明长度不能超过500")
        private String remark;
    }
}
