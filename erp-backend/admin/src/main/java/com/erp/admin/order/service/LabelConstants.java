package com.erp.admin.order.service;

/**
 * 订单/批次标签相关的常量集中管理。
 */
public final class LabelConstants {

    private LabelConstants() {}

    // 文件名中的类型片段
    public static final String FILE_NAME_ORDER = "ORDER";
    public static final String FILE_NAME_SUPPLY = "SUPPLY";

    // 文件类型（入库记录类型）
    public static final String FILE_TYPE_WB_ORDER_PDF = "WB_ORDER_PDF";
    public static final String FILE_TYPE_WB_SUPPLY_PDF = "WB_SUPPLY_PDF";
    public static final String FILE_TYPE_OZON_PDF = "OZON_PDF";
    public static final String FILE_TYPE_YD_PDF = "YD_PDF";

    // 批次状态
    public static final String BATCH_STATUS_CREATED = "CREATED";
    public static final String BATCH_STATUS_GENERATED = "GENERATED";
    public static final String BATCH_STATUS_PARTIAL = "PARTIAL";
    public static final String BATCH_STATUS_FAILED = "FAILED";

    // 文件状态
    public static final String FILE_STATUS_PENDING = "PENDING";
    public static final String FILE_STATUS_GENERATED = "GENERATED";
    public static final String FILE_STATUS_FAILED = "FAILED";

    // 条目（订单）状态
    public static final String ITEM_STATUS_PENDING = "PENDING";
    public static final String ITEM_STATUS_SUCCESS = "SUCCESS";
    public static final String ITEM_STATUS_FAILED = "FAILED";

    // 条目失败原因码
    public static final String ITEM_FAIL_NON_WB_PLATFORM = "NON_WB_PLATFORM";
    public static final String ITEM_FAIL_ORDER_LABEL_MISSING = "ORDER_LABEL_MISSING";
    public static final String ITEM_FAIL_SUPPLY_LABEL_MISSING = "SUPPLY_LABEL_MISSING";
    public static final String ITEM_FAIL_FILE_GENERATE = "FILE_GENERATE_FAILED";
    public static final String ITEM_FAIL_FILE_UPLOAD = "FILE_UPLOAD_FAILED";
    public static final String ITEM_FAIL_SHIPMENT_MISSING = "SHIPMENT_MISSING";
    public static final String ITEM_FAIL_SKU_MAPPING_MISSING = "SKU_MAPPING_MISSING";
    public static final String ITEM_FAIL_SUPPLY_NOT_FOUND = "SUPPLY_NOT_FOUND";
    public static final String ITEM_FAIL_UNKNOWN = "UNKNOWN";

    // ========== 新增常量 ==========

    /** 家具品类ID */
    public static final Long FURNITURE_CATEGORY_ID = 5000035L;

    /** 文件名最大长度 */
    public static final int MAX_FILENAME_LENGTH = 180;

    /** 文件名组件最大长度 */
    public static final int MAX_FILENAME_COMPONENT_LENGTH = 60;

    /** OSS 标签文件路径前缀 */
    public static final String OSS_LABEL_PATH_PREFIX = "labels/";

    /** 不安全的文件名字符正则 */
    public static final String UNSAFE_FILENAME_CHARS_REGEX = "[\\\\/\\?%*:|\"<>]";

    /** 连续空白字符正则 */
    public static final String MULTIPLE_WHITESPACE_REGEX = "\\s+";

    /** 连续横杠正则 */
    public static final String MULTIPLE_DASH_REGEX = "-+";
}


