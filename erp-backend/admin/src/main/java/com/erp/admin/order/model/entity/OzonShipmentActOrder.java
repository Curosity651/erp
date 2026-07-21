package com.erp.admin.order.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Ozon 运单-订单关联（留痕）。
 * <p>
 * 记录一次【准备发运】时哪些订单被纳入了哪份运单，供事后追溯。
 * 不代表运单 PDF 的实际内容（内容由 Ozon 按物流方式+发货日期自行汇总）。
 * <p>
 * 租户列由多租户拦截器自动注入，本实体不声明 tenantId。
 *
 * @author system
 */
@Data
@TableName("ozon_shipment_act_order")
@Schema(title = "Ozon运单-订单关联")
public class OzonShipmentActOrder {

    @TableId
    private Long id;

    @Schema(title = "ozon_shipment_act.id(本地主键，非 Ozon 运单ID)")
    private Long actId;

    @Schema(title = "erp_order.id")
    private Long orderId;

    @Schema(title = "Ozon posting number(快照)")
    private String postingNumber;

    private LocalDateTime createTime;
}
