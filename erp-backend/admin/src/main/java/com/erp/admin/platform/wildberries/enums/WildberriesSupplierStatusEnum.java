package com.erp.admin.platform.wildberries.enums;

/**
 * Wildberries 订单商家侧状态（supplierStatus）
 * 来源：/api/v3/orders/status
 */
public enum WildberriesSupplierStatusEnum {

    /** 新建，订单刚生成，尚未确认 */
    NEW("new", "新订单"),

    /** 商家已确认待发货 */
    CONFIRM("confirm", "拣货中/组装中"),

    /** 商家已发货（通常代表包裹已交付快递或仓库） */
    COMPLETE("complete", "已处理"),

    /** 商家已取消订单 */
    CANCEL("cancel", "商家取消"),

    /** 买家已收货，订单完成 */
    RECEIVE("receive", "买家已签收"),

    /** 买家拒收 / 商家拒单 */
    REJECT("reject", "买家拒收");

    private final String code;
    private final String label;

    WildberriesSupplierStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static WildberriesSupplierStatusEnum fromCode(String code) {
        if (code == null) return null;
        String normalized = code.toLowerCase();
        for (WildberriesSupplierStatusEnum e : values()) {
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
        WildberriesSupplierStatusEnum status = fromCode(code);
        return status != null ? status.getLabel() : code;
    }
}
