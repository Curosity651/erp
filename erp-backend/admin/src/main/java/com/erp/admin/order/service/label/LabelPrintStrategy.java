package com.erp.admin.order.service.label;

import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.service.label.model.OrderFailureInfo;
import com.erp.admin.order.service.label.model.PdfGenerationRequest;
import com.erp.admin.order.service.label.model.PlatformContext;
import com.erp.admin.product.model.entity.Sku;

import java.util.List;
import java.util.Map;

/**
 * 平台面单打印策略接口
 * <p>
 * 定义平台特有的面单打印行为契约。每个平台（Ozon、WB、Yandex等）实现此接口，
 * 由 {@link LabelPrintOrchestrator} 统一编排调用。
 * <p>
 * 策略模式的优势：
 * <ul>
 *   <li>新平台接入只需实现此接口，无需修改编排器</li>
 *   <li>平台逻辑隔离，互不影响</li>
 *   <li>便于单元测试</li>
 * </ul>
 *
 * @author system
 * @see LabelPrintOrchestrator
 */
public interface LabelPrintStrategy {

    /**
     * 获取平台代码
     * <p>
     * 用于策略注册和路由，需与 {@link com.erp.admin.platform.PlatformEnum} 的 code 一致。
     *
     * @return 平台代码，如 "ozon"、"wildberries"
     */
    String getPlatformCode();

    /**
     * 同步面单数据
     * <p>
     * 在打印前调用，负责从平台 API 获取缺失的面单数据并更新到数据库。
     * HTTP 调用在此方法内完成，编排器不关心具体实现。
     *
     * @param orders 待打印的订单列表
     * @return 平台上下文，可携带平台特有数据（如 WB 的 SupplyMap），供后续步骤使用
     */
    PlatformContext syncLabels(List<ErpOrder> orders);

    /**
     * 检查单个订单是否可打印
     * <p>
     * 定义平台特有的校验规则。编排器会遍历所有订单调用此方法进行分类。
     *
     * @param order      待检查的订单
     * @param skuMap     SKU 映射（skuCode -> Sku）
     * @param skuCodeMap platformItemId -> skuCode 映射
     * @param context    平台上下文，包含 syncLabels 阶段产生的数据
     * @return null 表示可打印；否则返回失败信息
     */
    OrderFailureInfo checkPrintability(ErpOrder order,
                                        Map<String, Sku> skuMap,
                                        Map<String, String> skuCodeMap,
                                        PlatformContext context);

    /**
     * 丰富仓库名称
     * <p>
     * 不同平台获取仓库名称的方式不同：
     * <ul>
     *   <li>WB：查询 WbOffice 表</li>
     *   <li>Ozon：从订单 rawJson 解析</li>
     * </ul>
     *
     * @param groups 订单分组（groupKey -> OrderGroup）
     */
    void enrichWarehouseNames(Map<String, OrderGrouper.OrderGroup> groups);

    /**
     * 生成 PDF 文件
     * <p>
     * 不同平台的 PDF 生成逻辑差异较大：
     * <ul>
     *   <li>WB：生成 2 个 PDF（订单面单 + Supply 面单），使用图片转 PDF</li>
     *   <li>Ozon：生成 1 个 PDF，使用 PDF 合并</li>
     * </ul>
     *
     * @param request PDF 生成请求，包含批次信息、分组数据和平台上下文
     */
    void generatePdfFiles(PdfGenerationRequest request);
}
