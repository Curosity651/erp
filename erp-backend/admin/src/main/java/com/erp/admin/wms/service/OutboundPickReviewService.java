package com.erp.admin.wms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskMapper;
import com.erp.admin.wms.mapper.WmsOutboundPickReviewMapper;
import com.erp.admin.wms.model.dto.OutboundPickReviewConfirmDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.entity.WmsOutboundPickReview;
import com.erp.admin.wms.model.vo.OutboundPickReviewSummaryVO;
import lombok.RequiredArgsConstructor;
import org.ballcat.common.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class OutboundPickReviewService {

	private static final long ALL_WAREHOUSES = 0L;
	private static final String COMPLETED = "COMPLETED";
	private static final String CANCELLED = "CANCELLED";
	private static final String PENDING = "PENDING";
	private static final String PICKING = "PICKING";
	private static final String PARTIAL_EXCEPTION = "PARTIAL_EXCEPTION";

	private final WmsFulfillmentPickTaskMapper taskMapper;
	private final WmsOutboundPickReviewMapper reviewMapper;
	private final TenantIdentityService tenantIdentityService;

	public OutboundPickReviewSummaryVO summary(LocalDate workDate, Long warehouseId) {
		Long tenantId = currentPlatformTenantId();
		LocalDate date = workDate == null ? LocalDate.now() : workDate;
		long scopeWarehouseId = normalizeWarehouseId(warehouseId);
		return buildSummary(tenantId, date, scopeWarehouseId);
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsOutboundPickReview confirm(OutboundPickReviewConfirmDTO dto, Long userId) {
		Assert.notNull(userId, "当前复核人不能为空");
		Long tenantId = currentPlatformTenantId();
		long warehouseId = normalizeWarehouseId(dto.getWarehouseId());
		OutboundPickReviewSummaryVO summary = buildSummary(tenantId, dto.getWorkDate(), warehouseId);
		Assert.isTrue(summary.getTotalTaskCount() > 0, "该日期没有拣货任务，不能完成复核");
		Assert.isTrue(Boolean.TRUE.equals(summary.getAllProcessed()),
				"仍有 " + summary.getUnprocessedTaskCount() + " 张拣货任务未处理完成");

		WmsOutboundPickReview review = findReview(tenantId, dto.getWorkDate(), warehouseId);
		LocalDateTime now = LocalDateTime.now();
		boolean creating = review == null;
		if (creating) {
			review = new WmsOutboundPickReview();
			review.setTenantId(tenantId);
			review.setReviewNo(newReviewNo(dto.getWorkDate(), now));
			review.setWorkDate(dto.getWorkDate());
			review.setWarehouseId(warehouseId);
			review.setCreateBy(userId);
			review.setCreateTime(now);
			review.setVersion(0);
		}
		else {
			review.setVersion((review.getVersion() == null ? 0 : review.getVersion()) + 1);
		}
		review.setReviewStatus(COMPLETED);
		review.setTaskCount(summary.getTotalTaskCount());
		review.setCompletedCount(summary.getCompletedTaskCount());
		review.setCancelledCount(summary.getCancelledTaskCount());
		review.setExceptionCount(summary.getExceptionTaskCount());
		review.setTaskSnapshotTime(maxTaskUpdateTime(tasks(dto.getWorkDate(), warehouseId)));
		review.setReviewedBy(userId);
		review.setReviewedTime(now);
		review.setRemark(trimToNull(dto.getRemark()));
		review.setUpdateBy(userId);
		review.setUpdateTime(now);
		if (creating) {
			Assert.isTrue(reviewMapper.insert(review) == 1, "出库复核记录创建失败");
		}
		else {
			Assert.isTrue(reviewMapper.updateById(review) == 1, "出库复核记录更新失败");
		}
		return review;
	}

	private OutboundPickReviewSummaryVO buildSummary(Long tenantId, LocalDate workDate,
			long warehouseId) {
		List<WmsFulfillmentPickTask> tasks = tasks(workDate, warehouseId);
		int completed = count(tasks, COMPLETED);
		int cancelled = count(tasks, CANCELLED);
		int pending = count(tasks, PENDING);
		int picking = count(tasks, PICKING);
		int exceptions = count(tasks, PARTIAL_EXCEPTION);
		int unprocessed = tasks.size() - completed - cancelled;
		LocalDateTime snapshotTime = maxTaskUpdateTime(tasks);
		WmsOutboundPickReview review = findReview(tenantId, workDate, warehouseId);

		OutboundPickReviewSummaryVO result = new OutboundPickReviewSummaryVO();
		result.setWorkDate(workDate);
		result.setWarehouseId(warehouseId == ALL_WAREHOUSES ? null : warehouseId);
		result.setTotalTaskCount(tasks.size());
		result.setCompletedTaskCount(completed);
		result.setCancelledTaskCount(cancelled);
		result.setPendingTaskCount(pending);
		result.setPickingTaskCount(picking);
		result.setExceptionTaskCount(exceptions);
		result.setUnprocessedTaskCount(unprocessed);
		result.setAllProcessed(!tasks.isEmpty() && unprocessed == 0);
		if (review != null) {
			result.setReviewId(review.getId());
			result.setReviewNo(review.getReviewNo());
			result.setReviewedBy(review.getReviewedBy());
			result.setReviewedTime(review.getReviewedTime());
			result.setRemark(review.getRemark());
		}
		result.setReviewCurrent(review != null
				&& COMPLETED.equals(review.getReviewStatus())
				&& Objects.equals(review.getTaskCount(), tasks.size())
				&& Objects.equals(review.getCompletedCount(), completed)
				&& Objects.equals(review.getCancelledCount(), cancelled)
				&& Objects.equals(review.getExceptionCount(), exceptions)
				&& Objects.equals(review.getTaskSnapshotTime(), snapshotTime));
		return result;
	}

	private List<WmsFulfillmentPickTask> tasks(LocalDate workDate, long warehouseId) {
		LocalDateTime start = LocalDateTime.of(workDate, LocalTime.MIN);
		LocalDateTime end = start.plusDays(1);
		return taskMapper.selectList(Wrappers.<WmsFulfillmentPickTask>lambdaQuery()
				.ge(WmsFulfillmentPickTask::getCreateTime, start)
				.lt(WmsFulfillmentPickTask::getCreateTime, end)
				.eq(warehouseId != ALL_WAREHOUSES,
						WmsFulfillmentPickTask::getWarehouseId, warehouseId)
				.orderByDesc(WmsFulfillmentPickTask::getCreateTime));
	}

	private WmsOutboundPickReview findReview(Long tenantId, LocalDate workDate,
			long warehouseId) {
		return reviewMapper.selectOne(Wrappers.<WmsOutboundPickReview>lambdaQuery()
				.eq(WmsOutboundPickReview::getTenantId, tenantId)
				.eq(WmsOutboundPickReview::getWorkDate, workDate)
				.eq(WmsOutboundPickReview::getWarehouseId, warehouseId));
	}

	private int count(List<WmsFulfillmentPickTask> tasks, String status) {
		return (int) tasks.stream().filter(task -> status.equals(task.getTaskStatus())).count();
	}

	private LocalDateTime maxTaskUpdateTime(List<WmsFulfillmentPickTask> tasks) {
		return tasks.stream().map(WmsFulfillmentPickTask::getUpdateTime)
				.filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
	}

	private long normalizeWarehouseId(Long warehouseId) {
		if (warehouseId == null) return ALL_WAREHOUSES;
		Assert.isTrue(warehouseId > 0, "仓库参数不正确");
		return warehouseId;
	}

	private Long currentPlatformTenantId() {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		if (!TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identity.getIdentityType())) {
			throw new BusinessException(403, "仅海外仓平台可以执行出库复核");
		}
		return identity.getTenantId();
	}

	private String newReviewNo(LocalDate workDate, LocalDateTime now) {
		return "OPR" + workDate.format(DateTimeFormatter.BASIC_ISO_DATE)
				+ now.format(DateTimeFormatter.ofPattern("HHmmss"))
				+ UUID.randomUUID().toString().substring(0, 4).toUpperCase();
	}

	private String trimToNull(String value) {
		if (value == null || value.trim().isEmpty()) return null;
		return value.trim();
	}
}
