package com.erp.admin.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.order.converter.LabelBatchConverter;
import com.erp.admin.order.mapper.ErpLabelBatchFileMapper;
import com.erp.admin.order.mapper.ErpLabelBatchItemMapper;
import com.erp.admin.order.mapper.ErpLabelBatchMapper;
import com.erp.admin.order.mapper.ErpOrderMapper;
import com.erp.admin.order.model.entity.ErpLabelBatch;
import com.erp.admin.order.model.entity.ErpLabelBatchFile;
import com.erp.admin.order.model.entity.ErpLabelBatchItem;
import com.erp.admin.order.model.entity.ErpOrder;
import com.erp.admin.order.model.qo.LabelBatchQO;
import com.erp.admin.order.model.vo.LabelBatchFileVO;
import com.erp.admin.order.model.vo.LabelBatchItemVO;
import com.erp.admin.order.model.vo.LabelBatchPageVO;
import com.erp.admin.order.model.vo.LabelBatchVO;
import com.erp.admin.order.util.LabelUtils;
import com.erp.admin.system.config.OssBucketKeys;
import com.erp.admin.system.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ballcat.common.model.domain.PageParam;
import org.ballcat.common.model.domain.PageResult;
import org.ballcat.mybatisplus.toolkit.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面单服务 - 通用能力服务
 * 提供批次管理、文件生成、状态更新等通用功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelService {

	private final ErpLabelBatchMapper labelBatchMapper;
	private final ErpLabelBatchItemMapper labelBatchItemMapper;
	private final ErpLabelBatchFileMapper labelBatchFileMapper;
	private final ErpOrderMapper erpOrderMapper;
	private final OssService ossService;

	/**
	 * 更新订单面单（独立事务）
	 * <p>
	 * 用于面单同步场景，确保 HTTP 调用在事务外，只有 DB 更新在事务内
	 *
	 * @param orderId     订单ID
	 * @param labelBase64 面单 Base64 数据
	 */
	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void updateOrderLabel(Long orderId, String labelBase64) {
		ErpOrder update = new ErpOrder();
		update.setId(orderId);
		update.setLabelBase64(labelBase64);
		update.setUpdateTime(LocalDateTime.now());
		erpOrderMapper.updateById(update);
		log.info("[LABEL] 更新订单面单成功: orderId={}", orderId);
	}

	/**
	 * 创建面单批次记录（短事务）
	 *
	 * @param platform  平台代码
	 * @param orders    订单列表
	 * @param remark    备注
	 * @param createdBy 创建人ID
	 * @return 批次记录
	 */
	@Transactional(rollbackFor = Exception.class)
	public ErpLabelBatch createBatch(String platform, List<ErpOrder> orders,
									 String remark, Long createdBy) {
		ErpLabelBatch batch = new ErpLabelBatch();
		batch.setBatchNo(LabelUtils.generateBatchNo());
		batch.setPlatform(platform);
		batch.setTotalOrders(orders.size());
		batch.setTotalFiles(0);
		batch.setStatus(LabelConstants.BATCH_STATUS_CREATED);
		batch.setRemark(remark);
		batch.setCreatedBy(createdBy);
		batch.setCreateTime(LocalDateTime.now());
		batch.setUpdateTime(LocalDateTime.now());
		labelBatchMapper.insert(batch);

		log.info("[LABEL] 创建批次成功: batchId={}, batchNo={}, platform={}, orderCount={}",
				batch.getId(), batch.getBatchNo(), platform, orders.size());

		return batch;
	}

	/**
	 * 批量创建批次项记录
	 *
	 * @param batchId 批次ID
	 * @param items   批次项列表
	 */
	@Transactional(rollbackFor = Exception.class)
	public void createBatchItems(Long batchId, List<ErpLabelBatchItem> items) {
		if (items == null || items.isEmpty()) {
			return;
		}

		for (ErpLabelBatchItem item : items) {
			item.setBatchId(batchId);
			item.setStatus(LabelConstants.ITEM_STATUS_PENDING);
			labelBatchItemMapper.insert(item);
		}

		log.info("[LABEL] 创建批次项成功: batchId={}, itemCount={}", batchId, items.size());
	}

	/**
	 * 上传 PDF 到 OSS (统一方法)
	 *
	 * @param pdfBytes PDF 字节数组
	 * @param batchNo  批次号
	 * @param fileName 文件名
	 * @return 对象存储 Key
	 */
	public String uploadPdfToOss(byte[] pdfBytes, String batchNo, String fileName) {
		try {
			String objectKey = LabelConstants.OSS_LABEL_PATH_PREFIX + batchNo + "/" + fileName;
			String result = ossService.putObject(OssBucketKeys.PUBLIC_FILES, pdfBytes, objectKey);

			log.info("[LABEL] PDF上传成功: batchNo={}, fileName={}, objectKey={}",
					batchNo, fileName, objectKey);

			return result;
		} catch (Exception e) {
			log.error("[LABEL] PDF上传失败: batchNo={}, fileName={}, error={}",
					batchNo, fileName, e.getMessage(), e);
			throw new RuntimeException("PDF上传失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 保存文件记录
	 *
	 * @param fileRecord 文件记录
	 * @return 文件ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long saveFileRecord(ErpLabelBatchFile fileRecord) {
		fileRecord.setStatus(LabelConstants.FILE_STATUS_GENERATED);
		labelBatchFileMapper.insert(fileRecord);

		log.info("[LABEL] 保存文件记录成功: fileId={}, batchId={}, fileName={}",
				fileRecord.getId(), fileRecord.getBatchId(), fileRecord.getFileName());

		return fileRecord.getId();
	}

	/**
	 * 批量更新订单项状态
	 *
	 * @param batchId  批次ID
	 * @param orderIds 订单ID列表
	 * @param status   状态
	 */
	@Transactional(rollbackFor = Exception.class)
	public void batchUpdateItemStatus(Long batchId, List<Long> orderIds, String status) {
		if (orderIds == null || orderIds.isEmpty()) {
			return;
		}

		labelBatchItemMapper.batchUpdateStatus(batchId, orderIds, status);

		log.debug("[LABEL] 批量更新订单项状态: batchId={}, orderCount={}, status={}",
				batchId, orderIds.size(), status);
	}

	/**
	 * 更新订单项状态为失败
	 *
	 * @param batchId  批次ID
	 * @param orderId  订单ID
	 * @param failCode 失败代码
	 * @param errorMsg 错误消息
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateItemFailure(Long batchId, Long orderId, String failCode,
								  String errorMsg) {
		labelBatchItemMapper.updateItemStatusAndFail(
				batchId, orderId, LabelConstants.ITEM_STATUS_FAILED, failCode, errorMsg
		);

		log.warn("[LABEL] 标记订单项失败: batchId={}, orderId={}, failCode={}, errorMsg={}",
				batchId, orderId, failCode, errorMsg);
	}

	/**
	 * 批量更新订单项的文件ID
	 *
	 * @param batchId  批次ID
	 * @param orderIds 订单ID列表
	 * @param fileId   文件ID
	 * @param fileType 文件类型 (ORDER/SUPPLY)
	 */
	@Transactional(rollbackFor = Exception.class)
	public void batchUpdateItemFileId(Long batchId, List<Long> orderIds, Long fileId,
									  String fileType) {
		if (orderIds == null || orderIds.isEmpty()) {
			return;
		}

		if ("ORDER".equals(fileType)) {
			labelBatchItemMapper.batchUpdateOrderFileId(batchId, orderIds, fileId);
		} else if ("SUPPLY".equals(fileType)) {
			labelBatchItemMapper.batchUpdateSupplyFileId(batchId, orderIds, fileId);
		}

		log.debug("[LABEL] 批量更新文件ID: batchId={}, orderCount={}, fileId={}, fileType={}",
				batchId, orderIds.size(), fileId, fileType);
	}


	/**
	 * 更新批次统计和状态
	 *
	 * @param batchId 批次ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateBatchStatistics(Long batchId) {
		ErpLabelBatch batch = labelBatchMapper.selectById(batchId);
		if (batch == null) {
			log.warn("[LABEL] 批次不存在: batchId={}", batchId);
			return;
		}

		// 统计文件
		List<ErpLabelBatchFile> files = labelBatchFileMapper.selectListByBatchId(batchId);
		int totalFiles = files.size();
		int fileSuccess = 0;
		int fileFailed = 0;
		for (ErpLabelBatchFile file : files) {
			if (LabelConstants.FILE_STATUS_GENERATED.equals(file.getStatus())) {
				fileSuccess++;
			} else if (LabelConstants.FILE_STATUS_FAILED.equals(file.getStatus())) {
				fileFailed++;
			}
		}

		// 统计订单项
		List<ErpLabelBatchItem> items = labelBatchItemMapper.selectByBatchId(batchId);
		int successCount = 0;
		int failedCount = 0;
		for (ErpLabelBatchItem item : items) {
			if (LabelConstants.ITEM_STATUS_SUCCESS.equals(item.getStatus())) {
				successCount++;
			} else if (LabelConstants.ITEM_STATUS_FAILED.equals(item.getStatus())) {
				failedCount++;
			}
		}

		// 更新批次
		batch.setTotalFiles(totalFiles);
		batch.setFileSuccess(fileSuccess);
		batch.setFileFailed(fileFailed);
		batch.setSuccessCount(successCount);
		batch.setFailedCount(failedCount);

		// 确定批次状态
		if (totalFiles == 0) {
			batch.setStatus(LabelConstants.BATCH_STATUS_FAILED);
		} else if (fileFailed == 0) {
			batch.setStatus(LabelConstants.BATCH_STATUS_GENERATED);
		} else if (fileSuccess > 0) {
			batch.setStatus(LabelConstants.BATCH_STATUS_PARTIAL);
		} else {
			batch.setStatus(LabelConstants.BATCH_STATUS_FAILED);
		}

		batch.setUpdateTime(LocalDateTime.now());
		labelBatchMapper.updateById(batch);

		log.info("[LABEL] 更新批次统计: batchId={}, status={}, totalFiles={}, fileSuccess={}, fileFailed={}, successCount={}, failedCount={}",
				batchId, batch.getStatus(), totalFiles, fileSuccess, fileFailed, successCount, failedCount);
	}

	/**
	 * 获取批次详情 VO（包含文件列表和失败订单项）
	 *
	 * @param batchId 批次ID
	 * @return 批次详情VO
	 */
	public LabelBatchVO getBatchVO(Long batchId) {
		ErpLabelBatch batch = labelBatchMapper.selectById(batchId);
		if (batch == null) {
			log.warn("[LABEL] 批次不存在: batchId={}", batchId);
			return null;
		}

		// 使用 MapStruct 转换批次基本信息
		LabelBatchVO vo = LabelBatchConverter.INSTANCE.batchToVO(batch);

		// 转换文件列表
		List<ErpLabelBatchFile> files = labelBatchFileMapper.selectListByBatchId(batchId);
		List<LabelBatchFileVO> fileVOs = LabelBatchConverter.INSTANCE.filesToVOs(files);
		// 设置下载链接
		for (int i = 0; i < files.size(); i++) {
			ErpLabelBatchFile file = files.get(i);
			LabelBatchFileVO fileVO = fileVOs.get(i);
			if (file.getObjectKey() != null) {
				fileVO.setDownloadUrl(ossService.getDownloadUrl(OssBucketKeys.PUBLIC_FILES, file.getObjectKey()));
			}
		}
		vo.setFiles(fileVOs);

		// 转换失败的订单项列表
		List<ErpLabelBatchItem> items = labelBatchItemMapper.selectByBatchId(batchId);
		List<LabelBatchItemVO> failedItemVOs = items.stream()
				.filter(item -> LabelConstants.ITEM_STATUS_FAILED.equals(item.getStatus()))
				.map(LabelBatchConverter.INSTANCE::itemToVO)
				.collect(java.util.stream.Collectors.toList());
		vo.setFailedItems(failedItemVOs);

		log.debug("[LABEL] 获取批次详情: batchId={}, files={}, failedItems={}", 
				batchId, fileVOs.size(), failedItemVOs.size());

		return vo;
	}

	/**
	 * 分页查询面单批次列表
	 *
	 * @param pageParam 分页参数
	 * @param qo 查询条件
	 * @return 分页结果
	 */
	public PageResult<LabelBatchPageVO> pageBatches(PageParam pageParam, LabelBatchQO qo) {
		LambdaQueryWrapper<ErpLabelBatch> wrapper = Wrappers.lambdaQuery(ErpLabelBatch.class)
				.eq(StringUtils.isNotBlank(qo.getPlatform()), ErpLabelBatch::getPlatform, qo.getPlatform())
				.ge(qo.getCreateTimeStart() != null, ErpLabelBatch::getCreateTime, qo.getCreateTimeStart())
				.le(qo.getCreateTimeEnd() != null, ErpLabelBatch::getCreateTime, qo.getCreateTimeEnd())
				.orderByDesc(ErpLabelBatch::getId);

		IPage<ErpLabelBatch> page = PageUtil.prodPage(pageParam);
		IPage<ErpLabelBatch> resultPage = labelBatchMapper.selectPage(page, wrapper);

		// 转换为 PageVO
		List<LabelBatchPageVO> records =
				LabelBatchConverter.INSTANCE.batchesToPageVOs(resultPage.getRecords());

		log.debug("[LABEL] 分页查询批次: platform={}, total={}, records={}",
				qo.getPlatform(), resultPage.getTotal(), records.size());

		return new PageResult<>(records, resultPage.getTotal());
	}
}

