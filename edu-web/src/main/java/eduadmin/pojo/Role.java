package eduadmin.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Role 实体类
 * 字段与数据库表 role 一一对应（下划线 ↔ 驼峰，全局已开启映射）
 * <p>
 * 为什么要有角色这一层：如果直接把权限点挂在员工身上，27 个员工就要维护 27 份授权，
 * 新人入职、岗位调整都要重配一遍。抽出角色后，权限只跟"岗位"绑定，
 * 人员变动时只改 emp_role 一行关联即可。
 */
@Data
public class Role {
    private Integer id; //ID,主键
    private String name; //角色名称, 如 班主任
    private String code; //角色编码, 唯一, 如 head_teacher；代码里判断角色用它，判断权限点用 permission.code
    private String remark; //备注
    private LocalDateTime createTime; //创建时间
    private LocalDateTime updateTime; //修改时间
}
