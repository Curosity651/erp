package com.erp.admin.wms.mapper;

import com.erp.admin.wms.model.entity.PurchaseOrderFile;
import com.erp.admin.wms.model.vo.FileInfoVO;
import com.erp.admin.wms.model.vo.PurchaseOrderFileVO;
import org.apache.ibatis.annotations.Param;
import org.ballcat.mybatisplus.conditions.query.LambdaQueryWrapperX;
import org.ballcat.mybatisplus.mapper.ExtendMapper;
import org.ballcat.mybatisplus.toolkit.WrappersX;

import java.util.List;

/**
 * 采购单附件Mapper
 *
 * @author erp
 */
public interface PurchaseOrderFileMapper extends ExtendMapper<PurchaseOrderFile> {

	/**
	 * 查询采购单附件列表（关联文件信息）
	 * @param purchaseOrderId 采购单ID
	 * @return 附件VO列表
	 */
	List<PurchaseOrderFileVO> selectVoListByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);

	/**
	 * 查询采购单指定类型的附件（返回FileInfoVO）
	 * @param purchaseOrderId 采购单ID
	 * @param fileType 文件类型
	 * @return 文件信息VO
	 */
	FileInfoVO selectFileInfoByOrderIdAndType(@Param("purchaseOrderId") Long purchaseOrderId,
											  @Param("fileType") String fileType);

	/**
	 * 查询采购单指定类型的附件列表（返回FileInfoVO列表）
	 * @param purchaseOrderId 采购单ID
	 * @param fileType 文件类型
	 * @return 文件信息VO列表
	 */
	List<FileInfoVO> selectFileInfoListByOrderIdAndType(@Param("purchaseOrderId") Long purchaseOrderId,
														@Param("fileType") String fileType);

	/**
	 * 查询采购单指定类型的附件
	 * @param purchaseOrderId 采购单ID
	 * @param fileType 文件类型
	 * @return 附件实体
	 */
	default PurchaseOrderFile selectByOrderIdAndType(Long purchaseOrderId, String fileType) {
		LambdaQueryWrapperX<PurchaseOrderFile> wrapper = WrappersX.lambdaQueryX(PurchaseOrderFile.class)
				.eq(PurchaseOrderFile::getPurchaseOrderId, purchaseOrderId)
				.eq(PurchaseOrderFile::getFileType, fileType);
		return this.selectOne(wrapper);
	}

	/**
	 * 删除采购单的所有附件
	 * @param purchaseOrderId 采购单ID
	 * @return 删除数量
	 */
	default int deleteByPurchaseOrderId(Long purchaseOrderId) {
		LambdaQueryWrapperX<PurchaseOrderFile> wrapper = WrappersX.lambdaQueryX(PurchaseOrderFile.class)
				.eq(PurchaseOrderFile::getPurchaseOrderId, purchaseOrderId);
		return this.delete(wrapper);
	}

}
