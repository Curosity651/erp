package com.erp.admin.system.controller;

import com.erp.admin.system.config.OssBucketKeys;
import com.erp.admin.system.model.dto.SysFileUploadDTO;
import com.erp.admin.system.model.vo.OssPostSignatureVO;
import com.erp.admin.system.model.vo.SysFileVO;
import com.erp.admin.system.service.OssService;
import com.erp.admin.system.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.log.operation.annotation.OperationLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统文件管理控制器
 *
 * @author erp
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/file")
@Tag(name = "系统文件管理", description = "通用文件上传下载管理接口")
public class SysFileController {

	private final OssService ossService;

	private final SysFileService sysFileService;

	/**
	 * 获取上传签名
	 * @param bucketKey 桶别名，如 public-files、private-docs
	 * @return 上传签名信息
	 */
	@GetMapping("/upload-signature")
	@Operation(summary = "获取上传签名", description = "获取OSS直传签名，通过 bucketKey 指定目标桶")
	public ApiResult<OssPostSignatureVO> getUploadSignature(
			@RequestParam(name = "bucketKey", defaultValue = OssBucketKeys.PUBLIC_FILES) String bucketKey) {
		OssPostSignatureVO signature = ossService.getPostSignature(bucketKey);
		return ApiResult.ok(signature);
	}

	/**
	 * 保存文件元数据
	 * @param dto 文件上传数据
	 * @return 文件ID
	 */
	@PostMapping
	@Operation(summary = "保存文件元数据", description = "前端直传OSS成功后，调用此接口保存文件元数据")
	@OperationLog(bizType = "系统文件管理", successMessage = "上传成功")
	public ApiResult<Long> saveFileMetadata(@Validated @RequestBody SysFileUploadDTO dto) {
		Long fileId = sysFileService.saveFileMetadata(dto);
		return ApiResult.ok(fileId);
	}

	/**
	 * 获取文件信息
	 * @param id 文件ID
	 * @return 文件信息
	 */
	@GetMapping("/info")
	@Operation(summary = "获取文件信息")
	public ApiResult<SysFileVO> getFileInfo(@RequestParam Long id) {
		SysFileVO fileInfo = sysFileService.getFileInfo(id);
		return ApiResult.ok(fileInfo);
	}

	/**
	 * 获取下载URL
	 * @param id 文件ID
	 * @return 下载URL（公有桶返回直接URL，私有桶返回签名URL）
	 */
	@GetMapping("/download-url")
	@Operation(summary = "获取下载URL", description = "公有桶返回直接URL，私有桶返回签名URL（有效期30分钟）")
	public ApiResult<String> getDownloadUrl(@RequestParam Long id) {
		String downloadUrl = sysFileService.getDownloadUrl(id);
		return ApiResult.ok(downloadUrl);
	}

	/**
	 * 删除文件
	 * @param id 文件ID
	 * @return 操作结果
	 */
	@DeleteMapping
	@Operation(summary = "删除文件")
	@OperationLog(bizType = "系统文件管理", successMessage = "删除成功")
	public ApiResult<Void> deleteFile(@RequestParam Long id) {
		sysFileService.deleteFile(id);
		return ApiResult.ok();
	}

	/**
	 * 批量获取下载URL
	 * @param ids 文件ID列表
	 * @return 文件ID与下载URL的映射
	 */
	@PostMapping("/batch-download-urls")
	@Operation(summary = "批量获取下载URL", description = "批量获取文件下载URL，公有桶返回直接URL，私有桶返回签名URL")
	public ApiResult<Map<Long, String>> batchGetDownloadUrls(@RequestBody List<Long> ids) {
		Map<Long, String> urls = sysFileService.batchGetDownloadUrls(ids);
		return ApiResult.ok(urls);
	}

}
