package com.erp.admin.order.common;

import com.erp.admin.order.model.qo.ErpOrderQO;
import com.erp.admin.product.service.SkuMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

/**
 * 订单查询预处理器
 * <p>
 * 职责：
 * - 设置平台过滤条件
 * - skuCode → platformItemIds 预解析（通过 sku_mapping 实时查询）
 */
@Component
@RequiredArgsConstructor
public class OrderQueryPreprocessor {

    private final SkuMappingService skuMappingService;

    /**
     * 预处理查询对象
     *
     * @param qo           查询参数
     * @param platformCode 平台代码
     * @return 处理后的 QO
     */
    public ErpOrderQO preprocess(ErpOrderQO qo, String platformCode) {
        qo.setPlatform(platformCode);

        String skuCode = qo.getSkuCode();
        if (StringUtils.hasText(skuCode)) {
            qo.setPlatformItemIds(skuMappingService
                    .resolvePlatformItemIdsForQuery(Collections.singletonList(skuCode)));
        }
        return qo;
    }
}
