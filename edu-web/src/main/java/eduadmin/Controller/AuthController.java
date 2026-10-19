package eduadmin.Controller;

import eduadmin.Service.AuthService;
import eduadmin.anno.Log;
import eduadmin.anno.RequiresPermission;
import eduadmin.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 授权管理（员工 ↔ 角色、角色 ↔ 权限点）
 * <p>
 * 前四个接口管的是"关系"，所以路由用两层表达主从：
 * /auth/roles/{empId}       某员工的角色
 * /auth/permissions/{roleId} 某角色的权限点
 * <p>
 * 授权统一采用 PUT + 覆盖式语义：前端传来的是"这次保存后应该有的完整集合"，
 * 服务端先清空再写入。这样前端不用自己算增删了哪些，
 * 重复提交、网络重试也不会产生重复关联（幂等）。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 查询当前登录人自己的权限点编码集合
     * <p>
     * ⚠️ 这个接口故意不加 @RequiresPermission：
     * 前端一进页面就要用它来决定菜单和按钮显不显示，
     * 如果它也要求某个权限点，就会出现"要先有权限才能知道自己有什么权限"的死循环。
     * 它只返回调用者自己的权限，不涉及别人的数据，放开是安全的。
     */
    @GetMapping("/permissions")
    public Result currentPermissions() {
        log.info("查询当前登录人的权限点集合");
        List<String> codes = authService.getCurrentPermissionCodes();
        return Result.success(codes);
    }

    //查询某员工已分配的角色ID集合（授权弹窗回显勾选用）
    //@RequiresPermission 是项目自己的注解（定义在 anno/RequiresPermission.java），
    //不是 Spring 也不是 Shiro 提供的。作用：访问这个接口必须持有 role:list 权限点。
    @RequiresPermission("role:list")
    @GetMapping("/roles/{empId}")
    public Result getEmpRoles(@PathVariable Integer empId) {
        log.info("查询员工已分配的角色, empId:{}", empId);
        List<Integer> roleIds = authService.getRoleIdsByEmpId(empId);
        return Result.success(roleIds);
    }

    //给员工分配角色（覆盖式，请求体是角色ID数组）
    @RequiresPermission("role:assign")
    @Log
    @PutMapping("/roles/{empId}")
    public Result assignEmpRoles(@PathVariable Integer empId, @RequestBody List<Integer> roleIds) {
        log.info("给员工分配角色, empId:{}, roleIds:{}", empId, roleIds);
        authService.assignRolesToEmp(empId, roleIds);
        return Result.success();
    }

    //查询某角色已分配的权限点ID集合（授权弹窗回显勾选用）
    @RequiresPermission("role:list")
    @GetMapping("/permissions/{roleId}")
    public Result getRolePermissions(@PathVariable Integer roleId) {
        log.info("查询角色已分配的权限点, roleId:{}", roleId);
        List<Integer> permissionIds = authService.getPermissionIdsByRoleId(roleId);
        return Result.success(permissionIds);
    }

    //给角色分配权限点（覆盖式，请求体是权限点ID数组）
    @RequiresPermission("role:assign")
    @Log
    @PutMapping("/permissions/{roleId}")
    public Result assignRolePermissions(@PathVariable Integer roleId, @RequestBody List<Integer> permissionIds) {
        log.info("给角色分配权限点, roleId:{}, permissionIds:{}", roleId, permissionIds);
        authService.assignPermissionsToRole(roleId, permissionIds);
        return Result.success();
    }

}
