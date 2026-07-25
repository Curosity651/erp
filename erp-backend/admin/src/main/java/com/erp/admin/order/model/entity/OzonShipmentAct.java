package com.erp.admin.order.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Ozon 运单（交接单 act）。
 * <p>
 * 一行 = 一份运单，对应一个 (店铺, 物流方式, 发货日期) 组合。
 * <p>
 * 注意：运单内容由 Ozon 按「物流方式 + 发货日期」汇总当日全部待发货件，
 * 因此 {@code orderCount} 仅记录本次操作纳入的订单数（留痕用），
 * 并不代表运单 PDF 中实际包含的货件数量。
 * <p>
 * 本实体不声明 tenantId 字段，租户列由 MyBatis-Plus 多租户拦截器自动注入与过滤
 * （见 {@code ErpTenantLineHandler} 白名单）。
 *
 * @author system
 */
@Data
@TableName("ozon_shipment_act")
@Schema(title = "Ozon运单(交接单act)")
public class OzonShipmentAct {

    /** 创建中：本地已建行，尚未拿到 Ozon 运单ID */
    public static final String STATUS_CREATING = "CREATING";
    /** Ozon 生成中：已拿到运单ID，PDF 尚未就绪 */
    public static final String STATUS_PENDING = "PENDING";
    /** 就绪：PDF 已下载并存入 OSS */
    public static final String STATUS_READY = "READY";
    /** 失败 */
    public static final String STATUS_FAILED = "FAILED";

    @TableId
    private Long id;

	private String requestKey;

    @Schema(title = "批次号：一次【准备发运】产生的一组运单共用，前端据此轮询")
    private String batchNo;

    @Schema(title = "店铺ID：运单调用所用 Ozon 凭证的归属")
    private Long shopId;

    @Schema(title = "Ozon 物流方式ID")
    private Long deliveryMethodId;

    @Schema(title = "物流方式名称(快照)")
    private String deliveryMethodName;

    @Schema(title = "发货仓库名(快照)")
    private String warehouseName;

    @Schema(title = "发货日期")
    private LocalDate departureDate;

    @Schema(title = "箱数")
    private Integer containersCount;

    @Schema(title = "Ozon 侧运单ID")
    private Long ozonActId;

    @Schema(title = "CREATING / PENDING / READY / FAILED")
    private String status;

    @Schema(title = "OSS 对象键(PDF)")
    private String objectKey;

    private String fileName;

    @Schema(title = "本次纳入的订单数(留痕，非运单实际货件数)")
    private Integer orderCount;

    private String errorMsg;

    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 是否已到达终态（不再需要轮询） */
    public boolean isTerminal() {
        return STATUS_READY.equals(status) || STATUS_FAILED.equals(status);
    }
}
