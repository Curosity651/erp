package com.erp.admin.platform.wildberries.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Wildberries 日期时间反序列化器
 * <p>
 * 处理 WB API 返回的带时区的日期时间格式,如:
 * - "2025-01-20T13:30:22+03:00"
 * - "2022-10-13T00:00:00Z"
 * <p>
 * 将带时区的时间转换为 LocalDateTime (丢弃时区信息)
 *
 * @author system
 */
public class WbDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateStr = p.getText();
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 解析带时区的日期时间
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            // 转换为 LocalDateTime (丢弃时区信息)
            return zonedDateTime.toLocalDateTime();
        } catch (Exception e) {
            throw new IOException("无法解析日期时间: " + dateStr, e);
        }
    }
}
