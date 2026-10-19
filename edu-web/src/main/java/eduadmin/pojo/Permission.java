package eduadmin.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Permission 权限点实体类
 * 字段与数据库表 permission 一一对应
 * <p>
 * code 约定为 resource:action 两段式（如 emp:delete）：
 * 决策引擎拿到接口上的注解后，直接按冒号拆出"资源"和"操作"，
 * 一份编码同时供权限点匹配和策略匹配使用，不用再维护一张资源对照表。
 * 特殊值 code = '*' 表示全部权限，超级管理员用。
 */
@Data
public class Permission {
    private Integer id; //ID,主键
    private String name; //权限点名称, 如 删除员工
    private String code; //权限点编码, 约定 resource:action, 如 emp:delete; * 表示全部权限
    private Integer type; //类型, 1:菜单, 2:按钮, 3:接口
    private Integer parentId; //父级ID, 0为顶级(菜单树用)
    private Integer sort; //排序号, 越小越靠前
    private String remark; //备注
    private LocalDateTime createTime; //创建时间
    private LocalDateTime updateTime; //修改时间
}
