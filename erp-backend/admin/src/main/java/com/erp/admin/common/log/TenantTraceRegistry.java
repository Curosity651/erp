package com.erp.admin.common.log;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 访问日志「租户归属」桥接表（traceId → 租户/用户）。
 *
 * <p>为什么需要：BallCat 访问日志由后台 {@code AccessLogSaveThread} 落库，脱离请求线程 → 既无 {@link com.erp.admin.common.tenant.TenantContext}，
 * 记录里 {@code userId} 也为空 → 无从判断该请求属于哪个租户。而每条访问日志都带唯一 {@code traceId}（与 MDC {@code traceId} 一致）。
 *
 * <p>做法：在<b>请求线程</b>内（{@code TenantResolveInterceptor.preHandle}，此刻租户/登录态都在）把
 * {@code traceId → (tenantId, userId, username)} 记到本表；访问日志落库时（{@link ErpAccessLogService}）按记录自带的
 * {@code traceId} 取回，据此把 {@code tenant_id}（及被框架漏记的 user）补上。
 *
 * <p>内存安全：落库时 {@link #consume} 会移除条目；对少数「有 preHandle 但不写访问日志」的请求（如被 accesslog 规则忽略的
 * 头像/swagger），条目会滞留，故用带上限的 LRU（超过 {@link #MAX} 淘汰最旧），恒定占用、不泄漏。
 *
 * @author erp
 */
public final class TenantTraceRegistry {

	/** 归属信息：请求所属租户 + 操作用户（用于补回访问日志漏记的 user）。 */
	public static final class Attribution {

		private final Long tenantId;

		private final Long userId;

		private final String username;

		Attribution(Long tenantId, Long userId, String username) {
			this.tenantId = tenantId;
			this.userId = userId;
			this.username = username;
		}

		public Long getTenantId() {
			return this.tenantId;
		}

		public Long getUserId() {
			return this.userId;
		}

		public String getUsername() {
			return this.username;
		}

	}

	/** 桥接表容量上限（LRU 淘汰）。够覆盖瞬时在途请求，远超实际并发。 */
	private static final int MAX = 20000;

	private static final Map<String, Attribution> HOLDER = Collections
		.synchronizedMap(new LinkedHashMap<String, Attribution>(1024, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Attribution> eldest) {
				return size() > MAX;
			}
		});

	private TenantTraceRegistry() {
	}

	/** 请求线程内登记（traceId 为空则跳过）。 */
	public static void put(String traceId, Long tenantId, Long userId, String username) {
		if (traceId == null || traceId.isEmpty()) {
			return;
		}
		HOLDER.put(traceId, new Attribution(tenantId, userId, username));
	}

	/** 落库时取回并移除；无则返回 null。 */
	public static Attribution consume(String traceId) {
		if (traceId == null || traceId.isEmpty()) {
			return null;
		}
		return HOLDER.remove(traceId);
	}

}
