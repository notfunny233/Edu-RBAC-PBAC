package eduadmin.Service;

import java.util.List;

/**
 * 授权关系 Service
 * <p>
 * 只负责"关系"的读写：员工有哪些角色、角色有哪些权限点。
 * 角色/权限点本身的增删改由 RoleService、PermissionService 负责，职责不重叠。
 */
public interface AuthService {

    //查询当前登录人的权限点编码集合（前端用它渲染菜单和按钮的显示隐藏）
    List<String> getCurrentPermissionCodes();

    //查询某员工已分配的角色ID集合（授权页面回显勾选用）
    List<Integer> getRoleIdsByEmpId(Integer empId);

    //给员工分配角色（覆盖式：先清空再写入）
    void assignRolesToEmp(Integer empId, List<Integer> roleIds);

    //查询某角色已分配的权限点ID集合（授权页面回显勾选用）
    List<Integer> getPermissionIdsByRoleId(Integer roleId);

    //给角色分配权限点（覆盖式：先清空再写入）
    void assignPermissionsToRole(Integer roleId, List<Integer> permissionIds);
}
