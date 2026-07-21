package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物流产品 VO（服务商管理页 + 货主建单选项共用）。
 *
 * @author erp
 */
@Data
@Schema(title = "物流产品")
public class LogisticsProductVO {

    private Long id;

    private String productName;

    private String productCode;

    @Schema(title = "特性词条")
    private List<String> tags;

    @Schema(title = "统一单价(每次使用)")
    private BigDecimal unitPrice;

    @Schema(title = "状态 1启用/0停用")
    private Integer status;

    private String remark;

    private String createTime;

}
