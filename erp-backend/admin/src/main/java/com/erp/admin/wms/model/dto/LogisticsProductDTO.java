package com.erp.admin.wms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 物流产品新建/编辑入参。
 *
 * @author erp
 */
@Data
@Schema(title = "物流产品入参")
public class LogisticsProductDTO {

    @Schema(title = "ID(编辑时必填)")
    private Long id;

    @NotBlank(message = "产品名称不能为空")
    @Schema(title = "产品名称")
    private String productName;

    @NotBlank(message = "产品编码不能为空")
    @Schema(title = "产品编码")
    private String productCode;

    @Schema(title = "特性词条(大件/小件/自提/自定义...)")
    private List<String> tags;

    @NotNull(message = "单价不能为空")
    @Schema(title = "统一单价(每次使用)")
    private BigDecimal unitPrice;

    @NotBlank(message = "币种不能为空")
    @Schema(title = "币种")
    private String currency;

    @Schema(title = "产品说明")
    private String productDescription;

    @Schema(title = "备注")
    private String remark;

}
