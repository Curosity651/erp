package com.erp.admin.platform.wildberries.enums;

/**
 * Wildberries 平台侧订单状态（wbStatus）
 * 来源：/api/v3/orders/status
 */
public enum WildberriesWbStatusEnum {

    /** 等待处理/派送（商家已发出但平台未入仓） */
    WAITING("waiting", "待处理"),

    /** 已分拣，货物已送达平台仓库/office */
    SORTED("sorted", "仓库已分拣"),

    /** 待取货（买家可取货） */
    READY_FOR_PICKUP("ready_for_pickup", "已到自提点，待提取"),

    /** 推迟配送（暂未发出给买家） */
    POSTPONED_DELIVERY("postponed_delivery", "快递延迟配送"),

    /** 已售出（买家签收完成） */
    SOLD("sold", "已成交"),

    /** 已取消（平台或买家取消） */
    CANCELED("canceled", "商家取消"),

    /** 客户取消 */
    CANCELED_BY_CLIENT("canceled_by_client", "客户取消（收货时）"),

    /** 客户拒收 */
    DECLINED_BY_CLIENT("declined_by_client", "客户下单后1小时内取消"),

    /** 质量问题（缺陷品） */
    DEFECT("defect", "因瑕疵取消");

    private final String code;
    private final String label;

    WildberriesWbStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static WildberriesWbStatusEnum fromCode(String code) {
        if (code == null) return null;
        String normalized = code.toLowerCase();
        for (WildberriesWbStatusEnum e : values()) {
            if (e.code.equals(normalized)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 获取状态的中文标签
     *
     * @param code 状态代码
     * @return 中文标签，如果找不到则返回原始代码
     */
    public static String getLabelByCode(String code) {
        WildberriesWbStatusEnum status = fromCode(code);
        return status != null ? status.getLabel() : code;
    }
}
