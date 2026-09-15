package com.erp.admin.wms.model.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ReturnProcessDTO {
    @NotNull(message = "退货处理单ID不能为空")
    private Long returnOrderId;

    @Valid
    @NotEmpty(message = "处理明细不能为空")
    private List<Line> items;

    @Data
    public static class Line {
        @NotNull(message = "退货明细ID不能为空")
        private Long itemId;

        @Min(value = 0, message = "返工合格数量不能为负数")
        private Integer reworkPassQty;

        @Min(value = 0, message = "返工销毁数量不能为负数")
        private Integer reworkScrapQty;

        private String targetZone;

        private String targetLocationCode;
    }
}
