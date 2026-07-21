package com.erp.admin.common.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 时区转换工具类
 * <p>
 * 用于将 UTC 时间转换为莫斯科时间（Europe/Moscow, UTC+3）
 *
 * @author system
 */
public class TimeZoneUtils {

    /**
     * 莫斯科时区
     */
    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

    /**
     * 将 UTC 时间转换为莫斯科时间
     *
     * @param utcTime UTC 时间
     * @return 莫斯科时间，如果输入为 null 则返回 null
     */
    public static LocalDateTime toMoscowTime(LocalDateTime utcTime) {
        if (utcTime == null) {
            return null;
        }
        return utcTime.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(MOSCOW_ZONE)
                .toLocalDateTime();
    }

    /**
     * 将 UTC OffsetDateTime 转换为莫斯科时间的 LocalDateTime
     *
     * @param utcTime UTC 时间（带时区偏移）
     * @return 莫斯科时间，如果输入为 null 则返回 null
     */
    public static LocalDateTime toMoscowTime(OffsetDateTime utcTime) {
        if (utcTime == null) {
            return null;
        }
        return utcTime.atZoneSameInstant(MOSCOW_ZONE)
                .toLocalDateTime();
    }

    /**
     * 将 UTC OffsetDateTime 转换为莫斯科时间的 OffsetDateTime
     *
     * @param utcTime UTC 时间（带时区偏移）
     * @return 莫斯科时间（带时区偏移），如果输入为 null 则返回 null
     */
    public static OffsetDateTime toMoscowTimeWithOffset(OffsetDateTime utcTime) {
        if (utcTime == null) {
            return null;
        }
        return utcTime.atZoneSameInstant(MOSCOW_ZONE)
                .toOffsetDateTime();
    }
}
