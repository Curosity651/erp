package com.erp.admin.common.log;

import com.erp.admin.tenant.mapper.ErpAccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.business.log.handler.CustomOperationLogHandler;
import org.ballcat.business.log.service.OperationLogService;
import org.ballcat.log.operation.domain.OperationLogInfo;
import org.ballcat.log.operation.handler.OperationLogHandler;
import org.ballcat.security.core.PrincipalAttributeAccessor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 操作日志「补全操作人」处理器（日志可见性修复）。
 *
 * <p>问题：BallCat 操作日志的 {@code operator} 仅来自 {@code @OperationLog(operator=SpEL)} 注解，框架<b>不会</b>
 * 自动从登录态解析操作人；本项目注解多未指定 → operator 恒为 NULL，导致按操作人/租户的可见性过滤全部落空。
 *
 * <p>本处理器以 {@link Primary} 覆盖默认 {@link OperationLogHandler}：在<b>同步</b>的 handle()（请求线程内，安全上下文可用）
 * 里，若 operator 为空则用 <b>当前登录用户的唯一 user_id 解析出用户名</b>（避免重名歧义）补上，再委派给 BallCat 原始
 * {@link CustomOperationLogHandler} 完成转换与保存。
 *
 * <p>不在 sqlSessionFactory 构建链上（由操作日志 AOP 拦截器使用），故注入 Mapper 不会引发循环依赖。
 *
 * @author erp
 */
@Slf4j
@Primary
@Component
public class ErpOperationLogHandler implements OperationLogHandler {

	private final OperationLogHandler delegate;

	private final PrincipalAttributeAccessor principalAttributeAccessor;

	private final ErpAccountMapper erpAccountMapper;

	public ErpOperationLogHandler(OperationLogService operationLogService,
			PrincipalAttributeAccessor principalAttributeAccessor, ErpAccountMapper erpAccountMapper) {
		// 复用 BallCat 原始转换+保存逻辑（公有构造），仅在其之前补全 operator
		this.delegate = new CustomOperationLogHandler(operationLogService);
		this.principalAttributeAccessor = principalAttributeAccessor;
		this.erpAccountMapper = erpAccountMapper;
	}

	@Override
	public void handle(OperationLogInfo operationLogInfo) {
		if (!StringUtils.hasText(operationLogInfo.getOperator())) {
			String username = resolveCurrentUsername();
			if (StringUtils.hasText(username)) {
				operationLogInfo.setOperator(username);
			}
		}
		delegate.handle(operationLogInfo);
	}

	/**
	 * 解析当前登录用户名：优先取 principal 用户名；为空则用唯一 user_id 反查用户名（避免重名/歧义）。
	 */
	private String resolveCurrentUsername() {
		try {
			String username = principalAttributeAccessor.getUsername();
			if (StringUtils.hasText(username)) {
				return username;
			}
			Long userId = principalAttributeAccessor.getUserId();
			if (userId != null) {
				return erpAccountMapper.selectUsernameById(userId);
			}
		}
		catch (Exception ignore) {
			// 无登录上下文（异常/后台）：不补操作人
		}
		return null;
	}

}
