package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 退货质检+上架入参。
 *
 * @author erp
 */
@Data
@Schema(title = "退货质检入参")
public class ReturnQcDTO {

    @NotNull(message = "退货单ID不能为空")
    @Schema(title = "退货单ID")
    private Long returnOrderId;

    @NotNull(message = "质检明细不能为空")
    @Schema(title = "质检明细")
    private List<ReturnQcLineDTO> lines;

    @Data
    @Schema(title = "质检明细行")
    public static class ReturnQcLineDTO {

        @Schema(title = "SKU编码")
        private String skuCode;

        @Schema(title = "良品数量")
        private Integer qualifiedQty;

        @Schema(title = "残次品数量")
        private Integer damagedQty;

        @Schema(title = "良品回库分区 RETURN/STANDARD")
        private String qualifiedZone;

        @Schema(title = "良品回库库位")
        private String qualifiedLocationCode;

        @Schema(title = "残次品回库库位")
        private String damagedLocationCode;

        @Schema(title = "质检备注")
        private String qcRemark;

        @Schema(title = "质检照片OSS文件ID列表(sys_file.id)")
        private List<Long> photoFileIds;

    }

}
