package com.clockshore.aooai.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公司内部部门实体，对应 MySQL 表 department。
 *
 * <p>Entity 层只表达数据库字段结构，不直接承载业务规则；
 * 复杂校验、状态流转和权限判断应放在 Service 层，便于后续维护和测试。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    /**
     * 部门ID。
     */
    private Long id;

    /**
     * 父部门ID。
     */
    private Long parentId;

    /**
     * 部门名称。
     */
    private String deptName;

    /**
     * 部门路径，便于应用层构建组织树。
     */
    private String deptPath;

    /**
     * 排序号。
     */
    private Integer sortNo;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
