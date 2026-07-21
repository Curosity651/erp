package com.erp.admin.common.log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.erp.admin.common.tenant.TenantContext;
import com.erp.admin.tenant.mapper.ErpAccountMapper;
import org.ballcat.business.log.model.entity.AccessLog;
import org.ballcat.business.log.service.impl.AccessLogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 访问日志「按租户回填」服务（日志隔离修复）。
 *
 * <p>问题：BallCat 访问日志由后台 {@code AccessLogSaveThread} 批量落库，脱离请求线程 → {@link TenantContext} 已清空，
 * 且框架抓取时连 {@code userId} 都没记 → {@code log_access_log.tenant_id} 恒 NULL → 各租户按 {@code WHERE tenant_id=?}
 * 读取时谁都查不到自己的访问日志。{@code AccessLog} 实体亦无 tenantId 字段，无法直接 set。
 *
 * <p>修复：以 {@link Primary} 覆盖默认 {@link AccessLogServiceImpl}。保存前按记录自带的 {@code traceId} 从
 * {@link TenantTraceRegistry}（请求线程内登记的 traceId→租户/用户）取回归属，用 {@link TenantContext#runAs} 在保存
 * 那一刻重建上下文 → 租户拦截器在 INSERT 自动追加 {@code tenant_id} 列；同时把框架漏记的 {@code userId/username} 补回。
 * 一批可能混多个租户，按租户分组分别保存。取不到归属（匿名/登录前请求）则保持 NULL，不误挂到任何租户。
 *
 * @author erp
 */
@Primary
@Service("erpAccessLogService")
public class ErpAccessLogService extends AccessLogServiceImpl {

	@Autowired
	private ErpAccountMapper erpAccountMapper;

	@Override
	public boolean save(AccessLog entity) {
		Long tenantId = attribute(entity);
		if (tenantId == null) {
			return super.save(entity);
		}
		return TenantContext.runAs(tenantId, () -> super.save(entity));
	}

	@Override
	public boolean saveBatch(Collection<AccessLog> entityList, int batchSize) {
		if (entityList == null || entityList.isEmpty()) {
			return super.saveBatch(entityList, batchSize);
		}
		// 按归属租户分组：同租户一批（tenant_id 随 INSERT 注入），无归属(null)单独一批保持 NULL
		Map<Long, List<AccessLog>> byTenant = new LinkedHashMap<>();
		for (AccessLog e : entityList) {
			Long tenantId = attribute(e);
			byTenant.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(e);
		}
		boolean ok = true;
		for (Map.Entry<Long, List<AccessLog>> group : byTenant.entrySet()) {
			Long tenantId = group.getKey();
			List<AccessLog> logs = group.getValue();
			if (tenantId == null) {
				ok &= super.saveBatch(logs, batchSize);
			}
			else {
				ok &= TenantContext.runAs(tenantId, () -> super.saveBatch(logs, batchSize));
			}
		}
		return ok;
	}

	/**
	 * 解析该访问日志的归属租户：优先按 traceId 从桥接表取回（并补回框架漏记的 user）；取不到再按记录自带的
	 * userId 反解（通常也为空 → null）。返回 null 表示无归属（匿名/登录前）。
	 */
	private Long attribute(AccessLog entity) {
		TenantTraceRegistry.Attribution attr = TenantTraceRegistry.consume(entity.getTraceId());
		if (attr != null) {
			if (entity.getUserId() == null && attr.getUserId() != null) {
				entity.setUserId(attr.getUserId());
			}
			if (entity.getUsername() == null && attr.getUsername() != null) {
				entity.setUsername(attr.getUsername());
			}
			return attr.getTenantId();
		}
		Long userId = entity.getUserId();
		if (userId == null) {
			return null;
		}
		try {
			return erpAccountMapper.selectTenantIdByUserId(userId);
		}
		catch (Exception ignore) {
			return null;
		}
	}

}
