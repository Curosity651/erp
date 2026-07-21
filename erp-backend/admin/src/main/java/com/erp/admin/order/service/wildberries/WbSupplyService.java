package com.erp.admin.order.service.wildberries;

import java.time.LocalDateTime;

import com.erp.admin.order.mapper.WbSupplyMapper;
import com.erp.admin.order.model.dto.wildberries.WbSupplySyncDTO;
import com.erp.admin.order.model.entity.WbSupply;
import com.erp.admin.platform.PlatformEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * WbSupplyService - Wildberries Supply（发货批次）服务
 * <p>
 * 职责：
 * - Supply 数据同步（Upsert）
 * - Supply 实体管理
 *
 * @author system
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WbSupplyService {

	private final WbSupplyMapper baseMapper;

	// ==================== 数据同步 ====================

	/**
	 * Upsert Supply
	 *
	 * @param shopId 店铺ID
	 * @param dto    Supply DTO
	 * @return Supply 实体
	 */
	public WbSupply upsertSupply(Long shopId, WbSupplySyncDTO dto) {
		WbSupply existing = baseMapper.selectOneByShopPlatformSupplyId(
				shopId, PlatformEnum.Wildberries.code(), dto.getSupplyId());
		if (existing != null) {
			applyOnUpdate(existing, dto);
			baseMapper.updateById(existing);
			return existing;
		}

		WbSupply po = new WbSupply();
		applyOnInsert(po, shopId, dto);
		baseMapper.insert(po);
		return po;
	}

	/**
	 * 已知 supply 不存在时使用：不做预查询，直接插入；
	 * 若并发下发生唯一约束冲突，回退 select+update，保证幂等与正确性。
	 */
	public WbSupply insertSupplyKnownAbsent(Long shopId, WbSupplySyncDTO dto) {
		WbSupply po = new WbSupply();
		applyOnInsert(po, shopId, dto);
		try {
			baseMapper.insert(po);
			return po;
		} catch (DuplicateKeyException e) {
			WbSupply existing = baseMapper.selectOneByShopPlatformSupplyId(
					shopId, PlatformEnum.Wildberries.code(), dto.getSupplyId());
			if (existing != null) {
				applyOnUpdate(existing, dto);
				baseMapper.updateById(existing);
				return existing;
			}
			throw e;
		}
	}

	// ==================== 私有方法 ====================

	private void applyOnInsert(WbSupply po, Long shopId, WbSupplySyncDTO dto) {
		LocalDateTime now = LocalDateTime.now();
		po.setShopId(shopId);
		po.setPlatform(PlatformEnum.Wildberries.code());
		po.setSupplyId(dto.getSupplyId());
		po.setName(dto.getName());
		po.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : now);
		po.setSyncedAt(now);
		po.setCreateTime(now);
		po.setUpdateTime(now);
		// 若 DTO 已提供面单，插入时直接写入
		if (StringUtils.hasText(dto.getLabelBase64())) {
			po.setLabelBase64(dto.getLabelBase64());
		}
	}

	private void applyOnUpdate(WbSupply existing, WbSupplySyncDTO dto) {
		LocalDateTime now = LocalDateTime.now();
		if (dto.getName() != null) {
			existing.setName(dto.getName());
		}
		existing.setSyncedAt(now);
		existing.setUpdateTime(now);
		// 仅当 DTO 提供时补写，避免覆盖已有面单
		if (StringUtils.hasText(dto.getLabelBase64())) {
			existing.setLabelBase64(dto.getLabelBase64());
		}
	}


	public void updateLabelBase64(Long supplyId, String labelBase64) {
		this.baseMapper.updateLabelBase64(supplyId, labelBase64);
	}
}
