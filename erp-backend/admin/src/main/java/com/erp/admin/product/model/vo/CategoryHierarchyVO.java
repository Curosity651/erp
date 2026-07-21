package com.erp.admin.product.model.vo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 品类层级视图对象
 *
 * @author system
 */
@Data
@Schema(title = "品类层级视图对象")
public class CategoryHierarchyVO {

	/**
	 * 当前品类ID
	 */
	@Schema(title = "当前品类ID")
	private Long categoryId;

	/**
	 * 当前品类名称
	 */
	@Schema(title = "当前品类名称")
	private String categoryName;

	/**
	 * 当前品类编码
	 */
	@Schema(title = "当前品类编码")
	private String categoryCode;

	/**
	 * 当前品类层级
	 */
	@Schema(title = "当前品类层级")
	private Integer categoryLevel;

	/**
	 * 父级品类路径（从根到当前品类的完整路径） 格式：[{id: 1, name: "一级品类", code: "L1"}, {id: 2, name: "二级品类",
	 * code: "L2"}]
	 */
	@Schema(title = "父级品类路径")
	private List<CategoryPathNode> parentPath;

	/**
	 * 完整路径名称（用/分隔） 格式：一级品类/二级品类/三级品类
	 */
	@Schema(title = "完整路径名称")
	private String fullPathName;

	/**
	 * 品类路径节点
	 */
	@Data
	@Schema(title = "品类路径节点")
	public static class CategoryPathNode {

		/**
		 * 品类ID
		 */
		@Schema(title = "品类ID")
		private Long id;

		/**
		 * 品类名称
		 */
		@Schema(title = "品类名称")
		private String name;

		/**
		 * 品类编码
		 */
		@Schema(title = "品类编码")
		private String code;

		/**
		 * 品类层级
		 */
		@Schema(title = "品类层级")
		private Integer level;

		public CategoryPathNode() {
		}

		public CategoryPathNode(Long id, String name, String code, Integer level) {
			this.id = id;
			this.name = name;
			this.code = code;
			this.level = level;
		}

	}

}
