package com.erp.admin.product.model.vo;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 供应商分页视图对象
 *
 * @author ballcat 2025-07-26 22:36:14
 */
@Data
@Schema(title = "供应商分页视图对象")
public class SupplierPageVO {

	/**
	 * 供应商ID
	 */
	@Schema(title = "供应商ID")
	private Long id;

	/**
	 * 供应商编码，唯一标识
	 */
	@Schema(title = "供应商编码，唯一标识")
	private String supplierCode;

	/**
	 * 供应商名称
	 */
	@Schema(title = "供应商名称")
	private String name;

	/**
	 * 供应商所在城市
	 */
	@Schema(title = "供应商所在城市")
	private String city;

	/**
	 * 详细地址
	 */
	@Schema(title = "详细地址")
	private String address;

	/**
	 * 税号
	 */
	@Schema(title = "税号")
	private String taxNumber;

	/**
	 * 法人姓名
	 */
	@Schema(title = "法人姓名")
	private String legalPersonName;

	/**
	 * 法人电话
	 */
	@Schema(title = "法人电话")
	private String legalPersonPhone;

	/**
	 * 业务联系人
	 */
	@Schema(title = "业务联系人")
	private String businessContactName;

	/**
	 * 业务联系人电话
	 */
	@Schema(title = "业务联系人电话")
	private String businessContactPhone;

	/**
	 * 业务联系人邮箱
	 */
	@Schema(title = "业务联系人邮箱")
	private String businessContactEmail;

	/**
	 * 公账账户名称
	 */
	@Schema(title = "公账账户名称")
	private String publicAccountName;

	/**
	 * 公账银行
	 */
	@Schema(title = "公账银行")
	private String publicBankName;

	/**
	 * 公账账户卡号
	 */
	@Schema(title = "公账账户卡号")
	private String publicAccountNo;

	/**
	 * 公账开户行地址
	 */
	@Schema(title = "公账开户行地址")
	private String publicBankAddress;

	/**
	 * 私账账户名称
	 */
	@Schema(title = "私账账户名称")
	private String privateAccountName;

	/**
	 * 私账账户卡号
	 */
	@Schema(title = "私账账户卡号")
	private String privateAccountNo;

	/**
	 * 私账银行
	 */
	@Schema(title = "私账银行")
	private String privateBankName;

	/**
	 * 私账开户行地址
	 */
	@Schema(title = "私账开户行地址")
	private String privateBankAddress;

	/**
	 * 营业执照照片OSS key
	 */
	@Schema(title = "营业执照照片OSS key")
	private String businessLicensePhoto;

	/**
	 * 状态（1-启用，0-停用）
	 */
	@Schema(title = "状态（1-启用，0-停用）")
	private Integer status;

	/**
	 * 备注
	 */
	@Schema(title = "备注")
	private String remarks;

	/**
	 * 逻辑删除标识，未删除为0，已删除为删除时间
	 */
	@Schema(title = "逻辑删除标识，未删除为0，已删除为删除时间")
	private Long deleted;

	/**
	 * 创建人
	 */
	@Schema(title = "创建人")
	private Long createBy;

	/**
	 * 修改人
	 */
	@Schema(title = "修改人")
	private Long updateBy;

	/**
	 * 创建时间
	 */
	@Schema(title = "创建时间")
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	@Schema(title = "更新时间")
	private LocalDateTime updateTime;

}
