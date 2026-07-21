package com.erp.admin.order.converter;

import com.erp.admin.order.model.entity.ErpLabelBatch;
import com.erp.admin.order.model.entity.ErpLabelBatchFile;
import com.erp.admin.order.model.entity.ErpLabelBatchItem;
import com.erp.admin.order.model.vo.LabelBatchFileVO;
import com.erp.admin.order.model.vo.LabelBatchItemVO;
import com.erp.admin.order.model.vo.LabelBatchPageVO;
import com.erp.admin.order.model.vo.LabelBatchVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 面单批次模型转换器
 *
 * @author erp
 */
@Mapper
public interface LabelBatchConverter {

	LabelBatchConverter INSTANCE = Mappers.getMapper(LabelBatchConverter.class);

	/**
	 * ErpLabelBatch 转 LabelBatchVO
	 * 注意：不包含 files 和 failedItems，需要单独设置
	 *
	 * @param batch 批次实体
	 * @return 批次VO
	 */
	@Mapping(source = "id", target = "batchId")
	@Mapping(target = "files", ignore = true)
	@Mapping(target = "failedItems", ignore = true)
	LabelBatchVO batchToVO(ErpLabelBatch batch);

	/**
	 * ErpLabelBatchFile 转 LabelBatchFileVO
	 * 注意：downloadUrl 需要单独设置
	 *
	 * @param file 文件实体
	 * @return 文件VO
	 */
	@Mapping(source = "id", target = "fileId")
	@Mapping(target = "downloadUrl", ignore = true)
	LabelBatchFileVO fileToVO(ErpLabelBatchFile file);

	/**
	 * 批量转换文件列表
	 *
	 * @param files 文件实体列表
	 * @return 文件VO列表
	 */
	List<LabelBatchFileVO> filesToVOs(List<ErpLabelBatchFile> files);

	/**
	 * ErpLabelBatchItem 转 LabelBatchItemVO
	 *
	 * @param item 订单项实体
	 * @return 订单项VO
	 */
	@Mapping(source = "id", target = "itemId")
	LabelBatchItemVO itemToVO(ErpLabelBatchItem item);

	/**
	 * 批量转换订单项列表
	 *
	 * @param items 订单项实体列表
	 * @return 订单项VO列表
	 */
	List<LabelBatchItemVO> itemsToVOs(List<ErpLabelBatchItem> items);

	/**
	 * ErpLabelBatch 转 LabelBatchPageVO
	 * 用于分页列表，不包含文件和失败订单项详情
	 *
	 * @param batch 批次实体
	 * @return 批次分页VO
	 */
	@Mapping(source = "id", target = "batchId")
	LabelBatchPageVO batchToPageVO(ErpLabelBatch batch);

	/**
	 * 批量转换批次列表为分页VO
	 *
	 * @param batches 批次实体列表
	 * @return 批次分页VO列表
	 */
	List<LabelBatchPageVO> batchesToPageVOs(List<ErpLabelBatch> batches);
}
