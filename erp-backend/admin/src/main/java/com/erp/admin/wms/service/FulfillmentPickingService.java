package com.erp.admin.wms.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.wms.mapper.WmsFulfillmentItemMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskLineMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentPickTaskOrderMapper;
import com.erp.admin.wms.mapper.WmsInventoryReservationMapper;
import com.erp.admin.wms.mapper.WmsLocationMapper;
import com.erp.admin.wms.mapper.WmsFulfillmentOrderMapper;
import com.erp.admin.wms.model.dto.FulfillmentPickScanDTO;
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
import com.erp.admin.wms.service.platform.PlatformActionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
public class FulfillmentPickingService {
	private final WmsFulfillmentOrderMapper orderMapper;
	private final FulfillmentPlatformActionService platformActionService;
	private WmsFulfillmentPickTaskMapper taskMapper;
	private WmsFulfillmentPickTaskOrderMapper taskOrderMapper;
	private WmsFulfillmentPickTaskLineMapper taskLineMapper;
	private WmsInventoryReservationMapper reservationMapper;
	private WmsFulfillmentItemMapper itemMapper;
	private WmsLocationMapper locationMapper;

	public FulfillmentPickingService(WmsFulfillmentOrderMapper orderMapper,
			FulfillmentPlatformActionService platformActionService) {
		this.orderMapper = orderMapper;
		this.platformActionService = platformActionService;
	}

	@Autowired
	public FulfillmentPickingService(WmsFulfillmentOrderMapper orderMapper,
			FulfillmentPlatformActionService platformActionService,
			WmsFulfillmentPickTaskMapper taskMapper,
			WmsFulfillmentPickTaskOrderMapper taskOrderMapper,
			WmsFulfillmentPickTaskLineMapper taskLineMapper,
			WmsInventoryReservationMapper reservationMapper,
			WmsFulfillmentItemMapper itemMapper, WmsLocationMapper locationMapper) {
		this(orderMapper, platformActionService);
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
		return orderMapper.selectList(Wrappers.<WmsFulfillmentOrder>lambdaQuery()
				.in(WmsFulfillmentOrder::getFulfillmentStatus, FulfillmentStatus.WAITING_SHELF,
						FulfillmentStatus.WAITING_PICK, FulfillmentStatus.PICKING)
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
			Assert.isTrue(first.getErpTenantId().equals(order.getErpTenantId()), "批量任务必须属于同一货主");
			Assert.isTrue(first.getWmsTenantId().equals(order.getWmsTenantId()), "批量任务必须属于同一服务商");
		}

		WmsFulfillmentPickTask task = new WmsFulfillmentPickTask();
		task.setTaskNo("FPT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
				+ UUID.randomUUID().toString().substring(0, 6).toUpperCase());
		task.setWarehouseId(first.getWarehouseId());
		task.setWmsTenantId(first.getWmsTenantId());
		task.setErpTenantId(first.getErpTenantId());
		task.setTaskStatus("PICKING");
		task.setOrderCount(orders.size());
		task.setTotalQuantity(0);
		task.setOperatorId(operatorId);
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
			Assert.isTrue(orderMapper.transit(order.getId(), FulfillmentStatus.WAITING_PICK,
					FulfillmentStatus.PICKING) == 1, "订单状态已变化，请重新创建任务");
		}
		task.setTotalQuantity(total);
		taskMapper.updateById(task);
		return task;
	}

	public List<WmsFulfillmentPickTask> listTasks() {
		requireTaskDependencies();
		return taskMapper.selectList(Wrappers.<WmsFulfillmentPickTask>lambdaQuery()
				.orderByDesc(WmsFulfillmentPickTask::getCreateTime));
	}

	public FulfillmentPickTaskDetailVO detail(Long taskId) {
		requireTaskDependencies();
		WmsFulfillmentPickTask task = taskMapper.selectById(taskId);
		Assert.notNull(task, "拣货任务不存在");
		FulfillmentPickTaskDetailVO result = new FulfillmentPickTaskDetailVO();
		result.setTask(task);
		result.setOrders(taskOrderMapper.selectList(Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
				.eq(WmsFulfillmentPickTaskOrder::getTaskId, taskId)
				.orderByAsc(WmsFulfillmentPickTaskOrder::getSequenceNo)));
		result.setLines(taskLineMapper.selectList(Wrappers.<WmsFulfillmentPickTaskLine>lambdaQuery()
				.eq(WmsFulfillmentPickTaskLine::getTaskId, taskId)
				.orderByAsc(WmsFulfillmentPickTaskLine::getSequenceNo)));
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public void scan(FulfillmentPickScanDTO dto) {
		requireTaskDependencies();
		WmsFulfillmentPickTask task = taskMapper.selectById(dto.getTaskId());
		Assert.notNull(task, "拣货任务不存在");
		Assert.isTrue("PICKING".equals(task.getTaskStatus()), "拣货任务已结束");
		List<WmsFulfillmentPickTaskOrder> pendingOrders = taskOrderMapper.selectList(
				Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
						.eq(WmsFulfillmentPickTaskOrder::getTaskId, dto.getTaskId())
						.ne(WmsFulfillmentPickTaskOrder::getOrderStatus, "COMPLETED")
						.orderByAsc(WmsFulfillmentPickTaskOrder::getSequenceNo));
		Assert.notEmpty(pendingOrders, "任务已全部拣完");
		WmsFulfillmentPickTaskOrder current = pendingOrders.get(0);
		WmsFulfillmentOrder order = orderMapper.selectById(current.getFulfillmentOrderId());
		Assert.isTrue(order != null && dto.getFulfillmentNo().equals(order.getFulfillmentNo()),
				"请先完成当前顺序订单：" + (order == null ? "-" : order.getFulfillmentNo()));
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
		current.setOrderStatus("COMPLETED");
		taskOrderMapper.updateById(current);
		Assert.isTrue(orderMapper.transit(order.getId(), FulfillmentStatus.PICKING,
				FulfillmentStatus.WAITING_PACK) == 1, "订单拣货状态更新失败");
		long pending = taskOrderMapper.selectCount(Wrappers.<WmsFulfillmentPickTaskOrder>lambdaQuery()
				.eq(WmsFulfillmentPickTaskOrder::getTaskId, task.getId())
				.ne(WmsFulfillmentPickTaskOrder::getOrderStatus, "COMPLETED"));
		if (pending == 0) {
			task.setTaskStatus("COMPLETED");
			taskMapper.updateById(task);
		}
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
	}

	private String message(RuntimeException ex) {
		return ex.getMessage() == null || ex.getMessage().trim().isEmpty()
				? "处理失败"
				: ex.getMessage();
	}
}
