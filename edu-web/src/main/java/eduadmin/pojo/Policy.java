package eduadmin.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Policy 权限策略实体类
 * 字段与数据库表 policy 一一对应
 * <p>
 * 这是 PBAC 相比 RBAC 多出来的那一层。RBAC 只能回答"有没有这个权限点"，
 * 回答不了"在什么条件下才能用"，例如：
 *   - 班主任有 emp:delete 权限点，但只能删本部门的员工
 *   - 员工有 emp:update 权限点，但不能改自己的薪资
 * 这类"条件约束"用权限点表达不了（权限点是布尔值，没有维度），
 * 于是抽成一条独立的策略记录：主体 + 资源 + 操作 + 条件 -> 允许/拒绝。
 */
@Data
public class Policy {
    private Integer id; //ID,主键
    private String name; //策略名称, 唯一, 如 班主任不得删除部门
    private Integer subjectType; //主体类型, 1:角色, 2:员工
    private Integer subjectId; //主体ID, subjectType=1 时对应 role.id, =2 时对应 emp.id
    private String resource; //资源, 如 emp; * 表示不限资源
    private String action; //操作, 如 delete; * 表示不限操作
    private Integer effect; //效果, 1:允许, 0:拒绝
    private String conditionExpr; //条件表达式(SpEL), 空表示无条件生效
    private Integer priority; //优先级, 越大越先匹配
    private Integer status; //状态, 1:启用, 0:停用(停用后不参与判定)
    private String remark; //备注
    private LocalDateTime createTime; //创建时间
    private LocalDateTime updateTime; //修改时间
}
