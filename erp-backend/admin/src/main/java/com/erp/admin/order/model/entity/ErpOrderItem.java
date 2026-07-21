package com.erp.admin.order.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("erp_order_item")
public class ErpOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;

	/**
     * 关联 erp_order.id
     */
    private Long orderId;

	/**
     * 平台商品ID (WB article / Ozon offerId / Yandex offerId).
     */
    private String platformItemId;

	/**
     * SKU 编码（订单同步时落库，出库/库存以此为准）
     */
    private String skuCode;

	/**
     * 数量
     */
    private Integer quantity;

	/**
     * 单价（跟随主表币种）
     */
    private BigDecimal itemPrice;

	/**
     * 行小计（跟随主表币种）
     */
    private BigDecimal itemAmount;

	/**
     * 行小计(RUB)
     */
    private BigDecimal itemAmountRub;

	/**
     * 已退货数量
     */
    private Integer returnedQuantity;

	/**
     * 创建时间
     */
    private LocalDateTime createTime;

	/**
     * 更新时间
     */
    private LocalDateTime updateTime;
}