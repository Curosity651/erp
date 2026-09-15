package com.erp.admin.wms.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 退货单明细（质检视角）。
 *
 * @author erp
 */
@Data
@Schema(title = "退货单明细")
public class ReturnOrderItemVO {

    private Long id;

    private String skuCode;

    private String warehouseSkuCode;

    private String skuName;

    private String platformOrderId;

    private String returnReason;

    @Schema(title = "电子类 → FAIL 是否强制拍照")
    private Boolean electronic;

    @Schema(title = "SKU标准每托数量")
    private Integer quantityPerPallet;

    @Schema(title = "应退数")
    private Integer expectedQty;

    @Schema(title = "实收数(收货后)")
    private Integer receivedQty;

    private Integer qualifiedQty;

    private Integer damagedQty;

    private Integer restockQty;

    private Integer reworkQty;

    private Integer scrapQty;

    private Integer reworkPassQty;

    private Integer reworkScrapQty;

    private String dispositionRemark;

    private String processedLocationCode;

    private String qualifiedZone;

    private String qualifiedLocationCode;

    private Long qualifiedPalletId;

    private Long qualifiedSlotId;

    private String qualifiedSlotCode;

    private String damagedLocationCode;

    private Long damagedPalletId;

    private Long damagedSlotId;

    private String damagedSlotCode;

    @Schema(title = "质检结果 PASS/FAIL")
    private String qcResult;

    @Schema(title = "回库分区 RETURN/STANDARD/DEFECTIVE")
    private String zone;

    @Schema(title = "品质 GOOD/DAMAGED")
    private String quality;

    @Schema(title = "回库库位")
    private String locationCode;

    @Schema(title = "质检备注")
    private String qcRemark;

    @Schema(title = "质检照片OSS文件ID列表(sys_file.id)")
    private List<Long> qcPhotoFileIds;

}
