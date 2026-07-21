package com.erp.admin.order.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("erp_label_batch_file")
@Schema(title = "面单打印批次-生成文件")
public class ErpLabelBatchFile {

    @TableId
    private Long id;

    private Long batchId;

    @Schema(title = "类型: WB_ORDER_PDF|WB_SUPPLY_PDF")
    private String type;

	private String destinationWarehouseId;

	private String destinationWarehouseName;

	private String erpSkuCode;

    private String erpSkuNo;

    private Integer skuCount;

    private String fileName;

    private String objectKey;

    private Integer pageCount;

    @Schema(title = "文件状态: PENDING|GENERATED|FAILED")
    private String status;

    @Schema(title = "失败原因")
    private String errorMsg;
}


