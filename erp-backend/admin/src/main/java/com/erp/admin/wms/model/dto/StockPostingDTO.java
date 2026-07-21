package com.erp.admin.wms.model.dto;

import com.erp.admin.wms.model.enums.PostingType;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存过账请求DTO
 * 业务层调用 StockPostingService.post() 的入参
 *
 * @author erp
 */
@Data
@Builder
public class StockPostingDTO {

    /** 仓库ID（仓库级操作时填写，区域级操作时可为 null 或 0） */
    private Long warehouseId;

    /** 区域ID（存在区域级操作时填写，便于查询） */
    private Long regionId;

    /** 货主维（单据货主），用于过账头部货主级溯源；为空时由明细维度承载 */
    private Long erpTenantId;

    /** 服务商维（货架承租方），用于过账头部服务商级溯源，可空 */
    private Long wmsTenantId;

    /** 过账类型 */
    @NotNull(message = "过账类型不能为空")
    private PostingType postingType;

    /** 来源单据类型 */
    private String sourceType;

    /** 来源单据ID */
    private Long sourceId;

    /** 来源单据号 */
    private String sourceNo;

    /** 关联过账单ID(调拨配对用) */
    private Long relatedPostingId;

    /** 业务发生时间 */
    private LocalDateTime bizTime;

    /** 备注 */
    private String remark;

    /** 过账明细 */
    @NotEmpty(message = "过账明细不能为空")
    private List<StockPostingItemDTO> items;

}
