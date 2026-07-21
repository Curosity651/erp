package com.erp.admin.order.service.label.model;

import com.erp.admin.order.service.label.OrderGrouper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * PDF 生成请求
 * <p>
 * 封装策略生成 PDF 所需的全部数据，作为 {@code LabelPrintStrategy.generatePdfFiles()} 的入参。
 * <p>
 * 包含：
 * <ul>
 *   <li>批次信息（batchId, batchNo）</li>
 *   <li>平台代码</li>
 *   <li>订单分组（按仓库+SKU）</li>
 *   <li>平台上下文（携带平台特有数据）</li>
 * </ul>
 *
 * @author system
 */
@Getter
@RequiredArgsConstructor
public class PdfGenerationRequest {

    /**
     * 批次ID
     */
    private final Long batchId;

    /**
     * 批次号
     */
    private final String batchNo;

    /**
     * 平台代码
     */
    private final String platform;

    /**
     * 订单分组（groupKey -> OrderGroup）
     */
    private final Map<String, OrderGrouper.OrderGroup> groups;

    /**
     * 平台上下文，携带平台特有数据
     */
    private final PlatformContext context;
}
