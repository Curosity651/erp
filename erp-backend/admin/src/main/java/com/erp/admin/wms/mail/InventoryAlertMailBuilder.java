package com.erp.admin.wms.mail;

import com.erp.admin.wms.model.enums.ForecastStatus;
import com.erp.admin.wms.model.vo.ForecastSummaryVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存预警邮件内容构建器
 *
 * @author erp
 */
@Component
public class InventoryAlertMailBuilder {

    @Value("${inventory.alert.mail.subject-prefix:【库存预警】}")
    private String subjectPrefix;

    /**
     * 构建邮件主题
     */
    public String buildSubject(int alertCount) {
        return subjectPrefix + " 发现 " + alertCount + " 个SKU需要关注";
    }

    /**
     * 构建 HTML 邮件内容
     */
    public String buildHtmlContent(List<ForecastSummaryVO> alertItems, int thresholdDays) {
        // 分组：断货 vs 告急
        List<ForecastSummaryVO> stockoutItems = filterByStatus(alertItems, ForecastStatus.STOCKOUT);
        List<ForecastSummaryVO> criticalItems = filterByStatus(alertItems, ForecastStatus.CRITICAL);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
        html.append("<div style='font-family: Arial, sans-serif; max-width: 800px;'>");

        // 标题
        html.append("<h2 style='color: #333;'>库存预警通知</h2>");
        html.append("<p>预警阈值：<strong>").append(thresholdDays).append("天</strong></p>");
        html.append("<p>预警数量：<strong>").append(alertItems.size()).append("个SKU</strong></p>");

        // 断货表格（红色标题）
        if (!stockoutItems.isEmpty()) {
            html.append("<h3 style='color: #e74c3c;'>⚠ 已断货 (").append(stockoutItems.size()).append("个)</h3>");
            html.append(buildTable(stockoutItems, "#e74c3c"));
        }

        // 告急表格（橙色标题）
        if (!criticalItems.isEmpty()) {
            html.append("<h3 style='color: #f39c12;'>⚡ 告急预警 (").append(criticalItems.size()).append("个)</h3>");
            html.append(buildTable(criticalItems, "#f39c12"));
        }

        html.append("<p style='color: #666; margin-top: 20px;'>请及时处理！</p>");
        html.append("</div></body></html>");

        return html.toString();
    }

    private String buildTable(List<ForecastSummaryVO> items, String headerColor) {
        StringBuilder table = new StringBuilder();
        table.append("<table style='border-collapse: collapse; width: 100%; margin-bottom: 20px;'>");
        table.append("<tr style='background-color: ").append(headerColor).append("; color: white;'>");
        table.append("<th style='padding: 8px; border: 1px solid #ddd;'>SKU编码</th>");
        table.append("<th style='padding: 8px; border: 1px solid #ddd;'>区域</th>");
        table.append("<th style='padding: 8px; border: 1px solid #ddd;'>可售库存</th>");
        table.append("<th style='padding: 8px; border: 1px solid #ddd;'>日均销量</th>");
        table.append("<th style='padding: 8px; border: 1px solid #ddd;'>可售天数</th>");
        table.append("</tr>");

        for (ForecastSummaryVO item : items) {
            table.append("<tr>");
            table.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(escapeHtml(item.getSkuCode())).append("</td>");
            table.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(escapeHtml(item.getRegionName())).append("</td>");
            table.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(item.getSellableQuantity()).append("</td>");
            table.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(item.getDailySales()).append("</td>");
            table.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(formatSellableDays(item.getSellableDays())).append("</td>");
            table.append("</tr>");
        }
        table.append("</table>");
        return table.toString();
    }

    private String formatSellableDays(Integer sellableDays) {
        if (sellableDays == null || sellableDays <= 0) {
            return "已断货";
        }
        return sellableDays + "天";
    }

    private List<ForecastSummaryVO> filterByStatus(List<ForecastSummaryVO> items, ForecastStatus status) {
        return items.stream().filter(vo -> vo.getStatus() == status).collect(Collectors.toList());
    }

    /**
     * HTML 转义，防止 XSS
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(text);
    }
}
