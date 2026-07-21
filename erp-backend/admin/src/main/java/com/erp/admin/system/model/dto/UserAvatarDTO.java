package com.erp.admin.system.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户头像DTO
 *
 * @author ballcat
 */
@Data
@NoArgsConstructor
@Schema(title = "用户头像DTO")
public class UserAvatarDTO {

	@Schema(title = "用户ID")
	private Long userId;

	@Schema(title = "头像ObjectKey")
	private String objectKey;

}
