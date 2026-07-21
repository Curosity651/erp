package com.erp.admin.system.controller;

import java.util.List;
import java.util.stream.Collectors;

import com.erp.admin.system.model.dto.UserAvatarDTO;
import com.erp.admin.system.model.dto.UserDropdownDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ballcat.business.system.model.entity.SysUser;
import org.ballcat.business.system.service.SysUserService;
import org.ballcat.common.model.result.ApiResult;
import org.ballcat.common.model.result.BaseResultCode;
import org.ballcat.log.operation.annotation.OperationLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器
 *
 * @author ballcat
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/user")
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {

	private final SysUserService sysUserService;

	/**
	 * 获取用户下拉框选项
	 * @param keyword 搜索关键字（可选）
	 * @return 用户下拉框选项列表
	 */
	@GetMapping("/dropdown")
	@Operation(summary = "获取用户下拉框选项", description = "获取用户下拉框选项，支持关键字搜索")
	public ApiResult<List<UserDropdownDTO>> getUserDropdown(
			@RequestParam(value = "keyword", required = false) String keyword) {

		try {
			// 获取所有用户列表
			List<SysUser> userList = this.sysUserService.list();

			// 如果有搜索关键字，进行过滤
			if (keyword != null && !keyword.trim().isEmpty()) {
				String searchKeyword = keyword.trim().toLowerCase();
				userList = userList.stream().filter(user -> {
					// 搜索用户名、昵称
					String username = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
					String nickname = user.getNickname() != null ? user.getNickname().toLowerCase() : "";

					return username.contains(searchKeyword) || nickname.contains(searchKeyword);
				}).collect(Collectors.toList());
			}

			// 转换为下拉框选项格式
			List<UserDropdownDTO> dropdownOptions = userList.stream()
				.map(user -> new UserDropdownDTO(user.getUserId(), getUserDisplayName(user)))
				.collect(Collectors.toList());

			log.debug("获取用户下拉框选项成功: 关键字={}, 结果数量={}", keyword, dropdownOptions.size());
			return ApiResult.ok(dropdownOptions);

		}
		catch (Exception e) {
			log.error("获取用户下拉框选项失败", e);
			return ApiResult.failed(BaseResultCode.LOGIC_CHECK_ERROR);
		}
	}

	/**
	 * 获取用户显示名称
	 * @param user 用户对象
	 * @return 用户显示名称
	 */
	private String getUserDisplayName(SysUser user) {
		return user.getNickname() != null ? user.getNickname() : "未知用户";
	}

	@OperationLog(bizType = "user", subType = "user.update.avatar", bizNo = "#{#userId}",
			successMessage = "修改用户头像: #{#p0.userId}")
	@PutMapping({ "/avatar" })
	@Operation(summary = "修改系统用户头像", description = "修改系统用户头像")
	public ApiResult<String> updateAvatar(@RequestBody UserAvatarDTO userAvatarDTO) {
		SysUser sysUser = new SysUser();
		sysUser.setUserId(userAvatarDTO.getUserId());
		sysUser.setAvatar(userAvatarDTO.getObjectKey());
		this.sysUserService.updateById(sysUser);
		return ApiResult.ok(userAvatarDTO.getObjectKey());
	}

}
