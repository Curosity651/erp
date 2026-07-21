package com.erp.admin.order.model.vo;

@lombok.Data
public class LabelBatchFileVO {

    private Long fileId;

    private String type;

    private String destinationWarehouseId;

	private String destinationWarehouseName;

	private String erpSkuCode;

    private String erpSkuNo;

    private Integer skuCount;

    private String fileName;

    private String objectKey;

    private Integer pageCount;

    private String downloadUrl;
}


