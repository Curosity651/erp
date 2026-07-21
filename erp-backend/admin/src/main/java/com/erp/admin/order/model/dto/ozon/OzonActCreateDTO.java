package com.erp.admin.order.model.dto.ozon;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 【准备发运】请求体。
 *
 * @author system
 */
@Data
@Schema(title = "准备发运请求")
public class OzonActCreateDTO {

    @NotEmpty(message = "订单不能为空")
    @Schema(title = "选中的订单ID列表")
    private List<Long> orderIds;

    @NotNull(message = "发货日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(title = "发货日期 yyyy-MM-dd")
    private LocalDate departureDate;
}
