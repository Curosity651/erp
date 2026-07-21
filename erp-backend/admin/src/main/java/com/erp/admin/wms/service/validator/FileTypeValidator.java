package com.erp.admin.wms.service.validator;

import com.erp.admin.system.model.entity.SysFile;
import com.erp.admin.system.service.SysFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件类型校验器
 * <p>
 * 用于校验采购单相关附件的文件类型
 *
 * @author erp
 */
@Component
@RequiredArgsConstructor
public class FileTypeValidator {

	private final SysFileService sysFileService;

	/**
	 * 合同文件允许的MIME类型
	 */
	private static final Set<String> CONTRACT_TYPES = new HashSet<>(Arrays.asList(
			"application/pdf",
			"application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
	));

	/**
	 * 付款凭证允许的MIME类型
	 */
	private static final Set<String> VOUCHER_TYPES = new HashSet<>(Arrays.asList(
			"application/pdf",
			"image/jpeg",
			"image/jpg",
			"image/png"
	));

	/**
	 * 质检报告允许的MIME类型
	 */
	private static final Set<String> QC_REPORT_TYPES = new HashSet<>(Arrays.asList(
			"application/pdf",
			"image/jpeg",
			"image/jpg",
			"image/png"
	));

	/**
	 * 校验合同文件类型
	 *
	 * @param fileId 文件ID
	 * @throws IllegalArgumentException 如果文件不存在或类型不支持
	 */
	public void validateContractFile(Long fileId) {
		SysFile file = sysFileService.getById(fileId);
		Assert.notNull(file, "合同文件不存在");
		Assert.isTrue(CONTRACT_TYPES.contains(file.getContentType().toLowerCase()),
				"合同文件仅支持 PDF、DOC、DOCX 格式");
	}

	/**
	 * 校验付款凭证文件类型
	 *
	 * @param fileId 文件ID
	 * @throws IllegalArgumentException 如果文件不存在或类型不支持
	 */
	public void validateVoucherFile(Long fileId) {
		SysFile file = sysFileService.getById(fileId);
		Assert.notNull(file, "付款凭证文件不存在");
		Assert.isTrue(VOUCHER_TYPES.contains(file.getContentType().toLowerCase()),
				"付款凭证仅支持 PDF、JPG、PNG 格式");
	}

	/**
	 * 校验质检报告文件类型
	 *
	 * @param fileId 文件ID
	 * @throws IllegalArgumentException 如果文件不存在或类型不支持
	 */
	public void validateQcReportFile(Long fileId) {
		SysFile file = sysFileService.getById(fileId);
		Assert.notNull(file, "质检报告文件不存在");
		Assert.isTrue(QC_REPORT_TYPES.contains(file.getContentType().toLowerCase()),
				"质检报告仅支持 PDF、JPG、PNG 格式");
	}

	/**
	 * 校验文件是否存在
	 *
	 * @param fileId 文件ID
	 * @throws IllegalArgumentException 如果文件不存在
	 */
	public void validateFileExists(Long fileId) {
		SysFile file = sysFileService.getById(fileId);
		Assert.notNull(file, "文件不存在");
	}

}
