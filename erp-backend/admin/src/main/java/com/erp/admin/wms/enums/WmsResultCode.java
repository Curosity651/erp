package com.erp.admin.wms.enums;

import org.ballcat.common.model.result.ResultCode;

/**
 * WMS 模块业务错误码
 */
public enum WmsResultCode implements ResultCode {

    /**
     * 库存不足
     */
    STOCK_INSUFFICIENT(40010, "库存不足"),

    /**
     * 库位已生成，不可重复生成
     */
    LOCATION_ALREADY_GENERATED(40009, "库位已生成，不可重复生成"),

    /**
     * 仓库结构参数无效（排/列必须大于0）
     */
    INVALID_RACK_STRUCTURE(40011, "仓库结构参数无效：排/列必须大于0"),

    /**
     * 缺少默认标准分区，请先初始化分区
     */
    STANDARD_ZONE_REQUIRED(40012, "请先创建标准(STANDARD)分区再生成库位"),

    /**
     * 仓库已有货物落位，禁止清空库位重新生成
     */
    LOCATION_OCCUPIED(40013, "该仓库已有货物落位，无法重新生成库位；请先清空库存"),

    /**
     * 该排在所选时间段已被分配
     */
    RACK_ALREADY_ASSIGNED(40013, "该排在所选时间段已被分配"),

    /**
     * 仅海外仓平台可分配货架
     */
    RACK_ASSIGN_FORBIDDEN(40014, "仅海外仓平台可分配货架"),

    /**
     * 库存聚合刷新版本冲突（乐观锁重试耗尽）
     */
    INVENTORY_VERSION_CONFLICT(40015, "库存聚合刷新冲突，请重试"),

    /**
     * 仅海外仓平台可执行收货/上架作业
     */
    INBOUND_OP_FORBIDDEN(40016, "仅海外仓平台可执行收货/上架作业"),

    /**
     * 上架分配数量与收货数量不一致
     */
    PUTAWAY_QUANTITY_MISMATCH(40017, "上架分配数量必须与各SKU收货数量一致"),

    /**
     * 上架库位不存在于本仓
     */
    PUTAWAY_LOCATION_NOT_FOUND(40018, "上架库位不存在于本仓库"),

    /**
     * 上架库位已被占用（库位独占，一库位一SKU）
     */
    PUTAWAY_LOCATION_OCCUPIED(40019, "库位已被占用，请另选空库位（一库位仅放一种SKU）"),

    /**
     * 上架库位分区与品质不匹配（良品→标准区，次品→不良品区）
     */
    PUTAWAY_ZONE_QUALITY_MISMATCH(40020, "库位分区与货物品质不匹配：良品须入标准区、次品须入不良品区"),

    /**
     * 仓库货架已分配给服务商，禁止改结构/重新生成库位
     */
    RACK_ASSIGNED_LOCKED(40021, "该仓货架已分配给服务商，无法调整结构/重新生成库位；请先到「货架分配」页解除分配"),

    /**
     * 目标库位有货物占用，不能改分区
     */
    LOCATION_ZONE_MOVE_OCCUPIED(40022, "以下库位有货物占用，不能改分区，请先清空该库位"),

    /**
     * 上架库位所在货架未租给本货主的服务商
     */
	PUTAWAY_RACK_NOT_OWNED(40023, "该库位所在货架未租给本货主的服务商，不能上架；请选择本服务商租用货架上的库位"),

	/**
	 * 新库位库存模式下禁止继续写入旧托盘/批次库存。
	 */
	LEGACY_INVENTORY_WRITE_DISABLED(40024, "旧托盘库存写入已停用");

    private final int code;

    private final String message;

    WmsResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
