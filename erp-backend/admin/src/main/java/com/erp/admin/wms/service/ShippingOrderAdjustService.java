package com.erp.admin.wms.service;

import com.erp.admin.wms.mapper.ShippingOrderAdjustMapper;
import com.erp.admin.wms.model.entity.ShippingOrderAdjust;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 物流单仓库调整记录服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingOrderAdjustService extends ExtendServiceImpl<ShippingOrderAdjustMapper, ShippingOrderAdjust> {

    private static final DateTimeFormatter ADJUST_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 生成调整单号
     * 格式: SA + yyyyMMddHHmmss + 4位随机数
     */
    public String generateAdjustNo() {
        String timestamp = LocalDateTime.now().format(ADJUST_NO_FORMATTER);
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return "SA" + timestamp + random;
    }

    /**
     * 创建调整记录
     * @param shippingOrderId 物流单ID
     * @param fromRegionId 原区域ID
     * @param toRegionId 新区域ID
     * @param remark 备注
     * @return 调整记录实体
     */
    public ShippingOrderAdjust createAdjust(Long shippingOrderId, Long fromRegionId, Long toRegionId, String remark) {
        ShippingOrderAdjust adjust = new ShippingOrderAdjust();
        adjust.setAdjustNo(generateAdjustNo());
        adjust.setShippingOrderId(shippingOrderId);
        adjust.setFromRegionId(fromRegionId);
        adjust.setToRegionId(toRegionId);
        adjust.setRemark(remark);
        this.save(adjust);

        log.info("Created shipping order adjust, adjustNo={}, shippingOrderId={}, {} -> {}",
                adjust.getAdjustNo(), shippingOrderId, fromRegionId, toRegionId);
        return adjust;
    }

    /**
     * 更新过账单ID
     * @param adjustId 调整记录ID
     * @param stockPostingId 过账单ID
     */
    public void updateStockPostingId(Long adjustId, Long stockPostingId) {
        ShippingOrderAdjust adjust = this.getById(adjustId);
        if (adjust != null) {
            adjust.setStockPostingId(stockPostingId);
            this.updateById(adjust);
        }
    }

}
