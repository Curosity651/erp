package com.erp.admin.order.service.label.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 平台上下文容器
 * <p>
 * 用于在面单打印流程中携带平台特有的数据，避免在策略接口中暴露平台特有类型。
 * <p>
 * 使用示例：
 * <pre>
 * // 创建空上下文
 * PlatformContext ctx = PlatformContext.empty();
 *
 * // 创建带初始值的上下文
 * PlatformContext ctx = PlatformContext.of("supplyMap", supplyMap);
 *
 * // 获取数据
 * Map<String, WbSupply> supplyMap = ctx.get("supplyMap");
 * </pre>
 *
 * @author system
 */
public class PlatformContext {

    /**
     * 数据存储容器
     */
    private final Map<String, Object> data = new HashMap<>();

    /**
     * 私有构造，通过静态工厂方法创建实例
     */
    private PlatformContext() {
    }

    /**
     * 创建空的平台上下文
     *
     * @return 空上下文实例
     */
    public static PlatformContext empty() {
        return new PlatformContext();
    }

    /**
     * 创建带初始键值对的平台上下文
     *
     * @param key   键
     * @param value 值
     * @return 上下文实例
     */
    public static PlatformContext of(String key, Object value) {
        PlatformContext ctx = new PlatformContext();
        ctx.put(key, value);
        return ctx;
    }

    /**
     * 存入数据
     *
     * @param key   键
     * @param value 值
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * 获取数据（泛型自动转换）
     *
     * @param key 键
     * @param <T> 目标类型
     * @return 值，不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    /**
     * 获取数据（带类型参数，用于明确类型）
     *
     * @param key  键
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 值，不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        return (T) data.get(key);
    }

    /**
     * 判断是否包含指定键
     *
     * @param key 键
     * @return 是否包含
     */
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
}
