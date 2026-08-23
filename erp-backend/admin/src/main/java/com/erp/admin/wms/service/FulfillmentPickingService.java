package com.erp.admin.wms.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskLineMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskOrderMapper;
import com.erp.admin.wms.mapper.WmsInventoryReservationMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.dto.FulfillmentPickScanDTO;
import com.erp.admin.wms.model.dto.FulfillmentPickTaskQueryDTO;
import com.erp.admin.wms.model.dto.FulfillmentPickExceptionDTO;
import com.erp.admin.wms.model.entity.WmsFulfillmentOrder;
import com.erp.admin.wms.model.entity.WmsFulfillmentItem;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTask;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskLine;
import com.erp.admin.wms.model.entity.WmsFulfillmentPickTaskOrder;
import com.erp.admin.wms.model.entity.WmsInventoryReservation;
import com.erp.admin.wms.model.entity.WmsLocation;
import com.erp.admin.wms.model.enums.FulfillmentStatus;
import com.erp.admin.wms.model.vo.FulfillmentBatchResultVO;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskDetailVO;
import com.erp.admin.wms.model.vo.FulfillmentPickCurrentOrderVO;
import com.erp.admin.wms.model.vo.FulfillmentPickTaskOrderVO;
import com.erp.admin.wms.service.platform.PlatformActionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class FulfillmentPickingService {
	private final WmsFulfillmentOrderMapper orderMapper;
	private final FulfillmentPlatformActionService platformActionService;
	private final FulfillmentProgressService progressService;
	private WmsFulfillmentPickTaskMapper taskMapper;
	private WmsFulfillmentPickTaskOrderMapper taskOrderMapper;
	private WmsFulfillmentPickTaskLineMapper taskLineMapper;
	private WmsInventoryReservationMapper reservationMapper;
	private WmsFulfillmentItemMapper itemMapper;
	private WmsLocationMapper locationMapper;
	@Autowired
	private FulfillmentStatusSyncService statusSyncService;

	public FulfillmentPickingService(WmsFulfillmentOrderMapper orderMapper,
			FulfillmentPlatformActionService platformActionService) {
		this(orderMapper, platformActionService, null);
	}

	public FulfillmentPickingService(WmsFulfillmentOrderMapper orderMapper,
			FulfillmentPlatformActionService platformActionService,
			FulfillmentProgressService progressService) {
		this.orderMapper = orderMapper;
		this.platformActionService = platformActionService;
		this.progressService = progressService;
	}

	@Autowired
	public FulfillmentPickingService(WmsFulfillmentOrderMapper orderMapper,
			FulfillmentPlatformActionService platformActionService,
			WmsFulfillmentPickTaskMapper taskMapper,
			WmsFulfillmentPickTaskOrderMapper taskOrderMapper,
			WmsFulfillmentPickTaskLineMapper taskLineMapper,
			WmsInventoryReservationMapper reservationMapper,
			WmsFulfillmentItemMapper itemMapper, WmsLocationMapper locationMapper,
			FulfillmentProgressService progressService) {
		this(orderMapper, platformActionService, progressService);
		this.taskMapper = taskMapper;
		this.taskOrderMapper = taskOrderMapper;
		this.taskLineMapper = taskLineMapper;
		this.reservationMapper = reservationMapper;
		this.itemMapper = itemMapper;
		this.locationMapper = locationMapper;
	}

	public FulfillmentBatchResultVO accept(List<Long> fulfillmentOrderIds) {
		Assert.notEmpty(fulfillmentOrderIds, "请选择待下架订单");
		FulfillmentBatchResultVO result = new FulfillmentBatchResultVO();
		for (Long orderId : fulfillmentOrderIds) {
			try {
				acceptOne(orderId);
				result.addSuccess(orderId);
			}
			catch (RuntimeException ex) {
				result.addFailure(orderId, message(ex));
			}
		}
		return result;
	}

	public List<WmsFulfillmentOrder> listShelfOrders() {
		return listShelfOrders(null, null, null, null, null);
	}

	public List<WmsFulfillmentOrder> listShelfOrders(LocalDateTime startTime,
			LocalDateTime endTime, Long erpTenantId, Long logisticsProductId,
			Long warehouseId) {
		return orderMapper.selectList(Wrappers.<WmsFulfillmentOrder>lambdaQuery()
				.in(WmsFulfillmentOrder::getFulfillmentStatus, FulfillmentStatus.WAITING_SHELF,
						FulfillmentStatus.WAITING_PICK, FulfillmentStatus.PICKING)
				.ge(startTime != null, WmsFulfillmentOrder::getCreateTime, startTime)
				.le(endTime != null, WmsFulfillmentOrder::getCreateTime, endTime)
				.eq(erpTenantId != null, WmsFulfillmentOrder::getErpTenantId, erpTenantId)
				.eq(logisticsProductId != null, WmsFulfillmentOrder::getLogisticsProductId,
						logisticsProductId)
				.eq(warehouseId != null, WmsFulfillmentOrder::getWarehouseId, warehouseId)
				.orderByAsc(WmsFulfillmentOrder::getCreateTime));
	}

	@Transactional(rollbackFor = Exception.class)
	public WmsFulfillmentPickTask createTask(List<Long> orderIds, Long operatorId) {
		requireTaskDependencies();
		Assert.notEmpty(orderIds, "请选择待拣订单");
		List<WmsFulfillmentOrder> orders = new ArrayList<>();
		for (Long id : orderIds) {
			WmsFulfillmentOrder order = orderMapper.selectForUpdate(id);
			Assert.notNull(order, "履约订单不存在");
			Assert.isTrue(order.getFulfillmentStatus() == FulfillmentStatus.WAITING_PICK,
					"订单不是待拣货状态：" + order.getFulfillmentNo());
			orders.add(order);
		}
		WmsFulfillmentOrder first = orders.get(0);
		for (WmsFulfillmentOrder order : orders) {
			Assert.isTrue(first.getWarehouseId().equals(order.getWarehouseId()), "批量任务必须属于同一仓库");
		}

		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setTaskNo("FPT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
				+ UUID.randomUUID().toString().substring(0, 6).toUpperCase());
		task.setWarehouseId(first.getWarehouseId());
		task.setTaskStatus("PENDING");
		task.setOrderCount(orders.size());
		task.setTotalQuantity(0);
		task.setOperatorId(null);
		Assert.isTrue(taskMapper.insert(task) == 1, "拣货任务创建失败");

		int total = 0;
		int lineSequence = 1;
		for (int index = 0; index < orders.size(); index++) {
			WmsFulfillmentOrder order = orders.get(index);
			WmsFulfillmentPickTaskOrder taskOrder = new WmsFulfillmentPickTaskOrder();
			taskOrder.setTaskId(task.getId());
			taskOrder.setFulfillmentOrderId(order.getId());
			taskOrder.setSequenceNo(index + 1);
			taskOrder.setOrderStatus("PENDING");
			taskOrderMapper.insert(taskOrder);
			List<WmsInventoryReservation> reservations = reservationMapper.selectList(
					Wrappers.<WmsInventoryReservation>lambdaQuery()
							.eq(WmsInventoryReservation::getFulfillmentOrderId, order.getId())
							.eq(WmsInventoryReservation::getReservationStatus, "RESERVED")
							.orderByAsc(WmsInventoryReservation::getLocationId));
			Assert.notEmpty(reservations, "订单没有有效库存预占：" + order.getFulfillmentNo());
			for (WmsInventoryReservation reservation : reservations) {
				WmsFulfillmentItem item = itemMapper.selectById(reservation.getFulfillmentItemId());
				WmsLocation location = locationMapper.selectById(reservation.getLocationId());
				Assert.notNull(item, "履约商品不存在");
				Assert.notNull(location, "预占库位不存在");
				WmsFulfillmentPickTaskLine line = new WmsFulfillmentPickTaskLine();
				line.setTaskId(task.getId());
				line.setFulfillmentOrderId(order.getId());
				line.setReservationId(reservation.getId());
				line.setInventoryId(reservation.getInventoryId());
				line.setLocationId(reservation.getLocationId());
				line.setLocationCode(location.getLocationCode());
				line.setFulfillmentItemId(item.getId());
				line.setSkuCode(item.getSkuCode());
				line.setWarehouseSkuCode(item.getWarehouseSkuCode());
				line.setSequenceNo(lineSequence++);
				line.setPlannedQuantity(reservation.getQuantity());
				line.setPickedQuantity(0);
				line.setLineStatus("PENDING");
				line.setVersion(0);
				taskLineMapper.insert(line);
				total += reservation.getQuantity();
			}
		}
		task.setTotalQuantity(total);
		taskMapper.updateById(task);
		return task;
	}

	public List<WmsFulfillmentPickTask> listTasks() {
		return listTasks(new FulfillmentPickTaskQueryDTO());
	}

	public List<WmsFulfillmentPickTask> listTasks(FulfillmentPickTaskQueryDTO query) {
		requireTaskDependencies();
		FulfillmentPickTaskQueryDTO filter = query == null
				? new FulfillmentPickTaskQueryDTO() : query;
		List<WmsFulfillmentPickTask> tasks = taskMapper.selectList(Wrappers.<WmsFulfillmentPickTask>lambdaQuery()
				.like(filter.getTaskNo() != null && !filter.getTaskNo().trim().isEmpty(),
						WmsFulfillmentPickTask::getTaskNo, filter.getTaskNo())
				.eq(filter.getWarehouseId() != null, WmsFulfillmentPickTask::getWarehouseId,
						filter.getWarehouseId())
				.eq(filter.getTaskStatus() != null && !filter.getTaskStatus().trim().isEmpty(),
						WmsFulfillmentPickTask::getTaskStatus, filter.getTaskStatus())
				.eq(filter.getOperatorId() != null, WmsFulfillmentPickTask::getOperatorId,
						filter.getOperatorId())
				.ge(filter.getStartTime() != null, WmsFulfillmentPickTask::getCreateTime,
						filter.getStartTime())
				.le(filter.getEndTime() != null, WmsFulfillmentPickTask::getCreateTime,
						filter.getEndTime())
				.orderByDesc(WmsFulfillmentPickTask::getCreateTime));
		for (WmsFulfillmentPickTask task : tasks) {
			List<WmsFulfillmentPickTaskOrder> taskOrders = taskOrderMapper.selectList(
					Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
							.eq(WmsFulfillmentPickTaskOrder::getTaskId, task.getId()));
			int completed = 0;
			int exceptions = 0;
			for (WmsFulfillmentPickTaskOrder taskOrder : taskOrders) {
				if ("COMPLETED".equals(taskOrder.getOrderStatus())
						|| "CANCELLED".equals(taskOrder.getOrderStatus())) completed++;
				if ("EXCEPTION".equals(taskOrder.getOrderStatus())) exceptions++;
			}
			task.setCompletedOrderCount(completed);
			task.setExceptionOrderCount(exceptions);
		}
		return tasks;
	}

	@Transactional(rollbackFor = Exception.class)
	public void claimTask(Long taskId, Long userId) {
		requireTaskDependencies();
		Assert.notNull(userId, "当前操作人不能为空");
		WmsFulfillmentPickTask task = taskMapper.selectById(taskId);
		Assert.notNull(task, "拣货任务不存在");
		Assert.isTrue("PENDING".equals(task.getTaskStatus()), "任务已被领取或不能领取");
		Assert.isTrue(taskMapper.claim(taskId, userId, LocalDateTime.now()) == 1,
				"任务已被其他员工领取，请刷新后重试");
		List<WmsFulfillmentPickTaskOrder> orders = taskOrderMapper.selectList(
				Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
						.eq(WmsFulfillmentPickTaskOrder::getTaskId, taskId));
		for (WmsFulfillmentPickTaskOrder taskOrder : orders) {
			WmsFulfillmentOrder order = orderMapper.selectForUpdate(taskOrder.getFulfillmentOrderId());
			Assert.notNull(order, "履约订单不存在");
			if (order.getFulfillmentStatus() == FulfillmentStatus.WAITING_PICK) {
				Assert.isTrue(orderMapper.transit(order.getId(), FulfillmentStatus.WAITING_PICK,
						FulfillmentStatus.PICKING) == 1, "订单状态已变化，请刷新后重试");
				syncProgress(order, FulfillmentStatus.PICKING);
			}
			else {
				Assert.isTrue(order.getFulfillmentStatus() == FulfillmentStatus.PICKING
								|| order.getFulfillmentStatus() == FulfillmentStatus.WAITING_PACK
								|| order.getFulfillmentStatus() == FulfillmentStatus.PACKED,
						"任务包含不可拣货订单：" + order.getFulfillmentNo());
			}
		}
	}

	public void releaseTask(Long taskId, Long userId) {
		requireTaskDependencies();
		Assert.isTrue(taskMapper.release(taskId, userId) == 1,
				"只有当前拣货员可以释放进行中的任务");
	}

	public void transferTask(Long taskId, Long targetUserId) {
		requireTaskDependencies();
		Assert.notNull(targetUserId, "目标拣货员不能为空");
		Assert.isTrue(taskMapper.transfer(taskId, targetUserId, LocalDateTime.now()) == 1,
				"任务状态已变化，请刷新后重试");
	}

	public FulfillmentPickTaskDetailVO detail(Long taskId) {
		return detail(taskId, null);
	}

	public FulfillmentPickTaskDetailVO detail(Long taskId, Long selectedOrderId) {
		requireTaskDependencies();
		WmsFulfillmentPickTask task = taskMapper.selectById(taskId);
		Assert.notNull(task, "拣货任务不存在");
		FulfillmentPickTaskDetailVO result = new FulfillmentPickTaskDetailVO();
		result.setTask(task);
		List<WmsFulfillmentPickTaskOrder> orders = taskOrderMapper.selectList(Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
				.eq(WmsFulfillmentPickTaskOrder::getTaskId, taskId)
				.orderByAsc(WmsFulfillmentPickTaskOrder::getSequenceNo));
		List<WmsFulfillmentPickTaskLine> lines = taskLineMapper.selectList(Wrappers.<WmsFulfillmentPickTaskLine>lambdaQuery()
				.eq(WmsFulfillmentPickTaskLine::getTaskId, taskId)
				.orderByAsc(WmsFulfillmentPickTaskLine::getSequenceNo));
		result.setOrders(orders);
		result.setLines(lines);
		List<FulfillmentPickTaskOrderVO> queue = new ArrayList<>();
		FulfillmentPickTaskOrderVO selected = null;
		for (WmsFulfillmentPickTaskOrder candidate : orders) {
			WmsFulfillmentOrder fulfillmentOrder = orderMapper.selectById(candidate.getFulfillmentOrderId());
			List<WmsFulfillmentPickTaskLine> route = new ArrayList<>();
			for (WmsFulfillmentPickTaskLine line : lines) {
				if (candidate.getFulfillmentOrderId().equals(line.getFulfillmentOrderId())) route.add(line);
			}
			FulfillmentPickTaskOrderVO queueItem = new FulfillmentPickTaskOrderVO();
			queueItem.setTaskOrder(candidate);
			queueItem.setFulfillmentOrder(fulfillmentOrder);
			queueItem.setRouteLines(route);
			queueItem.setFirstLocationCode(route.isEmpty() ? null : route.get(0).getLocationCode());
			Set<String> skus = new HashSet<>();
			int total = 0;
			int picked = 0;
			for (WmsFulfillmentPickTaskLine line : route) {
				skus.add(line.getWarehouseSkuCode());
				total += line.getPlannedQuantity() == null ? 0 : line.getPlannedQuantity();
				picked += line.getPickedQuantity() == null ? 0 : line.getPickedQuantity();
			}
			queueItem.setSkuCount(skus.size());
			queueItem.setTotalQuantity(total);
			queueItem.setPickedQuantity(picked);
			queue.add(queueItem);
			if (selectedOrderId != null && selectedOrderId.equals(candidate.getFulfillmentOrderId())) {
				selected = queueItem;
			}
		}
		queue.sort((left, right) -> compareLocation(left.getFirstLocationCode(),
				right.getFirstLocationCode()));
		result.setOrderQueue(queue);
		if (selected == null) {
			for (FulfillmentPickTaskOrderVO candidate : queue) {
				String status = candidate.getTaskOrder().getOrderStatus();
				if (!"COMPLETED".equals(status) && !"CANCELLED".equals(status)
						&& !"EXCEPTION".equals(status)) {
					selected = candidate;
					break;
				}
			}
		}
		if (selected != null) {
			FulfillmentPickCurrentOrderVO current = new FulfillmentPickCurrentOrderVO();
			current.setTaskOrder(selected.getTaskOrder());
			current.setFulfillmentOrder(selected.getFulfillmentOrder());
			current.setRouteLines(selected.getRouteLines());
			result.setCurrentOrder(current);
		}
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public void completePackedOrder(Long fulfillmentOrderId) {
		requireTaskDependencies();
		List<WmsFulfillmentPickTaskOrder> matches = taskOrderMapper.selectList(
				Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
						.eq(WmsFulfillmentPickTaskOrder::getFulfillmentOrderId, fulfillmentOrderId));
		Assert.isTrue(matches.size() == 1, "履约订单未关联唯一拣货任务");
		WmsFulfillmentPickTaskOrder taskOrder = matches.get(0);
		Assert.isTrue("WAITING_LABEL".equals(taskOrder.getOrderStatus()), "任务订单尚未取齐或已完成");
		taskOrder.setOrderStatus("COMPLETED");
		taskOrder.setCompletedTime(LocalDateTime.now());
		taskOrderMapper.updateById(taskOrder);
		long pending = taskOrderMapper.selectCount(Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
				.eq(WmsFulfillmentPickTaskOrder::getTaskId, taskOrder.getTaskId())
				.notIn(WmsFulfillmentPickTaskOrder::getOrderStatus, "COMPLETED", "CANCELLED"));
		if (pending == 0) {
			WmsFulfillmentPickTask task = taskMapper.selectById(taskOrder.getTaskId());
			Assert.notNull(task, "拣货任务不存在");
			task.setTaskStatus("COMPLETED");
			taskMapper.updateById(task);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void markException(Long taskId, Long fulfillmentOrderId,
			FulfillmentPickExceptionDTO dto, Long userId) {
		requireTaskDependencies();
		WmsFulfillmentPickTask task = taskMapper.selectById(taskId);
		Assert.notNull(task, "拣货任务不存在");
		requireOperator(task, userId);
		WmsFulfillmentPickTaskOrder taskOrder = findTaskOrder(taskId, fulfillmentOrderId);
		Assert.isTrue(!"COMPLETED".equals(taskOrder.getOrderStatus())
						&& !"CANCELLED".equals(taskOrder.getOrderStatus())
						&& !"EXCEPTION".equals(taskOrder.getOrderStatus()),
				"当前订单状态不允许标记异常");
		WmsFulfillmentOrder order = orderMapper.selectForUpdate(fulfillmentOrderId);
		Assert.notNull(order, "履约订单不存在");
		taskOrder.setPreviousOrderStatus(taskOrder.getOrderStatus());
		taskOrder.setPreviousFulfillmentStatus(order.getFulfillmentStatus().name());
		taskOrder.setExceptionType(dto.getExceptionType());
		taskOrder.setExceptionReason(dto.getReason());
		taskOrder.setExceptionImageUrls(dto.getImageUrls() == null
				? null : String.join("\n", dto.getImageUrls()));
		taskOrder.setOrderStatus("EXCEPTION");
		taskOrderMapper.updateById(taskOrder);
		Assert.isTrue(orderMapper.transit(order.getId(), order.getFulfillmentStatus(),
				FulfillmentStatus.EXCEPTION) == 1, "订单状态已变化，请刷新后重试");
		task.setTaskStatus("PARTIAL_EXCEPTION");
		taskMapper.updateById(task);
		syncProgress(order, FulfillmentStatus.EXCEPTION);
	}

	@Transactional(rollbackFor = Exception.class)
	public void restoreException(Long taskId, Long fulfillmentOrderId, Long userId) {
		requireTaskDependencies();
		WmsFulfillmentPickTask task = taskMapper.selectById(taskId);
		Assert.notNull(task, "拣货任务不存在");
		requireOperator(task, userId);
		WmsFulfillmentPickTaskOrder taskOrder = findTaskOrder(taskId, fulfillmentOrderId);
		Assert.isTrue("EXCEPTION".equals(taskOrder.getOrderStatus()), "订单不是异常状态");
		Assert.hasText(taskOrder.getPreviousOrderStatus(), "异常前任务状态缺失");
		Assert.hasText(taskOrder.getPreviousFulfillmentStatus(), "异常前订单状态缺失");
		WmsFulfillmentOrder order = orderMapper.selectForUpdate(fulfillmentOrderId);
		Assert.notNull(order, "履约订单不存在");
		FulfillmentStatus restored = FulfillmentStatus.valueOf(
				taskOrder.getPreviousFulfillmentStatus());
		Assert.isTrue(orderMapper.transit(order.getId(), FulfillmentStatus.EXCEPTION,
				restored) == 1, "订单状态已变化，请刷新后重试");
		taskOrder.setOrderStatus(taskOrder.getPreviousOrderStatus());
		clearException(taskOrder);
		taskOrderMapper.updateById(taskOrder);
		recalculateTaskStatus(taskId);
		syncProgress(order, restored);
	}

	@Transactional(rollbackFor = Exception.class)
	public void cancelException(Long taskId, Long fulfillmentOrderId, Long userId) {
		requireTaskDependencies();
		Assert.notNull(statusSyncService, "履约状态服务未配置");
		WmsFulfillmentPickTask task = taskMapper.selectById(taskId);
		Assert.notNull(task, "拣货任务不存在");
		requireOperator(task, userId);
		WmsFulfillmentPickTaskOrder taskOrder = findTaskOrder(taskId, fulfillmentOrderId);
		Assert.isTrue("EXCEPTION".equals(taskOrder.getOrderStatus()), "订单不是异常状态");
		List<WmsFulfillmentPickTaskLine> lines = taskLineMapper.selectList(
				Wrappers.<WmsFulfillmentPickTaskLine>lambdaQuery()
						.eq(WmsFulfillmentPickTaskLine::getTaskId, taskId)
						.eq(WmsFulfillmentPickTaskLine::getFulfillmentOrderId,
								fulfillmentOrderId));
		boolean goodsPicked = false;
		for (WmsFulfillmentPickTaskLine line : lines) {
			if (line.getPickedQuantity() != null && line.getPickedQuantity() > 0) {
				goodsPicked = true;
				break;
			}
		}
		statusSyncService.cancelFromException(fulfillmentOrderId, goodsPicked,
				taskOrder.getExceptionReason());
		taskOrder.setOrderStatus("CANCELLED");
		taskOrder.setCompletedTime(LocalDateTime.now());
		taskOrderMapper.updateById(taskOrder);
		recalculateTaskStatus(taskId);
	}

	private WmsFulfillmentPickTaskOrder findTaskOrder(Long taskId, Long fulfillmentOrderId) {
		WmsFulfillmentPickTaskOrder taskOrder = taskOrderMapper.selectOne(
				Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
						.eq(WmsFulfillmentPickTaskOrder::getTaskId, taskId)
						.eq(WmsFulfillmentPickTaskOrder::getFulfillmentOrderId,
								fulfillmentOrderId));
		Assert.notNull(taskOrder, "订单不属于当前拣货任务");
		return taskOrder;
	}

	private void clearException(WmsFulfillmentPickTaskOrder taskOrder) {
		taskOrder.setPreviousOrderStatus(null);
		taskOrder.setPreviousFulfillmentStatus(null);
		taskOrder.setExceptionType(null);
		taskOrder.setExceptionReason(null);
		taskOrder.setExceptionImageUrls(null);
	}

	private void recalculateTaskStatus(Long taskId) {
		WmsFulfillmentPickTask task = taskMapper.selectById(taskId);
		Assert.notNull(task, "拣货任务不存在");
		long exceptions = taskOrderMapper.selectCount(
				Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
						.eq(WmsFulfillmentPickTaskOrder::getTaskId, taskId)
						.eq(WmsFulfillmentPickTaskOrder::getOrderStatus, "EXCEPTION"));
		if (exceptions > 0) {
			task.setTaskStatus("PARTIAL_EXCEPTION");
		}
		else {
			long pending = taskOrderMapper.selectCount(
					Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
							.eq(WmsFulfillmentPickTaskOrder::getTaskId, taskId)
							.notIn(WmsFulfillmentPickTaskOrder::getOrderStatus,
									"COMPLETED", "CANCELLED"));
			task.setTaskStatus(pending == 0 ? "COMPLETED" : "PICKING");
		}
		taskMapper.updateById(task);
	}

	private int compareLocation(String left, String right) {
		if (left == null) return right == null ? 0 : 1;
		if (right == null) return -1;
		return left.compareToIgnoreCase(right);
	}

	@Transactional(rollbackFor = Exception.class)
	public void scan(FulfillmentPickScanDTO dto, Long userId) {
		requireTaskDependencies();
		WmsFulfillmentPickTask task = taskMapper.selectById(dto.getTaskId());
		Assert.notNull(task, "拣货任务不存在");
		Assert.isTrue("PICKING".equals(task.getTaskStatus())
						|| "PARTIAL_EXCEPTION".equals(task.getTaskStatus()), "拣货任务已结束");
		requireOperator(task, userId);
		List<WmsFulfillmentPickTaskOrder> taskOrders = taskOrderMapper.selectList(
				Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
						.eq(WmsFulfillmentPickTaskOrder::getTaskId, dto.getTaskId())
						.orderByAsc(WmsFulfillmentPickTaskOrder::getSequenceNo));
		WmsFulfillmentPickTaskOrder current = null;
		WmsFulfillmentOrder order = null;
		for (WmsFulfillmentPickTaskOrder candidate : taskOrders) {
			WmsFulfillmentOrder candidateOrder = orderMapper.selectById(candidate.getFulfillmentOrderId());
			if (candidateOrder != null && dto.getFulfillmentNo().equals(candidateOrder.getFulfillmentNo())) {
				current = candidate;
				order = candidateOrder;
				break;
			}
		}
		Assert.notNull(current, "订单不属于当前拣货任务");
		Assert.isTrue("PENDING".equals(current.getOrderStatus())
						|| "PICKING".equals(current.getOrderStatus()),
				"当前订单状态不允许继续取货");
		if ("PENDING".equals(current.getOrderStatus())) {
			current.setOrderStatus("PICKING");
			current.setStartedTime(LocalDateTime.now());
			taskOrderMapper.updateById(current);
		}
		List<WmsFulfillmentPickTaskLine> candidates = taskLineMapper.selectList(
				Wrappers.<WmsFulfillmentPickTaskLine>lambdaQuery()
						.eq(WmsFulfillmentPickTaskLine::getTaskId, dto.getTaskId())
						.eq(WmsFulfillmentPickTaskLine::getFulfillmentOrderId, order.getId())
						.eq(WmsFulfillmentPickTaskLine::getLocationCode, dto.getLocationCode())
						.eq(WmsFulfillmentPickTaskLine::getWarehouseSkuCode, dto.getWarehouseSkuCode())
						.ne(WmsFulfillmentPickTaskLine::getLineStatus, "COMPLETED")
						.orderByAsc(WmsFulfillmentPickTaskLine::getSequenceNo));
		Assert.notEmpty(candidates, "订单、库位或内部 SKU 与当前拣货明细不匹配");
		WmsFulfillmentPickTaskLine line = candidates.get(0);
		Assert.isTrue(taskLineMapper.addPicked(line.getId(), dto.getQuantity(), line.getVersion()) == 1,
				"扫描数量超出计划或数据已变化，请刷新后重试");
		completeOrderIfReady(task, current, order);
	}

	private void completeOrderIfReady(WmsFulfillmentPickTask task,
			WmsFulfillmentPickTaskOrder current, WmsFulfillmentOrder order) {
		long incomplete = taskLineMapper.selectCount(Wrappers.<WmsFulfillmentPickTaskLine>lambdaQuery()
				.eq(WmsFulfillmentPickTaskLine::getTaskId, task.getId())
				.eq(WmsFulfillmentPickTaskLine::getFulfillmentOrderId, order.getId())
				.ne(WmsFulfillmentPickTaskLine::getLineStatus, "COMPLETED"));
		if (incomplete > 0) return;
		current.setOrderStatus("WAITING_LABEL");
		taskOrderMapper.updateById(current);
		Assert.isTrue(orderMapper.transit(order.getId(), FulfillmentStatus.PICKING,
				FulfillmentStatus.WAITING_PACK) == 1, "订单拣货状态更新失败");
		syncProgress(order, FulfillmentStatus.WAITING_PACK);
	}

	private void requireOperator(WmsFulfillmentPickTask task, Long userId) {
		Assert.notNull(userId, "当前操作人不能为空");
		Assert.isTrue(userId.equals(task.getOperatorId()), "只有领取该任务的拣货员可以操作");
	}

	public void assertTaskOperator(Long fulfillmentOrderId, Long userId) {
		requireTaskDependencies();
		List<WmsFulfillmentPickTaskOrder> matches = taskOrderMapper.selectList(
				Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
						.eq(WmsFulfillmentPickTaskOrder::getFulfillmentOrderId,
								fulfillmentOrderId));
		Assert.isTrue(matches.size() == 1, "履约订单未关联唯一拣货任务");
		WmsFulfillmentPickTask task = taskMapper.selectById(matches.get(0).getTaskId());
		Assert.notNull(task, "拣货任务不存在");
		Assert.isTrue("PICKING".equals(task.getTaskStatus())
						|| "PARTIAL_EXCEPTION".equals(task.getTaskStatus()),
				"拣货任务当前不能操作");
		requireOperator(task, userId);
	}

	private void requireTaskDependencies() {
		Assert.state(taskMapper != null && taskOrderMapper != null && taskLineMapper != null
				&& reservationMapper != null && itemMapper != null && locationMapper != null,
				"拣货任务服务未完整初始化");
	}

	private void acceptOne(Long orderId) {
		WmsFulfillmentOrder order = orderMapper.selectById(orderId);
		Assert.notNull(order, "履约订单不存在");
		Assert.isTrue(order.getFulfillmentStatus() == FulfillmentStatus.WAITING_SHELF,
				"订单不是待下架状态");
		PlatformActionResult platformResult = platformActionService.accept(orderId);
		Assert.isTrue(platformResult != null && platformResult.isSuccess(),
				platformResult == null ? "平台确认结果为空" : platformResult.getMessage());
		Assert.isTrue(orderMapper.transit(orderId, FulfillmentStatus.WAITING_SHELF,
				FulfillmentStatus.WAITING_PICK) == 1, "订单状态已发生变化，请刷新后重试");
		syncProgress(order, FulfillmentStatus.WAITING_PICK);
	}

	private void syncProgress(WmsFulfillmentOrder order, FulfillmentStatus status) {
		if (progressService != null) {
			progressService.sync(order, status);
		}
	}

	private String message(RuntimeException ex) {
		return ex.getMessage() == null || ex.getMessage().trim().isEmpty()
				? "处理失败"
				: ex.getMessage();
	}
}
