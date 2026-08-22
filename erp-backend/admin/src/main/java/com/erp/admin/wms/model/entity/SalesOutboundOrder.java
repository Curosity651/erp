package com.erp.admin.wms.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售出库单实体
 *
 * @author erp
 */
@Data
@TableName("wms_sales_outbound_order")
@Schema(title = "销售出库单实体")
public class SalesOutboundOrder {

    @TableId(type = IdType.AUTO)
    @Schema(title = "主键ID")
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "归属货主 erp_tenant_id（数据级隔离，新建时自动盖章）")
    private Long erpTenantId;

    @Schema(title = "出库单号")
    private String outboundNo;

    @Schema(title = "来源类型：SALES销售出库 / CUSTOM自定义出库")
    private String sourceType;

    @Schema(title = "自定义出库类型：OFFLINE_ORDER/SAMPLE_SEND/SCRAP/RETURN_TO_SUPPLIER/OTHER（仅 CUSTOM）")
    private String customType;

    @Schema(title = "关联单号文本（线下订单号/退供单号等，仅 CUSTOM，可空）")
    private String refNo;

    @Schema(title = "收件人姓名（仅 CUSTOM，可空，销毁类无收货人）")
    private String receiverName;

    @Schema(title = "收件人电话（仅 CUSTOM，可空）")
    private String receiverPhone;

    @Schema(title = "收货地址（仅 CUSTOM，可空）")
    private String receiverAddress;

    @Schema(title = "平台：wildberries/ozon/yandex（自定义出库为空）")
    private String platform;

    @Schema(title = "出库仓库ID")
    private Long warehouseId;

    @Schema(title = "出库日期")
    private LocalDate outboundDate;

    @Schema(title = "订单数量")
    private Integer orderCount;

    @Schema(title = "SKU数量")
    private Integer skuCount;

    @Schema(title = "出库总数量")
    private Integer totalQuantity;

    @Schema(title = "单据状态: DRAFT/CONFIRMED/CANCELLED + 平台作业 PICKING/PICKED/BACKORDER/PACKED/SHIPPED/COMPLETED")
    private String orderStatus;

    @Schema(title = "关联库存过账单ID")
    private Long postingId;

    @Schema(title = "拣货任务类型 SINGLE/WAVE（平台创建任务后回填）")
    private String pickMode;

    @Schema(title = "拣货员用户ID（平台下架后回填）")
    private Long pickerId;

    @Schema(title = "拣货员姓名（平台下架后回填）")
    private String pickerName;

    @Schema(title = "打包模式 BY_SKU/BY_ORDER/SECONDARY/CARTON（打包后回填）")
    private String packMode;

    @Schema(title = "打包员（打包后回填）")
    private String packerName;

    @Schema(title = "签出物流渠道名（签出后回填）")
    private String channelName;

    @Schema(title = "面单跟踪号（签出后回填）")
    private String trackingNo;

    private Long shippedBy;

    private String shippedByName;

    private LocalDateTime shippedTime;

    @Schema(title = "称重kg（签出后回填）")
    private java.math.BigDecimal weight;

    @Schema(title = "链路二物流费（签出后回填）")
    private java.math.BigDecimal shippingFee;

    @Schema(title = "关联物流产品ID（计费依据，可空）")
	private Long logisticsProductId;

	/** 平台资料处理：WAREHOUSE_PRINT / OWNER_PROVIDED。 */
	private String documentMode;

    @Schema(title = "备注")
    private String remark;

    @Schema(title = "创建人ID")
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

}
