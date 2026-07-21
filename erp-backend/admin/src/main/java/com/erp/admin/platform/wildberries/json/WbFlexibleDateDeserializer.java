package com.erp.admin.platform.wildberries.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 灵活解析 WB 返回的日期字段（可能是纯日期或带时间/时区）。
 * 示例：
 *  - 2025-11-06
 *  - 2025-11-06T13:30:22Z
 *  - 2025-11-06T13:30:22+03:00
 *  - 2025-11-06T13:30:22.123
 *
 * 解析策略：
 * 1. 正则匹配纯日期（长度 10，yyyy-MM-dd）直接 LocalDate.parse
 * 2. 尝试按 ISO_OFFSET_DATE_TIME 解析（带时区）→ 取 toLocalDate()
 * 3. 尝试按 ISO_LOCAL_DATE_TIME 解析（无时区）→ 取 toLocalDate()
 * 4. 回退：截取前 10 位再 LocalDate.parse（避免异常中断）
 */
public class WbFlexibleDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getText();
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String s = raw.trim();
        // 纯日期格式 yyyy-MM-dd
        if (s.length() == 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                throw new IOException("无法解析日期: " + s, e);
            }
        }
        // 尝试带时区
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return zdt.toLocalDate();
        } catch (Exception ignored) {
        }
        // 尝试无时区的日期时间
        try {
            LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.toLocalDate();
        } catch (Exception ignored) {
        }
        // 回退：截取前 10 位
        if (s.length() >= 10) {
            String head = s.substring(0, 10);
            try {
                return LocalDate.parse(head, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                throw new IOException("无法解析日期(回退失败): " + s, e);
            }
        }
        throw new IOException("无法解析日期时间: " + s);
    }
}
