package com.erp.admin.wms.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.admin.tenant.model.vo.TenantIdentityVO;
import com.erp.admin.tenant.service.TenantIdentityService;
import com.erp.admin.wms.mapper.WmsTransportExpenseRecordMapper;
import com.erp.admin.wms.model.dto.TransportExpenseDTO;
import com.erp.admin.wms.model.entity.WmsTransportExpenseRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class TransportExpenseService {

	private final WmsTransportExpenseRecordMapper mapper;
	private final TenantIdentityService tenantIdentityService;

	public List<WmsTransportExpenseRecord> list(String expenseType) {
		Long tenantId = currentWmsTenantId();
		return mapper.selectList(Wrappers.<WmsTransportExpenseRecord>lambdaQuery()
				.eq(WmsTransportExpenseRecord::getTenantId, tenantId)
				.eq(hasText(expenseType), WmsTransportExpenseRecord::getExpenseType, expenseType)
				.orderByDesc(WmsTransportExpenseRecord::getExpenseDate)
				.orderByDesc(WmsTransportExpenseRecord::getSettlementMonth)
				.orderByDesc(WmsTransportExpenseRecord::getId));
	}

	@Transactional
	public Long create(TransportExpenseDTO dto, Long userId) {
		WmsTransportExpenseRecord record = new WmsTransportExpenseRecord();
		record.setTenantId(currentWmsTenantId());
		copy(dto, record);
		record.setCreateBy(userId);
		record.setCreateTime(LocalDateTime.now());
		record.setUpdateBy(userId);
		record.setUpdateTime(LocalDateTime.now());
		mapper.insert(record);
		return record.getId();
	}

	@Transactional
	public void update(Long id, TransportExpenseDTO dto, Long userId) {
		WmsTransportExpenseRecord record = requireOwned(id);
		copy(dto, record);
		record.setUpdateBy(userId);
		record.setUpdateTime(LocalDateTime.now());
		mapper.updateById(record);
	}

	@Transactional
	public void delete(Long id) {
		mapper.deleteById(requireOwned(id));
	}

	private void copy(TransportExpenseDTO dto, WmsTransportExpenseRecord record) {
		Assert.isTrue("FUEL".equals(dto.getExpenseType()) || "DRIVER_MONTHLY".equals(dto.getExpenseType()), "费用类型不正确");
		if ("FUEL".equals(dto.getExpenseType())) Assert.notNull(dto.getExpenseDate(), "油费日期不能为空");
		if ("DRIVER_MONTHLY".equals(dto.getExpenseType())) {
			Assert.hasText(dto.getSettlementMonth(), "月结月份不能为空");
			Assert.hasText(dto.getDriverName(), "司机不能为空");
		}
		record.setExpenseType(dto.getExpenseType());
		record.setExpenseDate("FUEL".equals(dto.getExpenseType()) ? dto.getExpenseDate() : null);
		record.setSettlementMonth("DRIVER_MONTHLY".equals(dto.getExpenseType()) ? dto.getSettlementMonth() : null);
		record.setDriverName(hasText(dto.getDriverName()) ? dto.getDriverName().trim() : null);
		record.setAmount(dto.getAmount());
		record.setCurrency(hasText(dto.getCurrency()) ? dto.getCurrency().trim().toUpperCase() : "CNY");
		record.setNote(hasText(dto.getNote()) ? dto.getNote().trim() : null);
	}

	private WmsTransportExpenseRecord requireOwned(Long id) {
		WmsTransportExpenseRecord record = mapper.selectById(id);
		Assert.notNull(record, "运输费用记录不存在");
		Assert.isTrue(currentWmsTenantId().equals(record.getTenantId()), "不能操作其他服务商的运输费用记录");
		return record;
	}

	private Long currentWmsTenantId() {
		TenantIdentityVO identity = tenantIdentityService.currentIdentity(null);
		Assert.isTrue(TenantIdentityService.IDENTITY_OVERSEAS_PLATFORM.equals(identity.getIdentityType())
				|| TenantIdentityService.IDENTITY_WMS_OPERATOR.equals(identity.getIdentityType()),
				"只有海外仓平台或WMS服务商可以维护运输费用台账");
		return identity.getTenantId();
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
