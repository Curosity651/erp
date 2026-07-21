package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 拣货员选项。
 *
 * @author erp
 */
@Data
@Schema(title = "拣货员选项")
public class PickerVO {

    @Schema(title = "用户ID")
    private Long id;

    @Schema(title = "姓名")
    private String name;

}
