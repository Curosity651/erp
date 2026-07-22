package com.erp.admin.system.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.erp.admin.system.config.BucketConfig;
import com.erp.admin.system.config.OssBucketKeys;
import com.erp.admin.system.converter.SysFileConverter;
import com.erp.admin.system.mapper.SysFileMapper;
import com.erp.admin.system.model.dto.SysFileUploadDTO;
import com.erp.admin.system.model.entity.SysFile;
import com.erp.admin.system.model.vo.SysFileVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.mybatisplus.service.impl.ExtendServiceImpl;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 系统文件服务
 *
 * @author erp
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileService extends ExtendServiceImpl<SysFileMapper, SysFile> {

	private final OssService ossService;

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final TenantIdentityService tenantIdentityService;

	/**
	 * 允许的文件类型
	 */
	private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
			"application/pdf",
			"image/jpeg",
			"image/jpg",
			"image/png",
			"application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
	);

	/**
	 * 最大文件大小（10MB）
	 */
	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

	/**
	 * 保存文件元数据
	 * @param dto 上传数据传输对象
	 * @return 文件ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public Long saveFileMetadata(SysFileUploadDTO dto) {
		// 获取桶配置（同时校验 bucketKey 是否有效）
		BucketConfig bucketConfig = ossService.getBucketConfig(dto.getBucketKey());

		// 校验文件类型
		Assert.isTrue(ALLOWED_CONTENT_TYPES.contains(dto.getContentType().toLowerCase()),
				"不支持的文件类型: " + dto.getContentType());

		// 校验文件大小
		Assert.isTrue(dto.getFileSize() <= MAX_FILE_SIZE,
				"文件大小不能超过10MB");

		SysFile sysFile = SysFileConverter.INSTANCE.dtoToEntity(dto);

		// 设置桶名称和存储类型（从配置获取）
		sysFile.setBucketName(bucketConfig.getBucketName());

		this.save(sysFile);
		log.info("保存文件元数据成功, id={}, fileName={}, bucketKey={}",
				sysFile.getId(), sysFile.getFileName(), dto.getBucketKey());
		return sysFile.getId();
	}

	/**
	 * 获取文件信息
	 * @param fileId 文件ID
	 * @return 文件视图对象
	 */
	public SysFileVO getFileInfo(Long fileId) {
		SysFile sysFile = this.getById(fileId);
		if (sysFile == null) {
			return null;
		}

		SysFileVO vo = SysFileConverter.INSTANCE.entityToVo(sysFile);

		vo.setUrl(ossService.getDownloadUrl(resolveBucketKey(sysFile), sysFile.getObjectKey()));

		return vo;
	}

	/**
	 * 获取文件下载URL
	 * @param fileId 文件ID
	 * @return 下载URL
	 */
	public String getDownloadUrl(Long fileId) {
		SysFile sysFile = this.getById(fileId);
		Assert.notNull(sysFile, "文件不存在");

		return ossService.getDownloadUrl(resolveBucketKey(sysFile), sysFile.getObjectKey());
	}

	/**
	 * 删除文件
	 * @param fileId 文件ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public void deleteFile(Long fileId) {
		SysFile sysFile = this.getById(fileId);
		Assert.notNull(sysFile, "文件不存在");

		// 归属校验:仅文件创建人本人或管理员可删,防止本租户内任意用户删他人上传的文件
		boolean isAdmin = Boolean.TRUE.equals(tenantIdentityService.currentIdentity(null).getAdmin());
		Assert.isTrue(isAdmin || (sysFile.getCreateBy() != null && sysFile.getCreateBy().equals(currentUserId())),
				"无权删除他人上传的文件");

		// 删除OSS文件（通过 bucketName 反向查找配置）
		ossService.deleteObjectByBucketName(sysFile.getBucketName(), sysFile.getObjectKey());

		// 删除数据库记录
		this.removeById(fileId);
		log.info("删除文件成功, id={}, fileName={}", fileId, sysFile.getFileName());
	}

	/**
	 * 批量获取文件信息
	 * @param fileIds 文件ID列表
	 * @return 文件视图对象列表
	 */
	public List<SysFileVO> getFileInfoList(List<Long> fileIds) {
		if (fileIds == null || fileIds.isEmpty()) {
			return new ArrayList<>();
		}

		List<SysFile> files = this.listByIds(fileIds);
		return files.stream().map(file -> {
			SysFileVO vo = SysFileConverter.INSTANCE.entityToVo(file);
			vo.setUrl(ossService.getDownloadUrl(resolveBucketKey(file), file.getObjectKey()));
			return vo;
		}).collect(Collectors.toList());
	}

	/**
	 * 批量获取下载URL
	 * @param fileIds 文件ID列表
	 * @return 文件ID与下载URL的映射
	 */
	public Map<Long, String> batchGetDownloadUrls(List<Long> fileIds) {
		if (fileIds == null || fileIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<SysFile> files = this.listByIds(fileIds);
		Map<Long, String> result = new HashMap<>();

		for (SysFile file : files) {
			String url = ossService.getDownloadUrl(resolveBucketKey(file), file.getObjectKey());
			result.put(file.getId(), url);
		}

		return result;
	}


	/** 当前登录用户ID(取不到返回 null)。 */
	private Long currentUserId() {
		try { return principalAttributeAccessor.getUserId(); } catch (Exception ignore) { return null; }
	}

	private String resolveBucketKey(SysFile file) {
		if (file.getBucketKey() != null && !file.getBucketKey().trim().isEmpty()) {
			return file.getBucketKey();
		}
		return file.getObjectKey() != null && file.getObjectKey().startsWith("uploads/private/")
				? OssBucketKeys.PRIVATE_FILES : OssBucketKeys.PUBLIC_FILES;
	}
}
