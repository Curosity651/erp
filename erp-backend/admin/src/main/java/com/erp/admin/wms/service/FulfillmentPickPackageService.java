package com.erp.admin.wms.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.system.config.OssBucketKeys;
import com.erp.admin.system.service.OssService;
import com.erp.admin.wms.mapper.WmsFulfillmentPickPackageMapper;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickPackage;
import com.erp.admin.wms.model.vo.FulfillmentPickPackageVO;
import com.erp.admin.wms.pickpackage.PickPackageArchive;
import com.erp.admin.wms.pickpackage.PickTaskPackageFiles;
import com.erp.admin.wms.pickpackage.PickTaskPackageRenderer;
import com.erp.admin.wms.pickpackage.PickTaskPackageSnapshot;
import com.erp.admin.wms.service.platform.PlatformLabelResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class FulfillmentPickPackageService {

	private static final int MAX_LABEL_BYTES = 50 * 1024 * 1024;
	private final FulfillmentPickingService pickingService;
	private final FulfillmentShippingService shippingService;
	private final WmsFulfillmentPickPackageMapper packageMapper;
	private final PickTaskPackageRenderer renderer;
	private final OssService ossService;

	public FulfillmentPickPackageVO generate(Long taskId, Long userId) {
		PickTaskPackageSnapshot snapshot = PickTaskPackageSnapshot.from(pickingService.detail(taskId));
		Assert.notNull(userId, "当前操作人不能为空");
		Assert.isTrue(userId.equals(snapshot.getOperatorId()), "只有当前拣货员可以生成任务文件包");
		WmsFulfillmentPickPackage existing = findSuccess(taskId, snapshot.getSnapshotHash());
		if (existing != null) return toVO(existing);

		LocalDateTime now = LocalDateTime.now();
		WmsFulfillmentPickPackage record = new WmsFulfillmentPickPackage();
		record.setBatchNo("FPP" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
				+ UUID.randomUUID().toString().substring(0, 6).toUpperCase());
		record.setTaskId(taskId);
		record.setSnapshotHash(snapshot.getSnapshotHash());
		record.setOrderCount(snapshot.getOrders().size());
		record.setTotalQuantity(snapshot.getTotalQuantity());
		record.setPackageStatus("BUILDING");
		record.setCreateBy(userId);
		record.setCreateTime(now);
		record.setUpdateTime(now);
		packageMapper.insert(record);
		try {
			Map<Long, byte[]> labels = new LinkedHashMap<>();
			for (PickTaskPackageSnapshot.Order order : snapshot.getOrders()) {
				PlatformLabelResult result = shippingService.printLabel(order.getFulfillmentOrderId(), userId);
				Assert.notNull(result, "面单获取失败：" + order.getSourceOrderNo());
				byte[] label;
				if ("MANUAL".equals(order.getPlatform())) {
					label = PickTaskPackageRenderer.simplePdf(java.util.Arrays.asList(
							"РУЧНАЯ ЭТИКЕТКА", "ЗАКАЗ: " + order.getSourceOrderNo(),
							"ЗАДАНИЕ: " + snapshot.getTaskNo()));
				}
				else {
					Assert.hasText(result.getLabelUrl(), "面单地址为空：" + order.getSourceOrderNo());
					label = download(result.getLabelUrl());
				}
				labels.put(order.getFulfillmentOrderId(), label);
			}
			PickTaskPackageFiles files = renderer.render(snapshot, labels, now);
			Assert.isTrue(PickPackageArchive.verify(files.getWarehouseZip(),
					PickPackageArchive.read(files.getWarehouseZip()).keySet()).isValid(), "俄文仓库包校验失败");
			Assert.isTrue(PickPackageArchive.verify(files.getArchiveZip(),
					PickPackageArchive.read(files.getArchiveZip()).keySet()).isValid(), "中文留底包校验失败");
			String base = "fulfillment-picking/packages/" + safe(snapshot.getTaskNo()) + "/"
					+ record.getBatchNo() + "/";
			record.setWarehouseFileName(files.getWarehouseFileName());
			record.setWarehouseObjectKey(ossService.putObject(OssBucketKeys.PRIVATE_FILES,
					files.getWarehouseZip(), base + files.getWarehouseFileName()));
			record.setWarehouseSha256(files.getWarehouseSha256());
			record.setArchiveFileName(files.getArchiveFileName());
			record.setArchiveObjectKey(ossService.putObject(OssBucketKeys.PRIVATE_FILES,
					files.getArchiveZip(), base + files.getArchiveFileName()));
			record.setArchiveSha256(files.getArchiveSha256());
			record.setPackageStatus("SUCCESS");
			record.setUpdateTime(LocalDateTime.now());
			packageMapper.updateById(record);
			return toVO(record);
		}
		catch (RuntimeException ex) {
			record.setPackageStatus("FAILED");
			record.setErrorMessage(abbreviate(ex.getMessage(), 1000));
			record.setUpdateTime(LocalDateTime.now());
			packageMapper.updateById(record);
			throw ex;
		}
	}

	public void requireSuccessfulPackage(Long taskId, Long userId) {
		PickTaskPackageSnapshot snapshot = PickTaskPackageSnapshot.from(pickingService.detail(taskId));
		Assert.notNull(userId, "当前操作人不能为空");
		Assert.isTrue(userId.equals(snapshot.getOperatorId()), "只有当前拣货员可以完成任务");
		Assert.notNull(findSuccess(taskId, snapshot.getSnapshotHash()),
				"请先导出拣货文件包，确认俄文仓库包和中文留底包均生成成功");
	}

	private WmsFulfillmentPickPackage findSuccess(Long taskId, String hash) {
		return packageMapper.selectOne(Wrappers.<WmsFulfillmentPickPackage>lambdaQuery()
				.eq(WmsFulfillmentPickPackage::getTaskId, taskId)
				.eq(WmsFulfillmentPickPackage::getSnapshotHash, hash)
				.eq(WmsFulfillmentPickPackage::getPackageStatus, "SUCCESS")
				.last("LIMIT 1"));
	}

	private FulfillmentPickPackageVO toVO(WmsFulfillmentPickPackage record) {
		FulfillmentPickPackageVO vo = new FulfillmentPickPackageVO();
		vo.setBatchNo(record.getBatchNo()); vo.setTaskId(record.getTaskId());
		vo.setSnapshotHash(record.getSnapshotHash());
		vo.setWarehouseFileName(record.getWarehouseFileName());
		vo.setWarehouseDownloadUrl(ossService.getDownloadUrl(OssBucketKeys.PRIVATE_FILES,
				record.getWarehouseObjectKey()));
		vo.setWarehouseSha256(record.getWarehouseSha256());
		vo.setArchiveFileName(record.getArchiveFileName());
		vo.setArchiveDownloadUrl(ossService.getDownloadUrl(OssBucketKeys.PRIVATE_FILES,
				record.getArchiveObjectKey()));
		vo.setArchiveSha256(record.getArchiveSha256());
		vo.setOrderCount(record.getOrderCount()); vo.setTotalQuantity(record.getTotalQuantity());
		vo.setGeneratedTime(record.getCreateTime());
		return vo;
	}

	private byte[] download(String address) {
		HttpURLConnection connection = null;
		try {
			URL url = new URL(address);
			Assert.isTrue("http".equalsIgnoreCase(url.getProtocol())
					|| "https".equalsIgnoreCase(url.getProtocol()), "面单地址协议不安全");
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(10_000); connection.setReadTimeout(60_000);
			connection.setInstanceFollowRedirects(true);
			int code = connection.getResponseCode();
			Assert.isTrue(code >= 200 && code < 300, "面单下载失败，HTTP " + code);
			try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				byte[] buffer = new byte[8192]; int total = 0; int count;
				while ((count = in.read(buffer)) >= 0) {
					total += count;
					Assert.isTrue(total <= MAX_LABEL_BYTES, "单个面单超过50MB限制");
					out.write(buffer, 0, count);
				}
				byte[] bytes = out.toByteArray();
				Assert.isTrue(bytes.length > 4 && bytes[0] == '%' && bytes[1] == 'P'
						&& bytes[2] == 'D' && bytes[3] == 'F', "平台返回的面单不是PDF文件");
				return bytes;
			}
		}
		catch (RuntimeException ex) { throw ex; }
		catch (Exception ex) { throw new IllegalStateException("面单下载失败：" + ex.getMessage(), ex); }
		finally { if (connection != null) connection.disconnect(); }
	}

	private String safe(String value) {
		return value.replaceAll("[^A-Za-z0-9._-]+", "_");
	}

	private String abbreviate(String value, int max) {
		if (value == null) return "生成失败";
		return value.length() <= max ? value : value.substring(0, max);
	}
}
